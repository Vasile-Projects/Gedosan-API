package it.vszdev.gedosanapi.dto;

import it.vszdev.gedosanapi.enums.Sesso;
import it.vszdev.gedosanapi.enums.TipoDonazione;
import it.vszdev.gedosanapi.validation.CodiceFiscale;
import it.vszdev.gedosanapi.validation.CoerenzaCodiceFiscale;
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
        @Pattern(regexp = NOME_REGEX, message = "Il nome può contenere solo lettere, spazi, apostrofi, punti e trattini")
        String nome,
        @NotBlank @Size(max = 120)
        @Pattern(regexp = NOME_REGEX, message = "Il cognome può contenere solo lettere, spazi, apostrofi, punti e trattini")
        String cognome,
        @NotNull @Past LocalDate dataNascita,
        @NotNull Sesso sesso,
        @NotBlank @Size(max = 16) @CodiceFiscale String codiceFiscale,
        @NotBlank @Email @Size(max = 255) String email,
        // Allineato al vincolo DB chk_donatori_cellulare. Il frontend antepone "+39" (non modificabile);
        // il pattern qui è la rete di sicurezza se quel prefisso mancasse o arrivassero altri caratteri.
        @NotBlank @Size(max = 15)
        @Pattern(regexp = "^\\+39[0-9]{9,10}$", message = "Il cellulare deve essere nel formato +39 seguito da 9 o 10 cifre")
        String cellulare,
        @NotNull Long idSlot,
        @NotNull TipoDonazione tipoDonazione
) {
    // Una lettera iniziale (accenti e alfabeti non latini inclusi via \p{L}), poi lettere, spazi,
    // apostrofi (dritto e tipografico), punti e trattini: copre nomi composti e stranieri.
    private static final String NOME_REGEX = "^\\p{L}[\\p{L} '.’-]*$";

    public PrenotazioneRequest {
        nome = normalizzaTesto(nome);
        cognome = normalizzaTesto(cognome);
    }

    // trim e collasso degli spazi multipli: il valore normalizzato è quello validato e quello salvato.
    private static String normalizzaTesto(String valore) {
        return valore == null ? null : valore.trim().replaceAll("\\s{2,}", " ");
    }
}
