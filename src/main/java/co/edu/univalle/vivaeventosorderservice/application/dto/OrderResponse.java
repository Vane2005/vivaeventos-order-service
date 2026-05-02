package co.edu.univalle.vivaeventosorderservice.application.dto;

import co.edu.univalle.vivaeventosorderservice.domain.model.OrderStatus;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderEntity;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class OrderResponse {
    private UUID id;
    private UUID userId;
    private UUID eventId;
    private UUID ticketTypeId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private Instant expiresAt;
    private Instant createdAt;

    public static OrderResponse from(OrderEntity e) {
        OrderResponse r = new OrderResponse();
        r.id = e.getId();
        r.userId = e.getUserId();
        r.eventId = e.getEventId();
        r.ticketTypeId = e.getTicketTypeId();
        r.quantity = e.getQuantity();
        r.unitPrice = e.getUnitPrice();
        r.totalPrice = e.getTotalPrice();
        r.status = e.getStatus();
        r.expiresAt = e.getExpiresAt();
        r.createdAt = e.getCreatedAt();
        return r;
    }

}