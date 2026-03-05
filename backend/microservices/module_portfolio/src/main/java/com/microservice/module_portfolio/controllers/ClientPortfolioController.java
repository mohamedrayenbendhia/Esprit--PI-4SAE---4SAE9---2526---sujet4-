package com.microservice.module_portfolio.controllers;

import com.microservice.module_portfolio.dto.PortfolioResponse;
import com.microservice.module_portfolio.services.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/client/portfolios")
@RequiredArgsConstructor
public class ClientPortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<PortfolioResponse> getByUserId(@PathVariable String userId) {
        PortfolioResponse portfolio = portfolioService.getByUserId(userId);
        if (!portfolio.isPublic()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        portfolioService.incrementViews(portfolio.getId());
        return ResponseEntity.ok(portfolio);
    }

    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getAllPublic() {
        return ResponseEntity.ok(portfolioService.getAllPublic());
    }
}