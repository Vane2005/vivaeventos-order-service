package co.edu.univalle.vivaeventosorderservice.application.usecase;

import co.edu.univalle.vivaeventosorderservice.domain.model.OrderStatus;
import co.edu.univalle.vivaeventosorderservice.infraestructure.client.EventServiceClient;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderEntity;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpireOrdersUseCaseTest {

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private EventServiceClient eventServiceClient;

    @InjectMocks
    private ExpireOrdersUseCase expireOrdersUseCase;

    @Test
    void expiredOrder_shouldChangeStatusAndReleaseStock() {
        UUID ticketTypeId = UUID.randomUUID();

        OrderEntity expiredOrder = new OrderEntity();
        expiredOrder.setId(UUID.randomUUID());
        expiredOrder.setTicketTypeId(ticketTypeId);
        expiredOrder.setQuantity(2);
        expiredOrder.setStatus(OrderStatus.PENDING);
        expiredOrder.setExpiresAt(Instant.now().minusSeconds(60)); // ya expiró

        when(orderRepository.findByStatusAndExpiresAtBefore(
                eq(OrderStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(expiredOrder));

        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        expireOrdersUseCase.expireOrders();

        // Verificar que el estado cambió a EXPIRED
        assertEquals(OrderStatus.EXPIRED, expiredOrder.getStatus());

        // Verificar que se liberó el stock en event-service
        verify(eventServiceClient).releaseStock(ticketTypeId, 2);
        verify(orderRepository).save(expiredOrder);
    }

    @Test
    void noExpiredOrders_shouldDoNothing() {
        when(orderRepository.findByStatusAndExpiresAtBefore(
                eq(OrderStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of());

        expireOrdersUseCase.expireOrders();

        verify(eventServiceClient, never()).releaseStock(any(), anyInt());
        verify(orderRepository, never()).save(any());
    }
}