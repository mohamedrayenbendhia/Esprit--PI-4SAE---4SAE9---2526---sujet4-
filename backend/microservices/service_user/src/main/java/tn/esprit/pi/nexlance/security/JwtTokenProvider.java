package tn.esprit.pi.nexlance.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Provider JWT pour la génération et validation des tokens
 * 
 * Ce composant gère :
 * - La génération de tokens JWT lors de la connexion
 * - La validation des tokens reçus dans les requêtes
 * - L'extraction des informations utilisateur depuis le token
 * - La vérification de l'expiration des tokens
 * 
 * @author NextLance Team
 * @version 1.0.0
 * @since 2026-02-15
 */
@Component
public class JwtTokenProvider {

    /**
     * Clé secrète pour signer les tokens JWT
     * IMPORTANT: En production, utiliser une clé forte et la stocker de manière sécurisée
     */
    @Value("${jwt.secret:nexlance-secret-key-change-this-in-production-with-strong-random-value}")
    private String jwtSecret;

    /**
     * Durée de validité du token en millisecondes (24 heures par défaut)
     */
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    /**
     * Génère un token JWT pour un utilisateur authentifié
     * 
     * @param authentication - Objet Authentication de Spring Security
     * @return Token JWT signé
     * 
     * @example
     * String token = jwtTokenProvider.generateToken(authentication);
     * // Retourne: "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIi..."
     */
    public String generateToken(Authentication authentication) {
        String email = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Extrait l'email de l'utilisateur depuis le token JWT
     * 
     * @param token - Token JWT
     * @return Email de l'utilisateur
     */
    public String getEmailFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Valide un token JWT
     * 
     * @param token - Token JWT à valider
     * @return true si le token est valide, false sinon
     * 
     * Vérifie :
     * - La signature du token
     * - La date d'expiration
     * - Le format du token
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token invalide
            return false;
        }
    }
}
