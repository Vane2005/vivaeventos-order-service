package co.edu.univalle.vivaeventosorderservice.infraestructure.messaging;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentApprovedMessage(
        UUID orderId,
        BigDecimal amount,
        String currency,
        String customerEmail,
        Instant paidAt
) implements Serializable {}