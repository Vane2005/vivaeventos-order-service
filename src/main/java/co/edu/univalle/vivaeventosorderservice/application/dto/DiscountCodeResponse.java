package co.edu.univalle.vivaeventosorderservice.application.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class DiscountCodeResponse {
    private String code;
    private String discountType;
    private BigDecimal discountValue;

}