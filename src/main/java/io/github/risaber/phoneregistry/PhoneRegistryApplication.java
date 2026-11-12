package io.github.risaber.phoneregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа Spring Boot. Веб-сервер не поднимается
 * (spring.main.web-application-type=none): сервис используется как компонент.
 */
@SpringBootApplication
public class PhoneRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhoneRegistryApplication.class, args);
    }
}
