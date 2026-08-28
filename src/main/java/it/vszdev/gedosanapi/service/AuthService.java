package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.auth.LoginRequest;
import it.vszdev.gedosanapi.dto.auth.TokenResponse;
import it.vszdev.gedosanapi.exception.CredenzialiNonValideException;
import it.vszdev.gedosanapi.models.Admin;
import it.vszdev.gedosanapi.repositories.AdminRepository;
import it.vszdev.gedosanapi.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AdminRepository adminRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.username())
                .orElseThrow(CredenzialiNonValideException::new);

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new CredenzialiNonValideException();
        }

        return jwtService.generaToken(admin.getId());
    }
}
