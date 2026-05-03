package co.edu.univalle.vivaeventosorderservice.infraestructure.web;

import co.edu.univalle.vivaeventosorderservice.application.dto.CreateOrderRequest;
import co.edu.univalle.vivaeventosorderservice.application.dto.OrderResponse;
import co.edu.univalle.vivaeventosorderservice.application.usecase.CreateOrderUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = createOrderUseCase.execute(request, UUID.fromString(userId));
        return ResponseEntity.status(201).body(order);
    }
}