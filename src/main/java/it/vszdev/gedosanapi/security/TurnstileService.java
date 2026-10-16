package it.vszdev.gedosanapi.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.vszdev.gedosanapi.exception.RichiestaNonValidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

// Verifica lato server del widget Cloudflare Turnstile ("non sono un bot") sulla prenotazione pubblica.
// Il token prodotto dal widget va sempre validato qui: il controllo nel solo frontend si aggira.
// Fail-closed: se Cloudflare non risponde la richiesta viene rifiutata, non lasciata passare.
@Service
public class TurnstileService {

    private static final Logger log = LoggerFactory.getLogger(TurnstileService.class);
    private static final String URL_VERIFICA = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final String MESSAGGIO_ERRORE = "Verifica anti-bot non superata. Ricarica la pagina e riprova.";

    private final boolean abilitato;
    private final String secret;
    private final RestClient restClient;

    public TurnstileService(@Value("${turnstile.enabled}") boolean abilitato,
                            @Value("${turnstile.secret:}") String secret) {
        if (abilitato && (secret == null || secret.isBlank())) {
            throw new IllegalStateException("turnstile.enabled=true richiede turnstile.secret");
        }
        this.abilitato = abilitato;
        this.secret = secret;

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public void verifica(String token) {
        if (!abilitato) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new RichiestaNonValidaException(MESSAGGIO_ERRORE);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", token);

        RispostaTurnstile risposta;
        try {
            risposta = restClient.post()
                    .uri(URL_VERIFICA)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(RispostaTurnstile.class);
        } catch (RestClientException e) {
            log.warn("Verifica Turnstile non riuscita: {}", e.getMessage());
            throw new RichiestaNonValidaException(MESSAGGIO_ERRORE);
        }

        if (risposta == null || !risposta.success()) {
            log.info("Token Turnstile rifiutato: {}", risposta == null ? "risposta vuota" : risposta.errorCodes());
            throw new RichiestaNonValidaException(MESSAGGIO_ERRORE);
        }
    }

    private record RispostaTurnstile(boolean success, @JsonProperty("error-codes") List<String> errorCodes) {
    }
}
