package co.edu.univalle.vivaeventosorderservice.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateOrderRequest {
    @NotNull
    private UUID eventId;
    @NotNull
    private UUID ticketTypeId;
    @NotNull @Min(1)
    private Integer quantity;
    private String discountCode;
}