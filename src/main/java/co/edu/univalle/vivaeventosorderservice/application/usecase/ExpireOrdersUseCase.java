package co.edu.univalle.vivaeventosorderservice.application.usecase;

import co.edu.univalle.vivaeventosorderservice.domain.model.OrderStatus;
import co.edu.univalle.vivaeventosorderservice.infraestructure.client.EventServiceClient;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderEntity;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderJpaRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ExpireOrdersUseCase {

    private final OrderJpaRepository orderRepository;
    private final EventServiceClient eventServiceClient;

    public ExpireOrdersUseCase(OrderJpaRepository orderRepository, EventServiceClient eventServiceClient) {
        this.orderRepository = orderRepository;
        this.eventServiceClient = eventServiceClient;
    }

    @Scheduled(fixedDelay = 60_000) // cada 60 segundos
    @Transactional
    public void expireOrders() {
        List<OrderEntity> expired = orderRepository
                .findByStatusAndExpiresAtBefore(OrderStatus.PENDING, Instant.now());

        for (OrderEntity order : expired) {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);

            // Devolver stock al event-service
            eventServiceClient.releaseStock(
                    order.getTicketTypeId(),
                    order.getQuantity()
            );
        }
    }
}
