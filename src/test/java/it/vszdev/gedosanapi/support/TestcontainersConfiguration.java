package it.vszdev.gedosanapi.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

// MySQL vero, stessa versione della produzione: CHECK, vincoli UNIQUE e lock pessimistici si comportano
// come in esercizio. Il container è condiviso da tutti i test grazie alla cache del contesto Spring.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysql() {
        return new MySQLContainer(DockerImageName.parse("mysql:9.7"));
    }
}
