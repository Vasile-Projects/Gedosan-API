package it.vszdev.gedosanapi.security;

import it.vszdev.gedosanapi.config.BotProperties;
import it.vszdev.gedosanapi.exception.Errore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

// Autentica n8n sugli endpoint /api/bot/**. Agisce solo su quei percorsi: la chiave del bot non apre
// l'area admin, e il JWT admin non apre il bot (SecurityConfig richiede il ruolo BOT).
@Component
public class BotApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Bot-Key";
    public static final String PREFISSO = "/api/bot/";

    private final BotProperties botProperties;
    private final ObjectMapper objectMapper;

    public BotApiKeyFilter(BotProperties botProperties, ObjectMapper objectMapper) {
        this.botProperties = botProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFISSO);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!chiaveValida(request.getHeader(HEADER))) {
            Errore errore = new Errore(HttpStatus.UNAUTHORIZED.value(), "Non autorizzato",
                    "Chiave del bot mancante o non valida", request.getRequestURI());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(errore));
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken("bot", null,
                List.of(new SimpleGrantedAuthority("ROLE_BOT")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    // Confronto a tempo costante. Senza chiave configurata nessuna richiesta passa.
    public boolean chiaveValida(String ricevuta) {
        String attesa = botProperties.getApiKey();
        if (attesa == null || attesa.isBlank() || ricevuta == null) {
            return false;
        }
        return MessageDigest.isEqual(attesa.getBytes(StandardCharsets.UTF_8), ricevuta.getBytes(StandardCharsets.UTF_8));
    }
}
