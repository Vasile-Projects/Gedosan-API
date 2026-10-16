package it.vszdev.gedosanapi.dto.donatore;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Token del link di conferma (43 caratteri base64url): il limite di lunghezza scarta input anomali
// prima ancora di calcolarne l'hash.
public record ConfermaPrenotazioneRequest(
        @NotBlank @Size(max = 100) String token
) {
}
