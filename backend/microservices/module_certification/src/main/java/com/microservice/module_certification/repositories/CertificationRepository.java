package com.microservice.module_certification.repositories;

import com.microservice.module_certification.entities.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findByUserId(String userId); // ✅
}