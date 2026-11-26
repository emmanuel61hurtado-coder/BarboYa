package com.barboya.barboya.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class CurrentUser {
    private CurrentUser() {}

    public static UUID id() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getPrincipal() == null) return null;
        return UUID.fromString(a.getPrincipal().toString());
    }

    public static String role() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getAuthorities().isEmpty()) return null;
        return a.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "").toLowerCase();
    }
}