package it.vszdev.gedosanapi.dto.admin;

import it.vszdev.gedosanapi.enums.Sesso;
import it.vszdev.gedosanapi.models.Donatore;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DonatoreAdminResponse(
        Long id,
        String nome,
        String cognome,
        LocalDate dataNascita,
        Sesso sesso,
        String codiceFiscale,
        String email,
        String cellulare,
        LocalDateTime createdAt
) {
    public DonatoreAdminResponse(Donatore d) {
        this(
                d.getId(),
                d.getNome(),
                d.getCognome(),
                d.getDataNascita(),
                d.getSesso(),
                d.getCodiceFiscale(),
                d.getEmail(),
                d.getCellulare(),
                d.getCreatedAt()
        );
    }
}
