package co.edu.univalle.vivaeventosorderservice.application.usecase;

import co.edu.univalle.vivaeventosorderservice.application.dto.CreateOrderRequest;
import co.edu.univalle.vivaeventosorderservice.application.dto.DiscountCodeResponse;
import co.edu.univalle.vivaeventosorderservice.application.dto.TicketTypeResponse;
import co.edu.univalle.vivaeventosorderservice.domain.model.OrderStatus;
import co.edu.univalle.vivaeventosorderservice.infraestructure.client.EventServiceClient;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderEntity;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderConcurrencyTest {

    @Mock
    private EventServiceClient eventServiceClient;

    @Mock
    private OrderJpaRepository orderRepository;

    @InjectMocks
    private CreateOrderUseCase createOrderUseCase;

    private UUID ticketTypeId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        ticketTypeId = UUID.randomUUID();
        eventId = UUID.randomUUID();
    }

    @Test
    void multipleUsersBuyingSimultaneously_shouldHandleStockCorrectly() throws InterruptedException {
        // Solo hay 1 boleta disponible
        TicketTypeResponse ticketType = new TicketTypeResponse();
        ticketType.setId(ticketTypeId);
        ticketType.setPrice(new BigDecimal("50000"));
        ticketType.setQuantityAvailable(1);

        // El primer call retorna stock disponible, el segundo lanza conflicto
        when(eventServiceClient.getTicketType(ticketTypeId))
                .thenReturn(ticketType)
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Stock insuficiente. Disponible: 0"));

        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger conflict = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    CreateOrderRequest request = new CreateOrderRequest();
                    request.setEventId(eventId);
                    request.setTicketTypeId(ticketTypeId);
                    request.setQuantity(1);
                    createOrderUseCase.execute(request, UUID.randomUUID());
                    success.incrementAndGet();
                } catch (ResponseStatusException e) {
                    conflict.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Solo uno debe haber comprado
        assertEquals(1, success.get(), "Solo un usuario debe poder comprar");
        assertEquals(1, conflict.get(), "El otro debe recibir conflicto de stock");
    }
}