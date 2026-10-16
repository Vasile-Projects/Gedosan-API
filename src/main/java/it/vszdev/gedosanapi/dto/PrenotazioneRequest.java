package it.vszdev.gedosanapi.dto;

import it.vszdev.gedosanapi.enums.Sesso;
import it.vszdev.gedosanapi.enums.TipoDonazione;
import it.vszdev.gedosanapi.validation.CodiceFiscale;
import it.vszdev.gedosanapi.validation.CoerenzaCodiceFiscale;
import it.vszdev.gedosanapi.validation.DatiAnagrafici;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@CoerenzaCodiceFiscale
public record PrenotazioneRequest(
        @NotBlank @Size(max = 60)
        @Pattern(regexp = DatiAnagrafici.NOME_REGEX, message = DatiAnagrafici.NOME_MESSAGGIO)
        String nome,
        @NotBlank @Size(max = 120)
        @Pattern(regexp = DatiAnagrafici.NOME_REGEX, message = DatiAnagrafici.COGNOME_MESSAGGIO)
        String cognome,
        @NotNull @Past LocalDate dataNascita,
        @NotNull Sesso sesso,
        @NotBlank @Size(max = 16) @CodiceFiscale String codiceFiscale,
        @NotBlank @Email @Size(max = 255) String email,
        // Allineato al vincolo DB chk_donatori_cellulare. Il frontend antepone "+39" (non modificabile);
        // il pattern qui è la rete di sicurezza se quel prefisso mancasse o arrivassero altri caratteri.
        @NotBlank @Size(max = 15)
        @Pattern(regexp = DatiAnagrafici.CELLULARE_REGEX, message = DatiAnagrafici.CELLULARE_MESSAGGIO)
        String cellulare,
        @NotNull Long idSlot,
        @NotNull TipoDonazione tipoDonazione,
        // Token del widget Cloudflare Turnstile. Facoltativo qui perché lo stesso DTO serve anche
        // all'area admin; nel flusso pubblico lo verifica TurnstileService.
        @Size(max = 2048) String turnstileToken
) implements DatiAnagrafici {

    public PrenotazioneRequest {
        nome = DatiAnagrafici.normalizzaTesto(nome);
        cognome = DatiAnagrafici.normalizzaTesto(cognome);
    }
}
