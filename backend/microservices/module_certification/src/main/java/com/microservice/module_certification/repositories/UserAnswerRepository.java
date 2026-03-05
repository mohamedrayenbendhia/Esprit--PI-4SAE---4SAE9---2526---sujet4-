package com.microservice.module_certification.repositories;

import com.microservice.module_certification.entities.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
    List<UserAnswer> findByUserTestResultId(Long userTestResultId);
    @Modifying
    @Query("DELETE FROM UserAnswer ua WHERE ua.question.id = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}