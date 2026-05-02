package com.smartfreelance.microservice.complaintservice.repository;

import com.smartfreelance.microservice.complaintservice.entity.NpsSurvey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NpsSurveyRepository extends JpaRepository<NpsSurvey, String> {

    Optional<NpsSurvey> findByComplaintId(String complaintId);
    boolean existsByComplaintId(String complaintId);

    List<NpsSurvey> findByRespondedAtIsNotNull();

    @Query("SELECT AVG(n.score) FROM NpsSurvey n WHERE n.score IS NOT NULL")
    Double computeAverageScore();

    @Query("SELECT COUNT(n) FROM NpsSurvey n WHERE n.score IS NOT NULL AND n.score <= 6")
    long countDetractors();

    @Query("SELECT COUNT(n) FROM NpsSurvey n WHERE n.score IS NOT NULL AND n.score >= 9")
    long countPromoters();

    @Query("SELECT COUNT(n) FROM NpsSurvey n WHERE n.score IS NOT NULL")
    long countResponded();
}
