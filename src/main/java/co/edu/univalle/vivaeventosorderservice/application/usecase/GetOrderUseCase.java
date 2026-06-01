package co.edu.univalle.vivaeventosorderservice.application.usecase;

import co.edu.univalle.vivaeventosorderservice.domain.port.OrderRepository;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetOrderUseCase {
    private final OrderRepository orderRepository;

    public GetOrderUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderEntity execute(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Orden no encontrada: " + orderId));
    }
}
