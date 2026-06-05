package co.edu.univalle.vivaeventosorderservice.infraestructure.web;

import co.edu.univalle.vivaeventosorderservice.application.dto.CreateOrderRequest;
import co.edu.univalle.vivaeventosorderservice.application.dto.OrderResponse;
import co.edu.univalle.vivaeventosorderservice.application.usecase.ConfirmOrderUseCase;
import co.edu.univalle.vivaeventosorderservice.application.usecase.CreateOrderUseCase;
import co.edu.univalle.vivaeventosorderservice.application.usecase.GetOrderUseCase;
import co.edu.univalle.vivaeventosorderservice.domain.model.OrderStatus;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest carga SOLO la capa web — mucho más rápido que @SpringBootTest
@WebMvcTest(OrderController.class)
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mockear los use cases — no queremos lógica de negocio real aquí
    @MockitoBean private CreateOrderUseCase createOrderUseCase;
    @MockitoBean private ConfirmOrderUseCase confirmOrderUseCase;
    @MockitoBean private GetOrderUseCase getOrderUseCase;

    private UUID userId;
    private UUID eventId;
    private UUID ticketTypeId;
    private UUID orderId;
    private OrderResponse sampleResponse;

    @BeforeEach
    void setUp() {
        userId       = UUID.randomUUID();
        eventId      = UUID.randomUUID();
        ticketTypeId = UUID.randomUUID();
        orderId      = UUID.randomUUID();

        // Construir una OrderEntity de muestra para generar el OrderResponse
        OrderEntity entity = new OrderEntity();
        entity.setId(orderId);
        entity.setUserId(userId);
        entity.setEventId(eventId);
        entity.setTicketTypeId(ticketTypeId);
        entity.setQuantity(2);
        entity.setUnitPrice(new BigDecimal("50000"));
        entity.setTotalPrice(new BigDecimal("100000"));
        entity.setStatus(OrderStatus.PENDING);
        entity.setCustomerEmail("test@test.com");
        entity.setCurrency("COP");
        entity.setCreatedAt(Instant.now());
        entity.setExpiresAt(Instant.now().plusSeconds(600));

        sampleResponse = OrderResponse.from(entity);
    }

    // ── POST /api/v1/orders ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /orders debe retornar 201 con la orden creada")
    void shouldReturn201WhenOrderCreated() throws Exception {
        CreateOrderRequest request = buildRequest(2, null);

        when(createOrderUseCase.execute(any(CreateOrderRequest.class), eq(userId)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.currency").value("COP"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(100000));
    }

    @Test
    @DisplayName("POST /orders sin header X-User-Id debe retornar 400")
    void shouldReturn400WhenUserIdHeaderMissing() throws Exception {
        CreateOrderRequest request = buildRequest(2, null);

        mockMvc.perform(post("/api/v1/orders")
                        // Sin header X-User-Id
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /orders con quantity < 1 debe retornar 400 (validación @Min)")
    void shouldReturn400WhenQuantityIsZero() throws Exception {
        CreateOrderRequest request = buildRequest(0, null); // quantity = 0 viola @Min(1)

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /orders sin eventId debe retornar 400 (validación @NotNull)")
    void shouldReturn400WhenEventIdIsNull() throws Exception {
        CreateOrderRequest request = buildRequest(2, null);
        request.setEventId(null); // viola @NotNull

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH /api/v1/orders/{orderId}/confirm ────────────────────────────────

    @Test
    @DisplayName("PATCH /orders/{id}/confirm debe retornar 200 con orden confirmada")
    void shouldReturn200WhenOrderConfirmed() throws Exception {
        // Reusar la entity pero con status PAID
        OrderEntity paidEntity = new OrderEntity();
        paidEntity.setId(orderId);
        paidEntity.setUserId(userId);
        paidEntity.setEventId(eventId);
        paidEntity.setTicketTypeId(ticketTypeId);
        paidEntity.setQuantity(2);
        paidEntity.setUnitPrice(new BigDecimal("50000"));
        paidEntity.setTotalPrice(new BigDecimal("100000"));
        paidEntity.setStatus(OrderStatus.PAID);
        paidEntity.setCustomerEmail("test@test.com");
        paidEntity.setCurrency("COP");
        paidEntity.setCreatedAt(Instant.now());
        paidEntity.setExpiresAt(Instant.now().plusSeconds(600));

        when(confirmOrderUseCase.execute(orderId))
                .thenReturn(paidEntity);

        mockMvc.perform(patch("/api/v1/orders/{orderId}/confirm", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    // ── GET /api/v1/orders/{orderId} ──────────────────────────────────────────

    @Test
    @DisplayName("GET /orders/{id} debe retornar 200 con la orden encontrada")
    void shouldReturn200WhenOrderFound() throws Exception {
        OrderEntity entity = new OrderEntity();
        entity.setId(orderId);
        entity.setUserId(userId);
        entity.setEventId(eventId);
        entity.setTicketTypeId(ticketTypeId);
        entity.setQuantity(2);
        entity.setUnitPrice(new BigDecimal("50000"));
        entity.setTotalPrice(new BigDecimal("100000"));
        entity.setStatus(OrderStatus.PENDING);
        entity.setCustomerEmail("test@test.com");
        entity.setCurrency("COP");
        entity.setCreatedAt(Instant.now());
        entity.setExpiresAt(Instant.now().plusSeconds(600));

        when(getOrderUseCase.execute(orderId)).thenReturn(entity);

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private CreateOrderRequest buildRequest(int quantity, String discountCode) {
        CreateOrderRequest r = new CreateOrderRequest();
        r.setEventId(eventId);
        r.setTicketTypeId(ticketTypeId);
        r.setQuantity(quantity);
        r.setCustomerEmail("test@test.com");
        r.setDiscountCode(discountCode);
        return r;
    }
}