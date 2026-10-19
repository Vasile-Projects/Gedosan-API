package it.vszdev.gedosanapi.dto.bot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// CF ed email come li ha scritti l'utente: maiuscole, minuscole e spazi li normalizza il backend.
public record RichiestaCodiceRequest(
        @NotNull Long chatId,
        @NotBlank @Size(max = 32) String codiceFiscale,
        @NotBlank @Size(max = 255) String email
) {
}
