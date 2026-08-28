package it.vszdev.gedosanapi.security;

import io.jsonwebtoken.JwtException;
import it.vszdev.gedosanapi.models.Admin;
import it.vszdev.gedosanapi.repositories.AdminRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String PREFISSO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final AdminRepository adminRepository;

    public JwtAuthFilter(JwtService jwtService, AdminRepository adminRepository) {
        this.jwtService = jwtService;
        this.adminRepository = adminRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(PREFISSO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFISSO_BEARER.length());
        try {
            Long idAdmin = jwtService.estraiIdAdmin(token);
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Admin admin = adminRepository.findById(idAdmin).orElse(null);
                if (admin != null) {
                    AdminPrincipal principal = new AdminPrincipal(admin.getId(), admin.getUsername());
                    var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT non valido: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
