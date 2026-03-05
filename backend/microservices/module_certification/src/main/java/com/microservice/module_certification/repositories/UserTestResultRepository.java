package com.microservice.module_certification.repositories;

import com.microservice.module_certification.entities.UserTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserTestResultRepository extends JpaRepository<UserTestResult, Long> {
    List<UserTestResult> findByUserId(String userId); // ✅
    boolean existsByUserIdAndTestIdAndIsPassed(String userId, Long testId, boolean isPassed);
}