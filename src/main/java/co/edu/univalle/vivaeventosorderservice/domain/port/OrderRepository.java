package co.edu.univalle.vivaeventosorderservice.domain.port;

import co.edu.univalle.vivaeventosorderservice.domain.model.OrderStatus;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderEntity;

import java.time.Instant;
import java.util.List;

public interface OrderRepository {
    OrderEntity save(OrderEntity order);
    List<OrderEntity> findByStatusAndExpiresAtBefore(OrderStatus status, Instant now);
}