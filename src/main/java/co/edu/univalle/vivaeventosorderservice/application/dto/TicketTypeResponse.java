package co.edu.univalle.vivaeventosorderservice.application.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;


@Getter
public class TicketTypeResponse {
    private UUID id;
    private String type;
    private BigDecimal price;
    private Integer quantityAvailable;

}