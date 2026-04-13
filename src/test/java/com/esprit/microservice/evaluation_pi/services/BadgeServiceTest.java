package com.esprit.microservice.evaluation_pi.services;

import com.esprit.microservice.evaluation_pi.entities.Badge;
import com.esprit.microservice.evaluation_pi.entities.UserBadge;
import com.esprit.microservice.evaluation_pi.repositories.BadgeRepository;
import com.esprit.microservice.evaluation_pi.repositories.UserBadgeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @InjectMocks
    private BadgeService badgeService;

    @Test
    void testCreateBadge() {
        // Given
        Badge badge = new Badge();
        badge.setName("Expert");
        when(badgeRepository.save(any(Badge.class))).thenReturn(badge);

        // When
        Badge result = badgeService.createBadge(badge);

        // Then
        assertNotNull(result);
        assertEquals("Expert", result.getName());
        verify(badgeRepository, times(1)).save(badge);
    }

    @Test
    void testGetBadgeById_NotFound() {
        // Given
        when(badgeRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            badgeService.getBadgeById(1L);
        });

        assertTrue(exception.getMessage().contains("Badge non trouvé"));
    }

    @Test
    void testCheckAndAssignBadges_ShouldAssignWhenCriteriaMet() {
        // Given
        String userId = "user123";

        Badge expertBadge = new Badge();
        expertBadge.setId(1L);
        expertBadge.setName("Expert");
        expertBadge.setMinScore(15.0);
        expertBadge.setMinProjects(Math.toIntExact(5L));

        when(badgeRepository.findAll()).thenReturn(Arrays.asList(expertBadge));
        // L'utilisateur n'a pas encore ce badge
        when(userBadgeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // When
        badgeService.checkAndAssignBadges(userId, 18.0, 10L);

        // Then
        // On vérifie qu'un UserBadge a été sauvegardé
        verify(userBadgeRepository, times(1)).save(any(UserBadge.class));
    }
}