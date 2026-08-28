package it.vszdev.gedosanapi.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import it.vszdev.gedosanapi.config.JwtProperties;
import it.vszdev.gedosanapi.dto.auth.TokenResponse;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey chiave;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.chiave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    // Subject = id admin, non username: lo schema DB è gestito a mano e l'unicità di username non è garantita.
    public TokenResponse generaToken(Long idAdmin) {
        Instant ora = Instant.now();
        Instant scadenza = ora.plusMillis(jwtProperties.getExpirationMs());

        String token = Jwts.builder()
                .subject(String.valueOf(idAdmin))
                .issuedAt(Date.from(ora))
                .expiration(Date.from(scadenza))
                .signWith(chiave)
                .compact();

        return new TokenResponse(token, scadenza);
    }

    public Long estraiIdAdmin(String token) {
        String subject = Jwts.parser()
                .verifyWith(chiave)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        return Long.valueOf(subject);
    }
}
