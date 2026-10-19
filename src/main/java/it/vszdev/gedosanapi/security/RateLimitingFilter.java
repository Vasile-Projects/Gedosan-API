package it.vszdev.gedosanapi.security;

import it.vszdev.gedosanapi.exception.Errore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final BotApiKeyFilter botApiKeyFilter;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimiterService rateLimiterService, BotApiKeyFilter botApiKeyFilter,
                              ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.botApiKeyFilter = botApiKeyFilter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Percorso percorso = classificaPercorso(request);
        if (percorso != Percorso.NESSUNO) {
            String chiave = estraiIp(request);
            boolean consentito = switch (percorso) {
                case LOGIN -> rateLimiterService.tryConsumeLogin(chiave);
                case PRENOTAZIONE -> rateLimiterService.tryConsumePrenotazione(chiave);
                case CONSULTAZIONE -> rateLimiterService.tryConsumeConsultazione(chiave);
                case CONFERMA -> rateLimiterService.tryConsumeConferma(chiave);
                case NESSUNO -> true;
            };

            if (!consentito) {
                scriviRispostaTroppeRichieste(request, response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void scriviRispostaTroppeRichieste(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Errore errore = new Errore(HttpStatus.TOO_MANY_REQUESTS.value(), "Troppe richieste",
                "Hai superato il numero massimo di richieste consentite. Riprova più tardi.", request.getRequestURI());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errore));
    }

    private Percorso classificaPercorso(HttpServletRequest request) {
        String metodo = request.getMethod();
        String uri = request.getRequestURI();

        if ("POST".equalsIgnoreCase(metodo)) {
            if ("/api/auth/login".equals(uri)) {
                return Percorso.LOGIN;
            }
            if ("/api/prenotazioni".equals(uri)) {
                return Percorso.PRENOTAZIONE;
            }
            if ("/api/prenotazioni/conferma".equals(uri)) {
                return Percorso.CONFERMA;
            }
            return Percorso.NESSUNO;
        }

        if ("GET".equalsIgnoreCase(metodo)
                && (uri.equals("/api/trasfusionali") || uri.startsWith("/api/trasfusionali/"))) {
            // n8n consulta per conto di tutti gli utenti del bot dallo stesso IP interno: con il limite
            // per IP un utente esaurirebbe la quota degli altri. I limiti del bot sono per chat.
            if (botApiKeyFilter.chiaveValida(request.getHeader(BotApiKeyFilter.HEADER))) {
                return Percorso.NESSUNO;
            }
            return Percorso.CONSULTAZIONE;
        }

        return Percorso.NESSUNO;
    }

    private String estraiIp(HttpServletRequest request) {
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp;
        }
        return request.getRemoteAddr();
    }

    private enum Percorso {
        LOGIN, PRENOTAZIONE, CONFERMA, CONSULTAZIONE, NESSUNO
    }
}
