package co.edu.univalle.vivaeventosorderservice.infraestructure.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderEventPublisher orderEventPublisher;

    @Test
    @DisplayName("Debe publicar mensaje de pago aprobado en el exchange correcto")
    void shouldPublishPaymentApprovedMessage() {
        PaymentApprovedMessage message = new PaymentApprovedMessage(
                UUID.randomUUID(),
                new BigDecimal("100000"),
                "COP",
                "test@test.com",
                Instant.now()
        );

        orderEventPublisher.publishPaymentApproved(message);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_PAGO_APROBADO,
                message
        );
    }
}