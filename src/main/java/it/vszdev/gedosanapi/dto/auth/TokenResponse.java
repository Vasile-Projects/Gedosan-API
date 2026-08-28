package it.vszdev.gedosanapi.dto.auth;

import java.time.Instant;

public record TokenResponse(
        String token,
        Instant scadenza
) {
}
