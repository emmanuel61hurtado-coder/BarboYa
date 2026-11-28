package com.barboya.barboya.dto;

import com.barboya.barboya.enums.Enums;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(String phone, String email, String fullName, String password, Enums.UserRole role) {}
    public record LoginRequest(String phone, String email, String password) {}
    public record TokenResponse(String accessToken, String tokenType, String role) {}
    public record MeResponse(java.util.UUID id, String phone, String email, String fullName, Enums.UserRole role) {}
}