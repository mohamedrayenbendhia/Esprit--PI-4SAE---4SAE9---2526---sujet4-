package tn.esprit.pi.nexlance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        
        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(requestURI)) {
            log.debug("Public endpoint, skipping JWT filter: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        
        log.info("=== JWT FILTER - Processing request: {} ===", requestURI);
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null) {
                log.info("Token found, length: {}, preview: {}...", token.length(), token.substring(0, Math.min(20, token.length())));
                
                // Create signing key from secret
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                log.info("Signing key created, secret length: {}", jwtSecret.length());
                
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
                
                log.info("Token parsed successfully. Subject: {}, userId: {}, userType: {}", 
                    claims.getSubject(), claims.get("userId"), claims.get("userType"));
                
                // Create a Spring Security Jwt object with all claims
                Jwt jwt = Jwt.withTokenValue(token)
                        .header("alg", "HS384")
                        .subject(claims.getSubject())
                        .claim("userId", claims.get("userId"))
                        .claim("userType", claims.get("userType"))
                        .issuedAt(claims.getIssuedAt() != null ? claims.getIssuedAt().toInstant() : Instant.now())
                        .expiresAt(claims.getExpiration() != null ? claims.getExpiration().toInstant() : Instant.now().plusSeconds(86400))
                        .build();
                
                // Extract authorities from userType claim
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                String userType = claims.get("userType", String.class);
                if (userType != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + userType.toUpperCase()));
                    log.info("Granted authority: ROLE_{}", userType.toUpperCase());
                }
                
                // Create authentication token with authorities
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.info("✓ JWT authenticated successfully for user: {}", claims.getSubject());
            } else {
                log.warn("No token found in request to {}", requestURI);
            }
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("❌ JWT Signature validation failed: {}", e.getMessage());
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.error("❌ JWT is malformed: {}", e.getMessage());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("❌ JWT is expired: {}", e.getMessage());
        } catch (Exception e) {
            log.error("❌ Cannot set user authentication", e);
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String uri) {
        // 2FA endpoints require authentication - do NOT skip them
        if (uri.startsWith("/api/auth/2fa/")) {
            return false;
        }
        return uri.startsWith("/api/auth/") || 
               uri.startsWith("/swagger-ui/") || 
               uri.startsWith("/v3/api-docs/") ||
               uri.startsWith("/uploads/");
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
