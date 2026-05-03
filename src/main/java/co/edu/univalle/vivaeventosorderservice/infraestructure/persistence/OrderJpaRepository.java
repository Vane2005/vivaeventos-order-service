package co.edu.univalle.vivaeventosorderservice.infraestructure.persistence;

import co.edu.univalle.vivaeventosorderservice.domain.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findByStatusAndExpiresAtBefore(OrderStatus status, Instant now);
}
