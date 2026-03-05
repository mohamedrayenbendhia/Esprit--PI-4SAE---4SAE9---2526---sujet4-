package com.microservice.module_portfolio.services;

import com.microservice.module_portfolio.dto.*;
import com.microservice.module_portfolio.entities.*;
import com.microservice.module_portfolio.exceptions.*;
import com.microservice.module_portfolio.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final ProjectRepository projectRepository;

    // ── Créer portfolio
    @Transactional
    public PortfolioResponse getByUserId(String userId) { // ✅
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portfolio not found for userId: " + userId));
        return toResponse(portfolio);
    }

    @Transactional
    public PortfolioResponse createPortfolio(PortfolioRequest request) {
        if (portfolioRepository.existsByUserId(request.getUserId())) { // userId est String ✅
            throw new DuplicateResourceException(
                    "Portfolio already exists for userId: " + request.getUserId());
        }
        Portfolio portfolio = Portfolio.builder()
                .userId(request.getUserId()) // String ✅
                .headline(request.getHeadline())
                .linkedinUrl(request.getLinkedinUrl())
                .githubUrl(request.getGithubUrl())
                .location(request.getLocation())
                .isPublic(request.isPublic())
                .build();
        Portfolio saved = portfolioRepository.save(portfolio);
        if (request.getProjects() != null && !request.getProjects().isEmpty()) {
            List<Project> projects = request.getProjects().stream()
                    .map(p -> toProjectEntity(p, saved))
                    .toList();
            projectRepository.saveAll(projects);
            saved.setProjects(projects);
        }
        return toResponse(saved);
    }

    // ── Récupérer par id
    public PortfolioResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public PortfolioResponse updatePortfolio(Long id, PortfolioUpdateRequest request) {
        Portfolio portfolio = findById(id);
        portfolio.setHeadline(request.getHeadline());
        portfolio.setLinkedinUrl(request.getLinkedinUrl());
        portfolio.setGithubUrl(request.getGithubUrl());
        portfolio.setLocation(request.getLocation());
        portfolio.setPublic(request.isPublic());

        if (request.getProjects() != null && !request.getProjects().isEmpty()) {
            portfolio.getProjects().clear();
            portfolioRepository.save(portfolio);
            List<Project> newProjects = request.getProjects().stream()
                    .map(p -> toProjectEntity(p, portfolio))
                    .toList();
            portfolio.getProjects().addAll(newProjects);
        }
        return toResponse(portfolioRepository.save(portfolio));
    }

    // ── Supprimer portfolio
    @Transactional
    public void deletePortfolio(Long id) {
        if (!portfolioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Portfolio not found with id: " + id);
        }
        portfolioRepository.deleteById(id);
    }

    // ── Ajouter projet
    @Transactional
    public ProjectResponse addProject(Long portfolioId, ProjectRequest request) {
        Portfolio portfolio = findById(portfolioId);
        return toProjectResponse(projectRepository.save(
                toProjectEntity(request, portfolio)));
    }

    // ── Récupérer projets
    public List<ProjectResponse> getProjects(Long portfolioId) {
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new ResourceNotFoundException(
                    "Portfolio not found with id: " + portfolioId);
        }
        return projectRepository.findByPortfolioId(portfolioId)
                .stream().map(this::toProjectResponse).toList();
    }

    // ── Modifier projet
    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + projectId));
        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setTechStack(request.getTechStack());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setGithubUrl(request.getGithubUrl());
        project.setDemoUrl(request.getDemoUrl());
        project.setImages(request.getImages());
        return toProjectResponse(projectRepository.save(project));
    }

    // ── Supprimer projet
    @Transactional
    public void deleteProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException(
                    "Project not found with id: " + projectId);
        }
        projectRepository.deleteById(projectId);
    }

    // ── Mappers
    private Portfolio findById(Long id) {
        return portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portfolio not found with id: " + id));
    }

    private Project toProjectEntity(ProjectRequest req, Portfolio portfolio) {
        return Project.builder()
                .portfolio(portfolio)
                .title(req.getTitle())
                .description(req.getDescription())
                .techStack(req.getTechStack())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .githubUrl(req.getGithubUrl())
                .demoUrl(req.getDemoUrl())
                .images(req.getImages())
                .build();
    }

    private ProjectResponse toProjectResponse(Project p) {
        return ProjectResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .techStack(p.getTechStack())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .githubUrl(p.getGithubUrl())
                .demoUrl(p.getDemoUrl())
                .images(p.getImages())
                .build();
    }

    private PortfolioResponse toResponse(Portfolio p) {
        List<Project> projects = p.getProjects() != null
                ? p.getProjects()
                : projectRepository.findByPortfolioId(p.getId());
        return PortfolioResponse.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .headline(p.getHeadline())
                .linkedinUrl(p.getLinkedinUrl())
                .githubUrl(p.getGithubUrl())
                .location(p.getLocation())
                .isPublic(p.isPublic())
                .viewsCount(p.getViewsCount())
                .projects(projects.stream().map(this::toProjectResponse).toList())
                .build();
    }

    @Transactional
    public PortfolioResponse toggleVisibility(Long id) {
        Portfolio portfolio = findById(id);
        portfolio.setPublic(!portfolio.isPublic());
        return toResponse(portfolioRepository.save(portfolio));
    }

    @Transactional
    public PortfolioResponse incrementViews(Long id) {
        Portfolio portfolio = findById(id);
        portfolio.setViewsCount(portfolio.getViewsCount() + 1);
        return toResponse(portfolioRepository.save(portfolio));
    }

    public List<PortfolioResponse> getAllPublic() {
        return portfolioRepository.findByIsPublicTrue()
                .stream().map(this::toResponse).toList();
    }

    // ── Récupérer tous les portfolios (admin)
    public List<PortfolioResponse> getAll() {
        return portfolioRepository.findAll()
                .stream().map(this::toResponse).toList();
    }
}