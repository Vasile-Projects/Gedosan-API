package it.vszdev.gedosanapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Map;


// Solo validazione formale (checksum); la coerenza con i dati dichiarati è in CoerenzaCodiceFiscaleValidator.
public class CodiceFiscaleValidator implements ConstraintValidator<CodiceFiscale, String> {

    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final Map<Character, Integer> VALORI_POSIZIONE_DISPARI = Map.ofEntries(
            Map.entry('0', 1), Map.entry('1', 0), Map.entry('2', 5), Map.entry('3', 7), Map.entry('4', 9),
            Map.entry('5', 13), Map.entry('6', 15), Map.entry('7', 17), Map.entry('8', 19), Map.entry('9', 21),
            Map.entry('A', 1), Map.entry('B', 0), Map.entry('C', 5), Map.entry('D', 7), Map.entry('E', 9),
            Map.entry('F', 13), Map.entry('G', 15), Map.entry('H', 17), Map.entry('I', 19), Map.entry('J', 21),
            Map.entry('K', 2), Map.entry('L', 4), Map.entry('M', 18), Map.entry('N', 20), Map.entry('O', 11),
            Map.entry('P', 3), Map.entry('Q', 6), Map.entry('R', 8), Map.entry('S', 12), Map.entry('T', 14),
            Map.entry('U', 16), Map.entry('V', 10), Map.entry('W', 22), Map.entry('X', 25), Map.entry('Y', 24),
            Map.entry('Z', 23)
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String cf = CodiceFiscaleUtil.normalizza(value);
        if (!CodiceFiscaleUtil.formatoValido(cf)) {
            return false;
        }

        int somma = 0;
        for (int posizione = 0; posizione < 15; posizione++) {
            char carattere = cf.charAt(posizione);
            boolean posizioneDispari = posizione % 2 == 0;
            somma += posizioneDispari ? VALORI_POSIZIONE_DISPARI.get(carattere) : valorePosizionePari(carattere);
        }

        char carattereControlloAtteso = ALFABETO.charAt(somma % 26);
        return carattereControlloAtteso == cf.charAt(15);
    }

    private int valorePosizionePari(char carattere) {
        return Character.isDigit(carattere) ? (carattere - '0') : (carattere - 'A');
    }
}
