package com.smartfreelance.microservice.organizationservice.config;

import com.smartfreelance.microservice.organizationservice.security.HeaderAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        // Retourner 401 quand l'utilisateur n'est pas authentifié
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        // Retourner 403 quand l'accès est refusé (évite le 500 par défaut de Spring Security 6)
                        .accessDeniedHandler((req, res, e) ->
                                res.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN, "Access Denied"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/organizations/public/**").permitAll()
                        .requestMatchers("/api/organizations/search").permitAll()
                        // Profil public — accessible sans authentification (partage de lien)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/organizations/*").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/organizations/*/members").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/organizations/*/reviews").permitAll()
                        // Offres de collaboration ouvertes — visibles publiquement
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/organizations/*/collab-offers").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/organizations/*/collab-offers/*").permitAll()
                        // TrustScore public — visible sur le profil sans authentification
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/organizations/*/trust-score").permitAll()
                        // Completion score — accessible aux utilisateurs authentifiés uniquement
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/organizations/*/completion-score").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new HeaderAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }
}
