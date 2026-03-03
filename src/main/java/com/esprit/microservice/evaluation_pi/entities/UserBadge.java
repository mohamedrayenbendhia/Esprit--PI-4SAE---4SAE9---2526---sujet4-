package com.esprit.microservice.evaluation_pi.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserBadge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId; // Changé de Long à String (pour UUID)

    @ManyToOne

    private Badge badge;

    private LocalDateTime assignedAt;
}