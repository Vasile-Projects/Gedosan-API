package it.vszdev.gedosanapi.dto.donatore;

import it.vszdev.gedosanapi.enums.Sesso;
import it.vszdev.gedosanapi.validation.CodiceFiscale;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DatiDonatoreRequest(
        @NotBlank @Size(max = 60) String nome,
        @NotBlank @Size(max = 120) String cognome,
        @NotNull @Past LocalDate dataNascita,
        @NotNull Sesso sesso,
        @NotBlank @Size(max = 16) @CodiceFiscale String codiceFiscale,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 15) String cellulare
) {
}
