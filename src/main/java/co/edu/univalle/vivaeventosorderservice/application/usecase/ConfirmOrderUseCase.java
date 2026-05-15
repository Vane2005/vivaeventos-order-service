package co.edu.univalle.vivaeventosorderservice.application.usecase;

import co.edu.univalle.vivaeventosorderservice.domain.model.OrderStatus;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderEntity;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class ConfirmOrderUseCase {

    private final OrderJpaRepository orderRepository;

    public ConfirmOrderUseCase(OrderJpaRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderEntity execute(UUID orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Orden no encontrada"));

        if (order.getStatus() == OrderStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La orden ha expirado");
        }

        if (order.getStatus() == OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La orden ya fue confirmada");
        }

        order.setStatus(OrderStatus.PAID);
        return orderRepository.save(order);
    }
}