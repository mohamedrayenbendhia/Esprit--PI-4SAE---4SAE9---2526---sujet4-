package tn.esprit.pi.nexlance.dto.auth;

import lombok.Data;

/**
 * DTO pour activer/désactiver la 2FA
 */
@Data
public class Toggle2FARequest {
    private boolean enabled;
}
