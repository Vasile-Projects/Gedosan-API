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
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimiterService rateLimiterService, ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
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
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return Percorso.NESSUNO;
        }
        String uri = request.getRequestURI();
        if ("/api/auth/login".equals(uri)) {
            return Percorso.LOGIN;
        }
        if ("/api/prenotazioni".equals(uri)) {
            return Percorso.PRENOTAZIONE;
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
        LOGIN, PRENOTAZIONE, NESSUNO
    }
}
