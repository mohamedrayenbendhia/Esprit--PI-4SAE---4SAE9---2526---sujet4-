package tn.esprit.pi.service_projects.chat;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private UUID senderId;

    @Column(nullable = false)
    private String senderName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    private String attachmentUrl;

    private LocalDateTime createdAt;

    @Builder.Default
    private Boolean edited = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (edited == null) edited = false;
    }

    public enum MessageType {
        TEXT, FILE, SYSTEM, MILESTONE_UPDATE
    }
}
