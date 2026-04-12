package tn.esprit.pi.nexlance.module_job_offers.entities;

import jakarta.persistence.*;
import tn.esprit.pi.nexlance.module_job_offers.converters.StringListConverter;
import tn.esprit.pi.nexlance.module_job_offers.enums.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID jobOfferId;

    @Column(nullable = false)
    private UUID freelanceId;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    @Column(precision = 10, scale = 2)
    private BigDecimal proposedRate;

    private LocalDateTime estimatedDelivery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> portfolioItems;

    private LocalDateTime availableFrom;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime respondedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Application() {
    }

    public Application(UUID id, UUID jobOfferId, UUID freelanceId, String coverLetter, BigDecimal proposedRate, 
                      LocalDateTime estimatedDelivery, ApplicationStatus status, List<String> portfolioItems, 
                      LocalDateTime availableFrom, Boolean isRead, LocalDateTime submittedAt, 
                      LocalDateTime respondedAt, LocalDateTime createdAt) {
        this.id = id;
        this.jobOfferId = jobOfferId;
        this.freelanceId = freelanceId;
        this.coverLetter = coverLetter;
        this.proposedRate = proposedRate;
        this.estimatedDelivery = estimatedDelivery;
        this.status = status;
        this.portfolioItems = portfolioItems;
        this.availableFrom = availableFrom;
        this.isRead = isRead;
        this.submittedAt = submittedAt;
        this.respondedAt = respondedAt;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getJobOfferId() {
        return jobOfferId;
    }

    public void setJobOfferId(UUID jobOfferId) {
        this.jobOfferId = jobOfferId;
    }

    public UUID getFreelanceId() {
        return freelanceId;
    }

    public void setFreelanceId(UUID freelanceId) {
        this.freelanceId = freelanceId;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public BigDecimal getProposedRate() {
        return proposedRate;
    }

    public void setProposedRate(BigDecimal proposedRate) {
        this.proposedRate = proposedRate;
    }

    public LocalDateTime getEstimatedDelivery() {
        return estimatedDelivery;
    }

    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public List<String> getPortfolioItems() {
        return portfolioItems;
    }

    public void setPortfolioItems(List<String> portfolioItems) {
        this.portfolioItems = portfolioItems;
    }

    public LocalDateTime getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDateTime availableFrom) {
        this.availableFrom = availableFrom;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        submittedAt = LocalDateTime.now();
    }
}
