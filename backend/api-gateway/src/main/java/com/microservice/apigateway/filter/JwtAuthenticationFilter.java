package com.microservice.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements GlobalFilter {

    private final String SECRET_STR = "NexLanceSecretKeyForJWTTokenGenerationAndValidation2026";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // Nouvelle syntaxe pour JJWT 0.12.6
                SecretKey key = Keys.hmacShaKeyFor(SECRET_STR.getBytes(StandardCharsets.UTF_8));

                Claims claims = Jwts.parser()
                        .verifyWith(key) // Remplace setSigningKey
                        .build()
                        .parseSignedClaims(token) // Remplace parseClaimsJws
                        .getPayload(); // Remplace getBody

                // Injection des headers
                ServerHttpRequest request = exchange.getRequest().mutate()
                        .header("X-User-Id", claims.get("userId", String.class))
                        .header("X-User-Email", claims.getSubject())
                        .header("X-User-Role", claims.get("userType", String.class))
                        .build();

                return chain.filter(exchange.mutate().request(request).build());
            } catch (Exception e) {
                // Token invalide ou expiré : on laisse le microservice décider du rejet (403)
                System.err.println("JWT Filter Error: " + e.getMessage());
            }
        }
        return chain.filter(exchange);
    }
}