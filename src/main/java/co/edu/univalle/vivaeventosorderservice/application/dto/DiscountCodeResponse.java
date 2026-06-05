package co.edu.univalle.vivaeventosorderservice.application.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DiscountCodeResponse {
    private String code;
    private String discountType;
    private BigDecimal discountValue;

}