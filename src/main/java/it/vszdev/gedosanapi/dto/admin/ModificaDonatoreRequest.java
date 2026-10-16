package it.vszdev.gedosanapi.dto.admin;

import it.vszdev.gedosanapi.enums.Sesso;
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

// Anagrafica completa del donatore modificata dal centro trasfusionale. Si inviano sempre tutti i
// campi (il frontend parte dal dettaglio del donatore): così la coerenza con il codice fiscale si
// verifica sempre sull'insieme finale, con le stesse regole del modulo di prenotazione.
@CoerenzaCodiceFiscale
public record ModificaDonatoreRequest(
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
        @NotBlank @Size(max = 15)
        @Pattern(regexp = DatiAnagrafici.CELLULARE_REGEX, message = DatiAnagrafici.CELLULARE_MESSAGGIO)
        String cellulare
) implements DatiAnagrafici {

    public ModificaDonatoreRequest {
        nome = DatiAnagrafici.normalizzaTesto(nome);
        cognome = DatiAnagrafici.normalizzaTesto(cognome);
    }
}
