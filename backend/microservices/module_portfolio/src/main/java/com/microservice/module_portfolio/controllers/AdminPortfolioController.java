package com.microservice.module_portfolio.controllers;

import com.microservice.module_portfolio.dto.*;
import com.microservice.module_portfolio.services.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/portfolios")
@RequiredArgsConstructor
public class AdminPortfolioController {

    private final PortfolioService portfolioService;

    // GET /api/admin/portfolios
    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getAll() {
        return ResponseEntity.ok(portfolioService.getAll());
    }

    // GET /api/admin/portfolios/{id}
    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(portfolioService.getById(id));
    }

    // GET /api/admin/portfolios/user/{userId}
    @GetMapping("/user/{userId}")
    public ResponseEntity<PortfolioResponse> getByUserId(@PathVariable String userId) { // ✅
        return ResponseEntity.ok(portfolioService.getByUserId(userId));
    }

    // DELETE /api/admin/portfolios/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.noContent().build();
    }
}