package it.vszdev.gedosanapi.validation;

import it.vszdev.gedosanapi.enums.Sesso;

import java.util.Map;

record CfDecodificato(int anno, int mese, int giorno, Sesso sesso) {

    private static final Map<Character, Integer> MESE = Map.ofEntries(
            Map.entry('A', 1), Map.entry('B', 2), Map.entry('C', 3), Map.entry('D', 4),
            Map.entry('E', 5), Map.entry('H', 6), Map.entry('L', 7), Map.entry('M', 8),
            Map.entry('P', 9), Map.entry('R', 10), Map.entry('S', 11), Map.entry('T', 12)
    );

    private static final Map<Character, Character> OMOCODIA = Map.ofEntries(
            Map.entry('L', '0'), Map.entry('M', '1'), Map.entry('N', '2'), Map.entry('P', '3'),
            Map.entry('Q', '4'), Map.entry('R', '5'), Map.entry('S', '6'), Map.entry('T', '7'),
            Map.entry('U', '8'), Map.entry('V', '9')
    );

    // Ritorna null se il CF non è nel formato standard: la coerenza non è verificabile e resta al checksum.
    static CfDecodificato da(String codiceFiscaleNormalizzato) {
        Integer mese = MESE.get(codiceFiscaleNormalizzato.charAt(8));
        Integer anno = decodificaDueCifre(codiceFiscaleNormalizzato.charAt(6), codiceFiscaleNormalizzato.charAt(7));
        Integer giornoSesso = decodificaDueCifre(codiceFiscaleNormalizzato.charAt(9), codiceFiscaleNormalizzato.charAt(10));

        if (mese == null || anno == null || giornoSesso == null) {
            return null;
        }

        // Convenzione CF: per le donne al giorno di nascita si somma 40.
        Sesso sesso = giornoSesso > 40 ? Sesso.F : Sesso.M;
        int giorno = giornoSesso > 40 ? giornoSesso - 40 : giornoSesso;
        return new CfDecodificato(anno, mese, giorno, sesso);
    }

    private static Integer decodificaDueCifre(char decine, char unita) {
        Integer d = decodificaCifra(decine);
        Integer u = decodificaCifra(unita);
        return (d == null || u == null) ? null : d * 10 + u;
    }

    private static Integer decodificaCifra(char c) {
        if (Character.isDigit(c)) {
            return c - '0';
        }
        Character sostituito = OMOCODIA.get(c);
        return sostituito == null ? null : sostituito - '0';
    }
}
