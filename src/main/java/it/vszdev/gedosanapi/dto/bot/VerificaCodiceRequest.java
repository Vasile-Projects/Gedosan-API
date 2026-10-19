package it.vszdev.gedosanapi.dto.bot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerificaCodiceRequest(
        @NotNull Long chatId,
        @NotBlank @Size(max = 16) String codice
) {
}
