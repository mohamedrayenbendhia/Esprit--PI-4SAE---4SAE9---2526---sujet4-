package tn.esprit.pi.nexlance.reputation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reputation")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReputationController {

    private final ReputationService reputationService;

    /**
     * Get reputation score for a freelancer
     */
    @GetMapping("/{freelancerId}")
    public ResponseEntity<ReputationScore> getReputation(@PathVariable String freelancerId) {
        return ResponseEntity.ok(reputationService.calculateReputation(freelancerId));
    }
}
