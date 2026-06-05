package co.edu.univalle.vivaeventosorderservice.application.usecase;

import co.edu.univalle.vivaeventosorderservice.application.dto.CreateOrderRequest;
import co.edu.univalle.vivaeventosorderservice.application.dto.DiscountCodeResponse;
import co.edu.univalle.vivaeventosorderservice.application.dto.OrderResponse;
import co.edu.univalle.vivaeventosorderservice.application.dto.TicketTypeResponse;
import co.edu.univalle.vivaeventosorderservice.domain.model.OrderStatus;
import co.edu.univalle.vivaeventosorderservice.infraestructure.client.EventServiceClient;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderEntity;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseTest {

    @Mock
    private EventServiceClient eventServiceClient;

    @Mock
    private OrderJpaRepository orderRepository;

    @InjectMocks
    private CreateOrderUseCase createOrderUseCase;

    private UUID userId;
    private UUID eventId;
    private UUID ticketTypeId;
    private CreateOrderRequest request;
    private TicketTypeResponse ticketType;

    @BeforeEach
    void setUp() {
        userId      = UUID.randomUUID();
        eventId     = UUID.randomUUID();
        ticketTypeId = UUID.randomUUID();

        request = new CreateOrderRequest();
        request.setEventId(eventId);
        request.setTicketTypeId(ticketTypeId);
        request.setQuantity(2);
        request.setCustomerEmail("test@test.com");

        ticketType = new TicketTypeResponse();
        ticketType.setPrice(new BigDecimal("50000"));
        ticketType.setQuantityAvailable(10);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Simula el save devolviendo la misma entidad con un ID asignado */
    private void mockRepositorySave() {
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(Instant.now());
            e.setExpiresAt(Instant.now().plusSeconds(600));
            return e;
        });
    }

    // ── Tests felices ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debe crear orden PENDING cuando hay stock suficiente")
    void shouldCreatePendingOrderWhenStockAvailable() {
        when(eventServiceClient.getTicketType(ticketTypeId)).thenReturn(ticketType);
        mockRepositorySave();

        OrderResponse result = createOrderUseCase.execute(request, userId);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEventId()).isEqualTo(eventId);
        assertThat(result.getTicketTypeId()).isEqualTo(ticketTypeId);
        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getCurrency()).isEqualTo("COP");
    }

    @Test
    @DisplayName("Debe calcular el precio total correctamente (unitPrice × quantity)")
    void shouldCalculateTotalPriceCorrectly() {
        // 50000 × 2 = 100000
        when(eventServiceClient.getTicketType(ticketTypeId)).thenReturn(ticketType);
        mockRepositorySave();

        OrderResponse result = createOrderUseCase.execute(request, userId);

        assertThat(result.getTotalPrice()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(result.getUnitPrice()).isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    @DisplayName("Debe reservar stock en el event-service después de guardar la orden")
    void shouldReserveStockAfterSavingOrder() {
        when(eventServiceClient.getTicketType(ticketTypeId)).thenReturn(ticketType);
        mockRepositorySave();

        createOrderUseCase.execute(request, userId);

        // Primero guarda, luego reserva stock — verificar ambas llamadas
        verify(orderRepository).save(any(OrderEntity.class));
        verify(eventServiceClient).reserveStock(ticketTypeId, 2);
    }

    @Test
    @DisplayName("Debe persistir todos los campos obligatorios de la orden")
    void shouldPersistAllRequiredFields() {
        when(eventServiceClient.getTicketType(ticketTypeId)).thenReturn(ticketType);
        mockRepositorySave();

        createOrderUseCase.execute(request, userId);

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());

        OrderEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getEventId()).isEqualTo(eventId);
        assertThat(saved.getTicketTypeId()).isEqualTo(ticketTypeId);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getCustomerEmail()).isEqualTo("test@test.com");
        assertThat(saved.getCurrency()).isEqualTo("COP");
        assertThat(saved.getExpiresAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    // ── Tests de stock ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debe lanzar CONFLICT (409) cuando el stock es insuficiente")
    void shouldThrow409WhenStockIsInsufficient() {
        ticketType.setQuantityAvailable(1); // Solo 1 disponible, piden 2
        when(eventServiceClient.getTicketType(ticketTypeId)).thenReturn(ticketType);

        assertThatThrownBy(() -> createOrderUseCase.execute(request, userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Stock insuficiente");

        // No debe guardar ni reservar si no hay stock
        verify(orderRepository, never()).save(any());
        verify(eventServiceClient, never()).reserveStock(any(), anyInt());
    }

    @Test
    @DisplayName("Debe crear orden cuando el stock es exactamente igual a la cantidad pedida")
    void shouldCreateOrderWhenStockExactlyMatchesQuantity() {
        ticketType.setQuantityAvailable(2); // Exactamente lo que piden
        when(eventServiceClient.getTicketType(ticketTypeId)).thenReturn(ticketType);
        mockRepositorySave();

        OrderResponse result = createOrderUseCase.execute(request, userId);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    // ── Tests de descuento ────────────────────────────────────────────────────

    @Test
    @DisplayName("Debe aplicar descuento porcentual correctamente")
    void shouldApplyPercentageDiscountCorrectly() {
        // 10% de 100000 = 10000 → total = 90000
        request.setDiscountCode("PROMO10");

        DiscountCodeResponse discount = new DiscountCodeResponse();
        discount.setCode("PROMO10");
        discount.setDiscountType("PERCENTAGE");
        discount.setDiscountValue(new BigDecimal("10"));

        when(eventServiceClient.getTicketType(ticketTypeId)).thenReturn(ticketType);
        when(eventServiceClient.validateDiscountCode("PROMO10")).thenReturn(discount);
        mockRepositorySave();

        OrderResponse result = createOrderUseCase.execute(request, userId);

        assertThat(result.getTotalPrice()).isEqualByComparingTo(new BigDecimal("90000"));
    }

    @Test
    @DisplayName("Debe aplicar descuento fijo correctamente")
    void shouldApplyFixedDiscountCorrectly() {
        // Descuento fijo de 20000 → total = 80000
        request.setDiscountCode("FIXED20K");

        DiscountCodeResponse discount = new DiscountCodeResponse();
        discount.setCode("FIXED20K");
        discount.setDiscountType("FIXED");
        discount.setDiscountValue(new BigDecimal("20000"));

        when(eventServiceClient.getTicketType(ticketTypeId)).thenReturn(ticketType);
        when(eventServiceClient.validateDiscountCode("FIXED20K")).thenReturn(discount);
        mockRepositorySave();

        OrderResponse result = createOrderUseCase.execute(request, userId);

        assertThat(result.getTotalPrice()).isEqualByComparingTo(new BigDecimal("80000"));
    }

    @Test
    @DisplayName("El total no debe ser negativo si el descuento supera el precio")
    void shouldNotAllowNegativeTotalWhenDiscountExceedsPrice() {
        // Descuento fijo de 999999 → total debe quedar en 0
        request.setDiscountCode("MEGA");

        DiscountCodeResponse discount = new DiscountCodeResponse();
        discount.setCode("MEGA");
        discount.setDiscountType("FIXED");
        discount.setDiscountValue(new BigDecimal("999999"));

        when(eventServiceClient.getTicketType(ticketTypeId)).thenReturn(ticketType);
        when(eventServiceClient.validateDiscountCode("MEGA")).thenReturn(discount);
        mockRepositorySave();

        OrderResponse result = createOrderUseCase.execute(request, userId);

        assertThat(result.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("No debe llamar a validateDiscountCode si no viene código de descuento")
    void shouldNotCallDiscountValidationWhenNoCode() {
        request.setDiscountCode(null);
        when(eventServiceClient.getTicketType(ticketTypeId)).thenReturn(ticketType);
        mockRepositorySave();

        createOrderUseCase.execute(request, userId);

        verify(eventServiceClient, never()).validateDiscountCode(any());
    }

    @Test
    @DisplayName("No debe llamar a validateDiscountCode si el código es blank")
    void shouldNotCallDiscountValidationWhenCodeIsBlank() {
        request.setDiscountCode("   ");
        when(eventServiceClient.getTicketType(ticketTypeId)).thenReturn(ticketType);
        mockRepositorySave();

        createOrderUseCase.execute(request, userId);

        verify(eventServiceClient, never()).validateDiscountCode(any());
    }
}