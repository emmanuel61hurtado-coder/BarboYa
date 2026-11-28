package com.barboya.barboya.service;

import com.barboya.barboya.dto.AuthDtos;
import com.barboya.barboya.model.User;
import com.barboya.barboya.repository.UserRepository;
import com.barboya.barboya.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtUtil jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public User register(AuthDtos.RegisterRequest req) {
        User u = new User();
        u.setPhone(req.phone());
        u.setEmail(req.email());
        u.setFullName(req.fullName());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(req.role() != null ? req.role() : com.barboya.barboya.enums.Enums.UserRole.customer);
        return users.save(u);
    }

    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest req) {
        User u = null;
        if (req.phone() != null) u = users.findByPhone(req.phone()).orElse(null);
        if (u == null && req.email() != null) u = users.findByEmail(req.email()).orElse(null);
        if (u == null || !encoder.matches(req.password(), u.getPasswordHash())) {
            throw new RuntimeException("Credenciales inválidas");
        }
        String token = jwt.createToken(u.getId(), u.getRole().name());
        return new AuthDtos.TokenResponse(token, "bearer", u.getRole().name());
    }

    public User me(UUID id) {
        return users.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}