package co.edu.univalle.vivaeventosorderservice.application.usecase;

import co.edu.univalle.vivaeventosorderservice.application.dto.CreateOrderRequest;
import co.edu.univalle.vivaeventosorderservice.application.dto.TicketTypeResponse;
import co.edu.univalle.vivaeventosorderservice.infraestructure.client.EventServiceClient;
import co.edu.univalle.vivaeventosorderservice.infraestructure.persistence.OrderJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderDiscountTest {

    @Mock
    private EventServiceClient eventServiceClient;

    @Mock
    private OrderJpaRepository orderRepository;

    @InjectMocks
    private CreateOrderUseCase createOrderUseCase;

    @Test
    void invalidDiscountCode_shouldRejectOrder() {
        UUID ticketTypeId = UUID.randomUUID();

        TicketTypeResponse ticketType = new TicketTypeResponse();
        ticketType.setId(ticketTypeId);
        ticketType.setPrice(new BigDecimal("100000"));
        ticketType.setQuantityAvailable(10);

        when(eventServiceClient.getTicketType(ticketTypeId)).thenReturn(ticketType);

        // El event-service rechaza el código
        when(eventServiceClient.validateDiscountCode("INVALIDO"))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Código de descuento no encontrado"));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setEventId(UUID.randomUUID());
        request.setTicketTypeId(ticketTypeId);
        request.setQuantity(1);
        request.setDiscountCode("INVALIDO");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> createOrderUseCase.execute(request, UUID.randomUUID()));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(orderRepository, never()).save(any()); // no debe guardar la orden
    }

    @Test
    void expiredDiscountCode_shouldRejectOrder() {
        UUID ticketTypeId = UUID.randomUUID();

        TicketTypeResponse ticketType = new TicketTypeResponse();
        ticketType.setId(ticketTypeId);
        ticketType.setPrice(new BigDecimal("100000"));
        ticketType.setQuantityAvailable(10);

        when(eventServiceClient.getTicketType(ticketTypeId)).thenReturn(ticketType);

        when(eventServiceClient.validateDiscountCode("EXPIRADO"))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT, "Código de descuento fuera de vigencia"));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setEventId(UUID.randomUUID());
        request.setTicketTypeId(ticketTypeId);
        request.setQuantity(1);
        request.setDiscountCode("EXPIRADO");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> createOrderUseCase.execute(request, UUID.randomUUID()));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(orderRepository, never()).save(any());
    }
}