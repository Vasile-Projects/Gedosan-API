package it.vszdev.gedosanapi.support;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Genera codici fiscali formalmente validi (con carattere di controllo) per persone inventate,
// coerenti con nome, cognome, data di nascita e sesso: superano @CodiceFiscale e @CoerenzaCodiceFiscale.
public final class CodiceFiscaleTest {

    private static final String VOCALI = "AEIOU";
    private static final String MESI = "ABCDEHLMPRST";
    private static final int[] DISPARI = {1, 0, 5, 7, 9, 13, 15, 17, 19, 21, 2, 4, 18, 20, 11, 3, 6, 8, 12, 14, 16, 10, 22, 25, 24, 23};

    private CodiceFiscaleTest() {
    }

    public static String genera(String cognome, String nome, LocalDate dataNascita, boolean donna) {
        int giorno = dataNascita.getDayOfMonth() + (donna ? 40 : 0);
        String base = codice(cognome, false) + codice(nome, true)
                + String.format("%02d", dataNascita.getYear() % 100)
                + MESI.charAt(dataNascita.getMonthValue() - 1)
                + String.format("%02d", giorno)
                + "H501";
        int somma = 0;
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            int v = Character.isDigit(c) ? c - '0' : c - 'A';
            somma += i % 2 == 0 ? DISPARI[v] : v;
        }
        return base + (char) ('A' + somma % 26);
    }

    private static String codice(String testo, boolean nome) {
        String s = testo.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
        List<Character> consonanti = new ArrayList<>();
        List<Character> vocali = new ArrayList<>();
        for (char c : s.toCharArray()) {
            (VOCALI.indexOf(c) >= 0 ? vocali : consonanti).add(c);
        }
        if (nome && consonanti.size() >= 4) {
            consonanti = List.of(consonanti.get(0), consonanti.get(2), consonanti.get(3));
        }
        StringBuilder sb = new StringBuilder();
        consonanti.forEach(sb::append);
        vocali.forEach(sb::append);
        sb.append("XXX");
        return sb.substring(0, 3);
    }
}
