package com.microservice.module_competence.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;

public class JwtUtil {

    public static Long getUserId(Authentication authentication) {
        if (authentication != null) {
            Object details = authentication.getDetails();
            if (details instanceof Claims claims) {
                String userId = claims.get("userId", String.class);
                if (userId != null) return Long.parseLong(userId);
            }
        }
        throw new RuntimeException("Unable to extract userId from token");
    }

    public static String getUserIdAsString(Authentication authentication) {
        if (authentication != null) {
            Object details = authentication.getDetails();
            if (details instanceof Claims claims) {
                return claims.get("userId", String.class);
            }
        }
        throw new RuntimeException("Unable to extract userId from token");
    }
}