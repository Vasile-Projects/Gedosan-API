package it.vszdev.gedosanapi.validation;

import java.text.Normalizer;
import java.util.Locale;

// Calcola le prime tre lettere del codice fiscale relative a cognome (posizioni 1-3) e nome
// (posizioni 4-6) a partire dal testo libero, secondo le regole dell'Agenzia delle Entrate.
// Ritorna null quando l'input, ripulito, non contiene lettere: in quel caso la coerenza non è
// verificabile e resta al solo checksum del CF.
final class CodiceCognomeNome {

    private static final String VOCALI = "AEIOU";

    private CodiceCognomeNome() {
    }

    static String cognome(String testo) {
        String pulito = pulisci(testo);
        if (pulito.isEmpty()) {
            return null;
        }
        return (consonanti(pulito) + vocali(pulito) + "XXX").substring(0, 3);
    }

    static String nome(String testo) {
        String pulito = pulisci(testo);
        if (pulito.isEmpty()) {
            return null;
        }
        String consonanti = consonanti(pulito);
        // Con almeno quattro consonanti si prendono la prima, la terza e la quarta.
        if (consonanti.length() >= 4) {
            return "" + consonanti.charAt(0) + consonanti.charAt(2) + consonanti.charAt(3);
        }
        return (consonanti + vocali(pulito) + "XXX").substring(0, 3);
    }

    // Maiuscolo, rimozione dei diacritici (À->A, É->E...) e di tutto ciò che non è A-Z
    // (spazi, apostrofi e trattini di cognomi/nomi composti: D'Angelo -> DANGELO, De Luca -> DELUCA).
    private static String pulisci(String testo) {
        if (testo == null) {
            return "";
        }
        String senzaDiacritici = Normalizer.normalize(testo, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return senzaDiacritici.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
    }

    private static String consonanti(String pulito) {
        return pulito.replaceAll("[AEIOU]", "");
    }

    private static String vocali(String pulito) {
        StringBuilder sb = new StringBuilder();
        for (char c : pulito.toCharArray()) {
            if (VOCALI.indexOf(c) >= 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
