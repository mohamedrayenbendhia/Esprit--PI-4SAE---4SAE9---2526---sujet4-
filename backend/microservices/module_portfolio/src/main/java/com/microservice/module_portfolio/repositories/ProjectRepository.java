package com.microservice.module_portfolio.repositories;

import com.microservice.module_portfolio.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByPortfolioId(Long portfolioId);
}
