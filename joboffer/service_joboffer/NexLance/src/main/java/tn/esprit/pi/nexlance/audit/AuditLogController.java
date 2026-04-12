package tn.esprit.pi.nexlance.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit-logs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Get recent audit logs
     */
    @GetMapping
    public ResponseEntity<List<AuditLog>> getRecentLogs() {
        return ResponseEntity.ok(auditLogService.getRecentLogs());
    }

    /**
     * Get audit trail for a specific entity
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLog>> getEntityAuditTrail(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        return ResponseEntity.ok(auditLogService.getEntityAuditTrail(entityType, entityId));
    }

    /**
     * Get audit trail for a specific user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getUserAuditTrail(@PathVariable String userId) {
        return ResponseEntity.ok(auditLogService.getUserAuditTrail(userId));
    }

    /**
     * Get logs by entity type
     */
    @GetMapping("/type/{entityType}")
    public ResponseEntity<List<AuditLog>> getLogsByEntityType(@PathVariable String entityType) {
        return ResponseEntity.ok(auditLogService.getLogsByEntityType(entityType));
    }

    /**
     * Get logs in date range
     */
    @GetMapping("/range")
    public ResponseEntity<List<AuditLog>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(auditLogService.getLogsByDateRange(start, end));
    }

    /**
     * Get audit statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAuditStats() {
        return ResponseEntity.ok(auditLogService.getAuditStats());
    }
}
