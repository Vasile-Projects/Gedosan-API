package it.vszdev.gedosanapi.validation;

import it.vszdev.gedosanapi.enums.Sesso;

import java.time.LocalDate;

// Dati anagrafici verificabili contro il codice fiscale con @CoerenzaCodiceFiscale. Implementata dai
// DTO che li ricevono (prenotazione pubblica/admin, modifica del donatore dall'area admin), così le
// regole di formato e di coerenza restano una sola.
public interface DatiAnagrafici {

    // Una lettera iniziale (accenti e alfabeti non latini inclusi via \p{L}), poi lettere, spazi,
    // apostrofi (dritto e tipografico), punti e trattini: copre nomi composti e stranieri.
    String NOME_REGEX = "^\\p{L}[\\p{L} '.’-]*$";
    String NOME_MESSAGGIO = "Il nome può contenere solo lettere, spazi, apostrofi, punti e trattini";
    String COGNOME_MESSAGGIO = "Il cognome può contenere solo lettere, spazi, apostrofi, punti e trattini";

    // Allineato al vincolo DB chk_donatori_cellulare.
    String CELLULARE_REGEX = "^\\+39[0-9]{9,10}$";
    String CELLULARE_MESSAGGIO = "Il cellulare deve essere nel formato +39 seguito da 9 o 10 cifre";

    String nome();

    String cognome();

    LocalDate dataNascita();

    Sesso sesso();

    String codiceFiscale();

    // trim e collasso degli spazi multipli: il valore normalizzato è quello validato e quello salvato.
    static String normalizzaTesto(String valore) {
        return valore == null ? null : valore.trim().replaceAll("\\s{2,}", " ");
    }
}
