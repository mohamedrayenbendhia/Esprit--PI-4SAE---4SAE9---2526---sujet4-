package com.microservice.module_portfolio.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "portfolios")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId; // ✅ UUID en String

    @Column(nullable = false)
    private String headline;
    private String linkedinUrl;
    private String githubUrl;
    private String location;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPublic = true;

    @Column(nullable = false)
    @Builder.Default
    private int viewsCount = 0;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default // ✅
    private List<Project> projects = new ArrayList<>();
}