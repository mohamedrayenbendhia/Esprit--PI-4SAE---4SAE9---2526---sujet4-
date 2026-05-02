package com.microservice.module_portfolio.repositories;

import com.microservice.module_portfolio.entities.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    Optional<Portfolio> findByUserId(String userId);
    boolean existsByUserId(String userId);
    List<Portfolio> findByIsPublicTrue();
}
