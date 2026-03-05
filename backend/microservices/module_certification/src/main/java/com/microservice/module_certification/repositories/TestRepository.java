package com.microservice.module_certification.repositories;

import com.microservice.module_certification.entities.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    Optional<Test> findBySkillId(Long skillId);
    boolean existsBySkillId(Long skillId);
}