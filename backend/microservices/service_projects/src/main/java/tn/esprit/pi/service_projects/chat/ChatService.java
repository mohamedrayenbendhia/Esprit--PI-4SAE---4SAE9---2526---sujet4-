package tn.esprit.pi.service_projects.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.Project;
import tn.esprit.pi.service_projects.module_project_Milestone.repositories.ProjectRepository;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProjectRepository projectRepository;
    private final NotificationClient notificationClient;

    public ChatMessage sendMessage(ChatMessage message) {
        ChatMessage saved = chatMessageRepository.save(message);
        // Broadcast to project chat topic
        messagingTemplate.convertAndSend(
            "/topic/chat/" + message.getProjectId(), saved
        );

        // Send notification to the other participant
        notifyChatRecipient(message);

        return saved;
    }

    /**
     * Notify the other project participant about a new chat message.
     * Runs asynchronously (fire-and-forget) to avoid slowing down the chat.
     */
    private void notifyChatRecipient(ChatMessage message) {
        try {
            // Skip system messages
            if (message.getType() == ChatMessage.MessageType.SYSTEM) return;

            UUID projectId = message.getProjectId();
            UUID senderId = message.getSenderId();

            projectRepository.findById(projectId).ifPresent(project -> {
                // Determine the recipient: the other participant in the project
                UUID recipientId = null;
                if (project.getClientId() != null && !project.getClientId().equals(senderId)) {
                    recipientId = project.getClientId();
                } else if (project.getFreelanceId() != null && !project.getFreelanceId().equals(senderId)) {
                    recipientId = project.getFreelanceId();
                }

                if (recipientId != null) {
                    String title = "New message from " + message.getSenderName();
                    String content = message.getContent();
                    if (content.length() > 100) {
                        content = content.substring(0, 97) + "...";
                    }

                    notificationClient.sendNotification(
                            recipientId.toString(),
                            "PROJECT",
                            title,
                            content,
                            projectId.toString(),
                            "PROJECT"
                    );
                }
            });
        } catch (Exception e) {
            log.warn("Failed to send chat notification: {}", e.getMessage());
        }
    }

    public ChatMessage sendSystemMessage(UUID projectId, String content) {
        ChatMessage msg = ChatMessage.builder()
                .projectId(projectId)
                .senderId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .senderName("System")
                .content(content)
                .type(ChatMessage.MessageType.SYSTEM)
                .build();
        return sendMessage(msg);
    }

    public List<ChatMessage> getProjectMessages(UUID projectId) {
        return chatMessageRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
    }

    public List<ChatMessage> getRecentMessages(UUID projectId) {
        List<ChatMessage> messages = chatMessageRepository
                .findTop50ByProjectIdOrderByCreatedAtDesc(projectId);
        Collections.reverse(messages);
        return messages;
    }

    public long getMessageCount(UUID projectId) {
        return chatMessageRepository.countByProjectId(projectId);
    }
}
