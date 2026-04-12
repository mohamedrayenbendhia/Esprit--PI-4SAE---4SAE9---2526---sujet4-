package tn.esprit.pi.nexlance.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "freelance_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FreelanceProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @OneToOne
    @JoinColumn(name = "userId", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Enumerated(EnumType.STRING)
    private Availability availability = Availability.AVAILABLE;

    private Integer experienceYears;

    private String portfolioUrl;

    private String linkedInUrl;

    private String githubUrl;

    private String location;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "JSON")
    private List<String> languages;

    private String timezone;

    @Column(precision = 5, scale = 2)
    private BigDecimal completionRate = BigDecimal.ZERO;

    private Integer responseTime;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "JSON")
    private List<String> skills;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "JSON")
    private List<String> certifications;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Availability {
        AVAILABLE,
        BUSY,
        UNAVAILABLE
    }
}
