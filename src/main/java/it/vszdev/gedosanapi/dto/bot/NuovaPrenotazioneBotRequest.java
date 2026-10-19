package it.vszdev.gedosanapi.dto.bot;

import jakarta.validation.constraints.NotNull;

public record NuovaPrenotazioneBotRequest(@NotNull Long idSlot) {
}
