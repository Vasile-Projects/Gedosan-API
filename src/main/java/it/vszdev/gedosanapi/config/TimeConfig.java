package it.vszdev.gedosanapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    // Fuso di riferimento del centro trasfusionale: tutte le date/orari di apertura e degli slot sono
    // espressi in ora italiana (lo stesso serverTimezone usato dal datasource). Iniettare Clock invece
    // di usare LocalDate.now()/LocalDateTime.now() rende deterministici i test sulle regole temporali.
    public static final ZoneId ZONA = ZoneId.of("Europe/Rome");

    @Bean
    public Clock clock() {
        return Clock.system(ZONA);
    }
}
