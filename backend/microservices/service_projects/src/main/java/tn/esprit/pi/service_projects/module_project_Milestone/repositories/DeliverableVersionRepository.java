package tn.esprit.pi.service_projects.module_project_Milestone.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.DeliverableVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliverableVersionRepository extends JpaRepository<DeliverableVersion, UUID> {

    List<DeliverableVersion> findByMilestoneIdOrderByVersionNumberDesc(UUID milestoneId);

    Optional<DeliverableVersion> findTopByMilestoneIdOrderByVersionNumberDesc(UUID milestoneId);

    @Query("SELECT COALESCE(MAX(dv.versionNumber), 0) FROM DeliverableVersion dv WHERE dv.milestoneId = :milestoneId")
    int getMaxVersionNumber(UUID milestoneId);

    long countByMilestoneId(UUID milestoneId);

    List<DeliverableVersion> findByMilestoneIdAndStatus(UUID milestoneId, DeliverableVersion.VersionStatus status);
}
