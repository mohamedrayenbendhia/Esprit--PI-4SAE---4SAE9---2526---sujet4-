package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.UserSanctionResponse;
import com.smartfreelance.microservice.complaintservice.entity.UserSanction;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationService;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import com.smartfreelance.microservice.complaintservice.repository.UserSanctionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SanctionServiceImplTest {

    @Mock private UserSanctionRepository       sanctionRepo;
    @Mock private ComplaintRepository          complaintRepo;
    @Mock private ComplaintNotificationService notificationService;

    @InjectMocks private SanctionServiceImpl sanctionService;

    @Test
    void applyAutomatic_FirstOffense_ShouldGiveWarning() {
        // Mock : 0 sanction existante
        when(sanctionRepo.countByUserId("user1")).thenReturn(0L);
        when(sanctionRepo.save(any(UserSanction.class))).thenAnswer(i -> i.getArgument(0));

        UserSanctionResponse res = sanctionService.applyAutomatic("user1", "comp123");

        assertEquals(UserSanction.SanctionType.WARNING, res.getType());
        assertTrue(res.isAppliedBySystem());
        assertNull(res.getExpiresAt());
    }

    @Test
    void applyAutomatic_SecondOffense_ShouldSuspendTemp() {
        // Mock : 1 sanction existante
        when(sanctionRepo.countByUserId("user1")).thenReturn(1L);
        when(sanctionRepo.save(any(UserSanction.class))).thenAnswer(i -> i.getArgument(0));

        UserSanctionResponse res = sanctionService.applyAutomatic("user1", "comp123");

        assertEquals(UserSanction.SanctionType.TEMP_SUSPENSION, res.getType());
        assertNotNull(res.getExpiresAt());
    }

    @Test
    void applyAutomatic_ThirdOffense_ShouldSuspendPermanent() {
        // Mock : 2 sanctions ou plus
        when(sanctionRepo.countByUserId("user1")).thenReturn(2L);
        when(sanctionRepo.save(any(UserSanction.class))).thenAnswer(i -> i.getArgument(0));

        UserSanctionResponse res = sanctionService.applyAutomatic("user1", "comp123");

        assertEquals(UserSanction.SanctionType.PERMANENT_SUSPENSION, res.getType());
    }

    @Test
    void applyManual_ShouldWorkForAdmin() {
        when(sanctionRepo.save(any(UserSanction.class))).thenAnswer(i -> i.getArgument(0));

        UserSanctionResponse res = sanctionService.applyManual(
                "user1", "Comportement inapproprié", UserSanction.SanctionType.WARNING, "admin-456"
        );

        assertEquals(UserSanction.SanctionType.WARNING, res.getType());
        assertFalse(res.isAppliedBySystem());
        verify(sanctionRepo).save(any());
    }

    @Test
    void liftSanction_Success() {
        UserSanction sanction = UserSanction.builder()
                .id("s1")
                .active(true)
                .build();

        when(sanctionRepo.findById("s1")).thenReturn(Optional.of(sanction));
        when(sanctionRepo.save(any())).thenReturn(sanction);

        UserSanctionResponse res = sanctionService.liftSanction("s1", "admin-789");

        assertFalse(res.isActive());
        verify(sanctionRepo).save(sanction);
    }

    @Test
    void liftSanction_NotFound_ThrowsException() {
        when(sanctionRepo.findById("unknown")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> sanctionService.liftSanction("unknown", "admin-1"));
    }

    @Test
    void expireOldSanctions_ShouldReturnCount() {
        when(sanctionRepo.expireOldSanctions(any())).thenReturn(5);
        int result = sanctionService.expireOldSanctions();
        assertEquals(5, result);
    }

    @Test
    void getForUser_ShouldReturnList() {
        UserSanction s = UserSanction.builder().userId("user1").build();
        when(sanctionRepo.findByUserId("user1")).thenReturn(List.of(s));

        List<UserSanctionResponse> results = sanctionService.getForUser("user1");

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }
}