package co.edu.univalle.vivaeventosorderservice.infraestructure.client;

import co.edu.univalle.vivaeventosorderservice.application.dto.DiscountCodeResponse;
import co.edu.univalle.vivaeventosorderservice.application.dto.TicketTypeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "event-service", url = "${services.event-service.url}")
public interface EventServiceClient {

    @GetMapping("/api/v1/events/ticket-types/{ticketTypeId}")
    TicketTypeResponse getTicketType(@PathVariable UUID ticketTypeId);

    @PutMapping("/api/v1/events/ticket-types/{ticketTypeId}/reserve")
    void reserveStock(@PathVariable UUID ticketTypeId, @RequestParam int quantity);

    @PutMapping("/api/v1/events/ticket-types/{ticketTypeId}/release")
    void releaseStock(@PathVariable UUID ticketTypeId, @RequestParam int quantity);

    @PostMapping("/api/v1/discount-codes/{code}/validate")
    DiscountCodeResponse validateDiscountCode(@PathVariable String code);

}