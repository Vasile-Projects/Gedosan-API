package it.vszdev.gedosanapi.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record VariazioneAperturaRequest(
        @NotEmpty List<Long> idTrasfusionali,
        @NotNull LocalDate dataVariazione,
        @NotNull Boolean apertura,
        @Size(max = 255) String motivo
) {
}
