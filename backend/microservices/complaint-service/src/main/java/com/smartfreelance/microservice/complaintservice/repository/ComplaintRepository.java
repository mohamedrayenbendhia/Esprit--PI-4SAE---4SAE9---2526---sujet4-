package com.smartfreelance.microservice.complaintservice.repository;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.Complaint.Priority;
import com.smartfreelance.microservice.complaintservice.entity.Complaint.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, String> {

    Optional<Complaint> findByTicketNumber(String ticketNumber);

    boolean existsByTicketNumber(String ticketNumber);

    List<Complaint> findByReporterId(String reporterId);

    List<Complaint> findByReportedUserId(String reportedUserId);

    List<Complaint> findByProjectId(String projectId);

    List<Complaint> findByStatus(Status status);

    List<Complaint> findByPriority(Priority priority);

    List<Complaint> findByAssignedToId(String assignedToId);

    List<Complaint> findByStatusAndPriority(Status status, Priority priority);

    @Query("SELECT c FROM Complaint c WHERE c.assignedToId IS NULL AND c.status = :status")
    List<Complaint> findUnassignedByStatus(@Param("status") Status status);

    @Query("SELECT c FROM Complaint c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    List<Complaint> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    long countByStatus(Status status);

    long countByPriority(Priority priority);

    @Query("SELECT c FROM Complaint c WHERE c.status NOT IN ('RESOLVED', 'CLOSED') " +
            "AND c.createdAt < :thresholdDate")
    List<Complaint> findOverdueComplaints(@Param("thresholdDate") LocalDateTime thresholdDate);
}
