package co.edu.univalle.vivaeventosorderservice.infraestructure.web;

import co.edu.univalle.vivaeventosorderservice.application.dto.CreateOrderRequest;
import co.edu.univalle.vivaeventosorderservice.application.dto.OrderResponse;
import co.edu.univalle.vivaeventosorderservice.application.usecase.ConfirmOrderUseCase;
import co.edu.univalle.vivaeventosorderservice.application.usecase.CreateOrderUseCase;
import co.edu.univalle.vivaeventosorderservice.application.usecase.GetOrderUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final GetOrderUseCase getOrderUseCase;

    private final ConfirmOrderUseCase confirmOrderUseCase;

    public OrderController(
            CreateOrderUseCase createOrderUseCase,
            ConfirmOrderUseCase confirmOrderUseCase,
            GetOrderUseCase getOrderUseCase) {

        this.createOrderUseCase = createOrderUseCase;
        this.confirmOrderUseCase = confirmOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
    }

    private final CreateOrderUseCase createOrderUseCase;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = createOrderUseCase.execute(request, UUID.fromString(userId));
        return ResponseEntity.status(201).body(order);
    }

    @PatchMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(OrderResponse.from(confirmOrderUseCase.execute(orderId)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId) {

        return ResponseEntity.ok(
                OrderResponse.from(
                        getOrderUseCase.execute(orderId)
                )
        );
    }
}