package tn.esprit.pi.service_projects.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * REST: Get all messages for a project
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ChatMessage>> getProjectMessages(@PathVariable UUID projectId) {
        return ResponseEntity.ok(chatService.getProjectMessages(projectId));
    }

    /**
     * REST: Get recent messages (last 50)
     */
    @GetMapping("/project/{projectId}/recent")
    public ResponseEntity<List<ChatMessage>> getRecentMessages(@PathVariable UUID projectId) {
        return ResponseEntity.ok(chatService.getRecentMessages(projectId));
    }

    /**
     * REST: Get message count
     */
    @GetMapping("/project/{projectId}/count")
    public ResponseEntity<Long> getMessageCount(@PathVariable UUID projectId) {
        return ResponseEntity.ok(chatService.getMessageCount(projectId));
    }

    /**
     * REST: Post a message (fallback if WebSocket not available)
     */
    @PostMapping("/project/{projectId}")
    public ResponseEntity<ChatMessage> postMessage(
            @PathVariable UUID projectId,
            @RequestBody ChatMessage message) {
        message.setProjectId(projectId);
        return ResponseEntity.ok(chatService.sendMessage(message));
    }

    /**
     * WebSocket: Handle incoming chat messages via STOMP
     * Client sends to: /app/chat/{projectId}
     * Subscribers receive on: /topic/chat/{projectId}
     */
    @MessageMapping("/chat/{projectId}")
    public void handleChatMessage(
            @DestinationVariable UUID projectId,
            @Payload ChatMessage message) {
        message.setProjectId(projectId);
        chatService.sendMessage(message);
    }
}
