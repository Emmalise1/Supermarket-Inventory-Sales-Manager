package com.supermarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SUPERmarket Inventory &amp; Sales Manager.
 *
 * <p>Architecture: React Frontend -&gt; REST API -&gt; Spring Boot -&gt;
 * MySQL (source of truth), MongoDB (audit/notifications), Redis (cache only).
 * Async events: Spring Boot -&gt; RabbitMQ -&gt; consumers -&gt; notifications/audit.
 */
@SpringBootApplication
public class SupermarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupermarketApplication.class, args);
    }
}
