package com.microservice.apigateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global gateway filter that extracts user info from the JWT Authorization header
 * and forwards it as X-User-Id, X-User-Email, X-User-Role headers
 * to downstream microservices (complaint-service, organization-service).
 */
@Component
public class JwtToHeadersFilter implements GlobalFilter, Ordered {

    private static final String SECRET = "NexLanceSecretKeyForJWTTokenGenerationAndValidation2026";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // --- PARTIE 1 : AJOUT DES HEADERS CORS SUR LA RÉPONSE ---
//        exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", "http://localhost:4200");
//        exchange.getResponse().getHeaders().add("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS");
//        exchange.getResponse().getHeaders().add("Access-Control-Allow-Headers", "*");
//        exchange.getResponse().getHeaders().add("Access-Control-Allow-Credentials", "true");

        // Les requêtes OPTIONS (Preflight CORS) sont gérées par le filtre CORS global de Spring Cloud Gateway.
        // Ne pas court-circuiter ici, sinon les headers Access-Control-* ne sont jamais ajoutés.
        if (exchange.getRequest().getMethod().name().equals("OPTIONS")) {
            return chain.filter(exchange);
        }

        List<String> authHeaders = exchange.getRequest().getHeaders().get(AUTH_HEADER);

        // Si pas de token, on laisse passer mais on va intercepter la réponse plus bas
        if (authHeaders == null || authHeaders.isEmpty() || !authHeaders.get(0).startsWith(BEARER_PREFIX)) {
            return chainWithRedirectCheck(exchange, chain);
        }

        String token = authHeaders.get(0).substring(BEARER_PREFIX.length());
        try {
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
            requestBuilder.header("X-User-Id", claims.get("userId", String.class));
            requestBuilder.header("X-User-Email", claims.getSubject());
            requestBuilder.header("X-User-Role", claims.get("userType", String.class));

            return chainWithRedirectCheck(exchange.mutate().request(requestBuilder.build()).build(), chain);

        } catch (Exception e) {
            // Token invalide : au lieu de laisser passer vers le 302 du microservice,
            // on pourrait même renvoyer 401 direct ici si on veut être strict.
            return chainWithRedirectCheck(exchange, chain);
        }
    }

    // --- PARTIE 2 : INTERCEPTEUR DE REDIRECTION ---
    // Cette méthode surveille la réponse du microservice. S'il renvoie 302 (Redirect), on le change en 401.
    private Mono<Void> chainWithRedirectCheck(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            var status = exchange.getResponse().getStatusCode();
            if (status != null && (status.is3xxRedirection())) {
                exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            }
        }));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}