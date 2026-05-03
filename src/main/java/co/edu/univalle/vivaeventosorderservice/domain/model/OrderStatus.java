package co.edu.univalle.vivaeventosorderservice.domain.model;

public enum OrderStatus {
    PENDING,    // esperando pago
    PAID,       // pago confirmado
    CANCELLED,  // cancelada o expirada
    EXPIRED
}