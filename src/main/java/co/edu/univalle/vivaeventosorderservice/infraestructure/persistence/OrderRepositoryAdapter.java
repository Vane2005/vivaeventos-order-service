package co.edu.univalle.vivaeventosorderservice.infraestructure.persistence;

import co.edu.univalle.vivaeventosorderservice.domain.model.OrderStatus;
import co.edu.univalle.vivaeventosorderservice.domain.port.OrderRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OrderEntity save(OrderEntity order) {
        return jpaRepository.save(order);
    }

    @Override
    public List<OrderEntity> findByStatusAndExpiresAtBefore(OrderStatus status, Instant now) {
        return jpaRepository.findByStatusAndExpiresAtBefore(status, now);
    }
}