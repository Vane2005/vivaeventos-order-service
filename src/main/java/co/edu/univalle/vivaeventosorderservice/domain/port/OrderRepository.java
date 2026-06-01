package co.edu.univalle.vivaeventosorderservice.domain.port;

import co.edu.univalle.vivaeventosorderservice.domain.model.OrderStatus;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    OrderEntity save(OrderEntity order);

    Optional<OrderEntity> findById(UUID id);

    List<OrderEntity> findByStatusAndExpiresAtBefore(
            OrderStatus status,
            Instant now
    );
}