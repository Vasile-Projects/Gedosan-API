package it.vszdev.gedosanapi.dto.admin;

import jakarta.validation.constraints.NotNull;

public record RiprogrammazionePrenotazioneRequest(
        @NotNull Long idSlotNuovo
) {
}
