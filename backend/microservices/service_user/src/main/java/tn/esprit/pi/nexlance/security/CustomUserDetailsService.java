package tn.esprit.pi.nexlance.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tn.esprit.pi.nexlance.entities.User;
import tn.esprit.pi.nexlance.repositories.UserRepository;

import java.util.Collection;
import java.util.Collections;

/**
 * Service personnalisé pour charger les détails utilisateur
 * 
 * Ce service est utilisé par Spring Security pour :
 * - Charger un utilisateur par son email (username)
 * - Convertir l'entité User en UserDetails Spring Security
 * - Définir les rôles et permissions de l'utilisateur
 * 
 * @author NextLance Team
 * @version 1.0.0
 * @since 2026-02-15
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Charge un utilisateur par son email
     * 
     * Cette méthode est appelée automatiquement par Spring Security
     * lors de l'authentification.
     * 
     * @param email - Email de l'utilisateur (utilisé comme username)
     * @return UserDetails contenant les informations de l'utilisateur
     * @throws UsernameNotFoundException si l'utilisateur n'existe pas
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> 
                    new UsernameNotFoundException("Utilisateur non trouvé avec l'email : " + email)
                );

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                getAuthorities(user)
        );
    }

    /**
     * Récupère les autorités (rôles) de l'utilisateur
     * 
     * @param user - Utilisateur
     * @return Collection des autorités
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        // Préfixer le type d'utilisateur avec "ROLE_" pour Spring Security
        String role = "ROLE_" + user.getType().name();
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }
}
