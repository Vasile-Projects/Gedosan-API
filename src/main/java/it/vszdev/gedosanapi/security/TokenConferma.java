package it.vszdev.gedosanapi.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

// Token monouso del link di conferma. In chiaro esiste solo nell'email; a database si salva l'hash,
// così chi leggesse la tabella non potrebbe confermare prenotazioni altrui. SHA-256 senza sale basta:
// il token è casuale a 256 bit, non indovinabile né soggetto ad attacchi a dizionario come una password.
public final class TokenConferma {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int BYTE_TOKEN = 32;

    private TokenConferma() {
    }

    // Base64 URL-safe senza padding: si inserisce nel link senza codifiche.
    public static String genera() {
        byte[] bytes = new byte[BYTE_TOKEN];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // Esadecimale minuscolo a 64 caratteri, come la colonna token_hash.
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponibile", e);
        }
    }
}
