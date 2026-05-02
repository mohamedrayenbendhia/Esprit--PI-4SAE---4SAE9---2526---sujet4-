package com.esprit.microservice.contrat_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Activation de la configuration CORS définie ci-dessous
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 2. Désactivation du CSRF (nécessaire pour les requêtes POST/PUT/DELETE)
                .csrf(csrf -> csrf.disable())
                // 3. Autorisation de toutes les requêtes (développement)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // On définit précisément l'origine (pas de "*" ici pour éviter les conflits)
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));

        // Méthodes autorisées
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Headers autorisés
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));

        // Headers exposés (important pour le téléchargement de PDF)
        configuration.setExposedHeaders(Arrays.asList("Content-Disposition"));

        // Autoriser l'envoi de cookies ou headers d'auth
        configuration.setAllowCredentials(true);

        // Durée de mise en cache de la réponse CORS (1 heure)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}