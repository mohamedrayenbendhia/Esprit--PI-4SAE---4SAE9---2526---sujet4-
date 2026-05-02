package com.smartfreelance.microservice.complaintservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lit les headers injectés par l'API Gateway et construit le SecurityContext.
 * Headers : X-User-Id, X-User-Email, X-User-Role
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userId    = request.getHeader("X-User-Id");
        String userRole  = request.getHeader("X-User-Role");

        // Log for debugging - check your IntelliJ/Eclipse console!
        System.out.println("Filter checking path: " + request.getRequestURI() + " with Role: " + userRole);

        if (userId != null && !userId.isBlank() && userRole != null && !userRole.isBlank()) {
            // Ensure the role is uppercase and prefixed with ROLE_
            String formattedRole = "ROLE_" + userRole.trim().toUpperCase();

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority(formattedRole)
            );

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
