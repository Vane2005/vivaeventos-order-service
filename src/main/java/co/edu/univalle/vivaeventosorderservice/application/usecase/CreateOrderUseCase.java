package co.edu.univalle.vivaeventosorderservice.application.usecase;

import co.edu.univalle.vivaeventosorderservice.application.dto.CreateOrderRequest;
import co.edu.univalle.vivaeventosorderservice.application.dto.DiscountCodeResponse;
import co.edu.univalle.vivaeventosorderservice.application.dto.OrderResponse;
import co.edu.univalle.vivaeventosorderservice.application.dto.TicketTypeResponse;
import co.edu.univalle.vivaeventosorderservice.domain.model.OrderStatus;
import co.edu.univalle.vivaeventosorderservice.infraestructure.client.EventServiceClient;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderEntity;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderJpaRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class CreateOrderUseCase {

    private final EventServiceClient eventServiceClient;
    private final OrderJpaRepository orderRepository;

    public CreateOrderUseCase(EventServiceClient eventServiceClient,
                              OrderJpaRepository orderRepository) {
        this.eventServiceClient = eventServiceClient;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse execute(CreateOrderRequest request, UUID userId) {

        // 1. Consultar tipo de boleta y validar que existe
        TicketTypeResponse ticketType = eventServiceClient
                .getTicketType(request.getTicketTypeId());

        // 2. Validar stock
        if (ticketType.getQuantityAvailable() < request.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stock insuficiente. Disponible: " + ticketType.getQuantityAvailable());
        }

        // 3. Calcular precio base
        BigDecimal total = ticketType.getPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        // 3b. Aplicar descuento si viene código
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedCode = null;

        if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            DiscountCodeResponse discount = eventServiceClient
                    .validateDiscountCode(request.getDiscountCode());

            if ("PERCENTAGE".equals(discount.getDiscountType())) {
                discountAmount = total.multiply(discount.getDiscountValue())
                        .divide(BigDecimal.valueOf(100));
            } else {
                discountAmount = discount.getDiscountValue();
            }

            // El total no puede ser negativo
            total = total.subtract(discountAmount).max(BigDecimal.ZERO);
            appliedCode = discount.getCode();
        }

        // 4. Crear orden PENDING con expiración de 10 min
        OrderEntity order = new OrderEntity();
        order.setUserId(userId);
        order.setEventId(request.getEventId());
        order.setTicketTypeId(request.getTicketTypeId());
        order.setQuantity(request.getQuantity());
        order.setUnitPrice(ticketType.getPrice());
        order.setTotalPrice(total);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        order.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        order.setDiscountCode(appliedCode);
        order.setDiscountAmount(discountAmount);

        OrderEntity saved = orderRepository.save(order);

        // 5. Reservar stock en event-service
        eventServiceClient.reserveStock(request.getTicketTypeId(), request.getQuantity());

        return OrderResponse.from(saved);
    }
}