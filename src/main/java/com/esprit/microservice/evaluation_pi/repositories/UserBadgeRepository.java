package com.esprit.microservice.evaluation_pi.repositories;

import com.esprit.microservice.evaluation_pi.entities.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUserId(String userId);
}