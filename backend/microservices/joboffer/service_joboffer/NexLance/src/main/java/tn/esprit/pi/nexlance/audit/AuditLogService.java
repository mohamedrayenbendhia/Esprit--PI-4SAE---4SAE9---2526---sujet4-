package tn.esprit.pi.nexlance.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Log an action and broadcast via WebSocket for real-time updates
     */
    public AuditLog logAction(String action, String entityType, String entityId,
                               String userId, String userRole, String details,
                               String oldValue, String newValue) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .userId(userId)
                .userRole(userRole)
                .details(details)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        auditLog = auditLogRepository.save(auditLog);
        log.info("Audit: {} {} {} by user {} ({})", action, entityType, entityId, userId, userRole);

        // Broadcast to all connected audit-log subscribers in real-time
        try {
            messagingTemplate.convertAndSend("/topic/audit-logs", auditLog);
            log.debug("Broadcast audit log to /topic/audit-logs");
        } catch (Exception e) {
            log.warn("Failed to broadcast audit log via WebSocket: {}", e.getMessage());
        }

        return auditLog;
    }

    /**
     * Simplified log method
     */
    public AuditLog log(String action, String entityType, String entityId, String userId, String details) {
        return logAction(action, entityType, entityId, userId, null, details, null, null);
    }

    /**
     * Get audit trail for a specific entity
     */
    public List<AuditLog> getEntityAuditTrail(String entityType, String entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }

    /**
     * Get audit trail for a specific user
     */
    public List<AuditLog> getUserAuditTrail(String userId) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    /**
     * Get recent audit logs
     */
    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    /**
     * Get logs by entity type
     */
    public List<AuditLog> getLogsByEntityType(String entityType) {
        return auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType);
    }

    /**
     * Get logs in date range
     */
    public List<AuditLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
    }

    /**
     * Get audit statistics
     */
    public Map<String, Object> getAuditStats() {
        long totalLogs = auditLogRepository.count();
        long todayLogs = auditLogRepository.countByTimestampAfter(
                LocalDateTime.now().toLocalDate().atStartOfDay());
        long jobOfferActions = auditLogRepository.countByEntityType("JOB_OFFER");
        long applicationActions = auditLogRepository.countByEntityType("APPLICATION");
        long recommendationActions = auditLogRepository.countByEntityType("RECOMMENDATION");
        long invitationActions = auditLogRepository.countByEntityType("INVITATION");
        long createActions = auditLogRepository.countByAction("CREATE");
        long updateActions = auditLogRepository.countByAction("UPDATE");
        long deleteActions = auditLogRepository.countByAction("DELETE");
        long statusChangeActions = auditLogRepository.countByAction("STATUS_CHANGE");

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalLogs", totalLogs);
        result.put("todayLogs", todayLogs);
        result.put("topEntityTypes", Map.of(
                "JOB_OFFER", jobOfferActions,
                "APPLICATION", applicationActions,
                "RECOMMENDATION", recommendationActions,
                "INVITATION", invitationActions
        ));
        result.put("topActions", Map.of(
                "CREATE", createActions,
                "UPDATE", updateActions,
                "DELETE", deleteActions,
                "STATUS_CHANGE", statusChangeActions
        ));
        return result;
    }
}
