package co.edu.univalle.vivaeventosorderservice.application.usecase;

import co.edu.univalle.vivaeventosorderservice.domain.model.OrderStatus;
import co.edu.univalle.vivaeventosorderservice.infraestructure.messaging.OrderEventPublisher;
import co.edu.univalle.vivaeventosorderservice.infraestructure.messaging.PaymentApprovedMessage;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmOrderUseCaseTest {

    @Mock private OrderJpaRepository orderRepository;
    @Mock private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private ConfirmOrderUseCase confirmOrderUseCase;

    private UUID orderId;
    private OrderEntity pendingOrder;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        pendingOrder = new OrderEntity();
        pendingOrder.setId(orderId);
        pendingOrder.setStatus(OrderStatus.PENDING);
        pendingOrder.setTotalPrice(new BigDecimal("100000"));
        pendingOrder.setCurrency("COP");
        pendingOrder.setCustomerEmail("test@test.com");
    }

    @Test
    @DisplayName("Debe confirmar orden y cambiar estado a PAID")
    void shouldConfirmOrderAndChangeStatusToPaid() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenReturn(pendingOrder);

        OrderEntity result = confirmOrderUseCase.execute(orderId);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(pendingOrder);
    }

    @Test
    @DisplayName("Debe publicar evento de pago aprobado al confirmar")
    void shouldPublishPaymentApprovedEvent() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenReturn(pendingOrder);

        confirmOrderUseCase.execute(orderId);

        ArgumentCaptor<PaymentApprovedMessage> captor =
                ArgumentCaptor.forClass(PaymentApprovedMessage.class);
        verify(orderEventPublisher).publishPaymentApproved(captor.capture());

        PaymentApprovedMessage message = captor.getValue();
        assertThat(message.customerEmail()).isEqualTo("test@test.com");
        assertThat(message.currency()).isEqualTo("COP");
        assertThat(message.amount()).isEqualByComparingTo(new BigDecimal("100000"));
    }

    @Test
    @DisplayName("Debe lanzar 404 cuando la orden no existe")
    void shouldThrow404WhenOrderNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> confirmOrderUseCase.execute(orderId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Orden no encontrada");

        verify(orderEventPublisher, never()).publishPaymentApproved(any());
    }

    @Test
    @DisplayName("Debe lanzar 409 cuando la orden ya está pagada")
    void shouldThrow409WhenOrderAlreadyPaid() {
        pendingOrder.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> confirmOrderUseCase.execute(orderId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ya fue confirmada");

        verify(orderEventPublisher, never()).publishPaymentApproved(any());
    }

    @Test
    @DisplayName("Debe lanzar 409 cuando la orden está expirada")
    void shouldThrow409WhenOrderIsExpired() {
        pendingOrder.setStatus(OrderStatus.EXPIRED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> confirmOrderUseCase.execute(orderId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("expirado");

        verify(orderEventPublisher, never()).publishPaymentApproved(any());
    }
}