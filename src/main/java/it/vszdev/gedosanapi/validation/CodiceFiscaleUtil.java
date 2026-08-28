package it.vszdev.gedosanapi.validation;

import java.util.Locale;
import java.util.regex.Pattern;

public final class CodiceFiscaleUtil {

    public static final Pattern FORMATO = Pattern.compile("[A-Z0-9]{16}");

    private CodiceFiscaleUtil() {
    }

    public static String normalizza(String codiceFiscale) {
        return codiceFiscale == null ? null : codiceFiscale.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean formatoValido(String codiceFiscaleNormalizzato) {
        return codiceFiscaleNormalizzato != null && FORMATO.matcher(codiceFiscaleNormalizzato).matches();
    }
}
