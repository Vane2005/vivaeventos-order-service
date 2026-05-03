package co.edu.univalle.vivaeventosorderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class VivaeventosOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VivaeventosOrderServiceApplication.class, args);
    }

}
