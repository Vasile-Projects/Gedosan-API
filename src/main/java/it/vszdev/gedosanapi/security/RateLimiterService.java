package it.vszdev.gedosanapi.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimiterService {

    private static final int LOGIN_MAX_RICHIESTE = 5;
    private static final Duration LOGIN_FINESTRA = Duration.ofMinutes(5);

    private static final int PRENOTAZIONE_MAX_RICHIESTE = 5;
    private static final Duration PRENOTAZIONE_FINESTRA = Duration.ofMinutes(1);

    // Endpoint pubblici di sola consultazione (elenco centri, giorni non disponibili, slot). Limite
    // generoso: copre un umano che naviga il calendario, taglia uno script che martella (lo slot crea
    // righe on-demand a ogni chiamata).
    private static final int CONSULTAZIONE_MAX_RICHIESTE = 60;
    private static final Duration CONSULTAZIONE_FINESTRA = Duration.ofMinutes(1);

    private final Cache<String, Bucket> loginBuckets = costruisciCache(LOGIN_FINESTRA);
    private final Cache<String, Bucket> prenotazioneBuckets = costruisciCache(PRENOTAZIONE_FINESTRA);
    private final Cache<String, Bucket> consultazioneBuckets = costruisciCache(CONSULTAZIONE_FINESTRA);

    public boolean tryConsumeLogin(String chiave) {
        return loginBuckets.get(chiave, k -> creaBucket(LOGIN_MAX_RICHIESTE, LOGIN_FINESTRA)).tryConsume(1);
    }

    public boolean tryConsumePrenotazione(String chiave) {
        return prenotazioneBuckets.get(chiave, k -> creaBucket(PRENOTAZIONE_MAX_RICHIESTE, PRENOTAZIONE_FINESTRA)).tryConsume(1);
    }

    public boolean tryConsumeConsultazione(String chiave) {
        return consultazioneBuckets.get(chiave, k -> creaBucket(CONSULTAZIONE_MAX_RICHIESTE, CONSULTAZIONE_FINESTRA)).tryConsume(1);
    }

    private Cache<String, Bucket> costruisciCache(Duration finestra) {
        return Caffeine.newBuilder()
                .expireAfterAccess(finestra.multipliedBy(2))
                .maximumSize(50_000)
                .build();
    }

    private Bucket creaBucket(int capacita, Duration finestra) {
        Bandwidth limite = Bandwidth.classic(capacita, Refill.intervally(capacita, finestra));
        return Bucket.builder().addLimit(limite).build();
    }
}
