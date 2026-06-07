package co.edu.univalle.vivaeventosorderservice.infraestructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "vivaeventos.events";
    public static final String ROUTING_KEY_PAGO_APROBADO = "pago.aprobado";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }
}