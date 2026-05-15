package co.edu.univalle.vivaeventosorderservice.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class Order {
    private String id;
    private String ticketType;
    private int quantity;
    private BigDecimal price;
    private OrderStatus status;

    public Order(String id, String ticketType, int quantity, BigDecimal price, OrderStatus status) {
        this.id = id;
        this.ticketType = ticketType;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
    }

    // Constructor without ID for new orders, where ID will be generated
    public Order(String ticketType, int quantity, BigDecimal price, OrderStatus status) {
        this.id = UUID.randomUUID().toString(); // Generate ID for new orders
        this.ticketType = ticketType;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
    }

}
