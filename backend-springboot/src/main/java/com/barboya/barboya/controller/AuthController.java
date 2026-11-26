package com.barboya.barboya.controller;

import com.barboya.barboya.dto.AuthDtos;
import com.barboya.barboya.model.User;
import com.barboya.barboya.security.CurrentUser;
import com.barboya.barboya.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/auth", "/api/auth"})
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public User register(@RequestBody AuthDtos.RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/login")
    public AuthDtos.TokenResponse login(@RequestBody AuthDtos.LoginRequest req) {
        return auth.login(req);
    }

    @GetMapping("/me")
    public User me() {
        return auth.me(CurrentUser.id());
    }
}