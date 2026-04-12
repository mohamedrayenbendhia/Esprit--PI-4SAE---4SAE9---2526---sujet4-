package tn.esprit.pi.service_projects.module_project_Milestone.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.DeliverableVersion;
import tn.esprit.pi.service_projects.module_project_Milestone.repositories.DeliverableVersionRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliverableVersionService {

    private final DeliverableVersionRepository versionRepository;
    private static final String UPLOAD_DIR = "uploads/deliverables";

    public DeliverableVersion uploadNewVersion(
            UUID milestoneId,
            UUID uploadedBy,
            String description,
            String changeNotes,
            MultipartFile file) throws IOException {

        // Auto-increment version number
        int nextVersion = versionRepository.getMaxVersionNumber(milestoneId) + 1;

        // Mark previous versions as SUPERSEDED
        List<DeliverableVersion> previous = versionRepository
                .findByMilestoneIdAndStatus(milestoneId, DeliverableVersion.VersionStatus.PENDING);
        for (DeliverableVersion v : previous) {
            v.setStatus(DeliverableVersion.VersionStatus.SUPERSEDED);
        }
        versionRepository.saveAll(previous);

        // Store file
        Path uploadPath = Paths.get(UPLOAD_DIR, milestoneId.toString());
        Files.createDirectories(uploadPath);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "deliverable";
        String storedFilename = "v" + nextVersion + "_" + originalFilename;
        Path filePath = uploadPath.resolve(storedFilename);
        file.transferTo(filePath.toFile());

        // Create version entry
        DeliverableVersion version = DeliverableVersion.builder()
                .milestoneId(milestoneId)
                .versionNumber(nextVersion)
                .fileName(originalFilename)
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .description(description)
                .changeNotes(changeNotes)
                .uploadedBy(uploadedBy)
                .status(DeliverableVersion.VersionStatus.PENDING)
                .build();

        return versionRepository.save(version);
    }

    public List<DeliverableVersion> getVersionHistory(UUID milestoneId) {
        return versionRepository.findByMilestoneIdOrderByVersionNumberDesc(milestoneId);
    }

    public DeliverableVersion getLatestVersion(UUID milestoneId) {
        return versionRepository.findTopByMilestoneIdOrderByVersionNumberDesc(milestoneId)
                .orElse(null);
    }

    public DeliverableVersion reviewVersion(UUID versionId, UUID reviewedBy,
                                             DeliverableVersion.VersionStatus status,
                                             String reviewComment) {
        DeliverableVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        version.setStatus(status);
        version.setReviewComment(reviewComment);
        version.setReviewedBy(reviewedBy);
        version.setReviewedAt(java.time.LocalDateTime.now());

        return versionRepository.save(version);
    }

    public long getVersionCount(UUID milestoneId) {
        return versionRepository.countByMilestoneId(milestoneId);
    }
}
