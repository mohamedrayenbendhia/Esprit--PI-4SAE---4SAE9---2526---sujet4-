package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.InviteMemberRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.InvitationResponse;
import com.smartfreelance.microservice.organizationservice.entity.Invitation;
import com.smartfreelance.microservice.organizationservice.entity.OrganizationMember;
import com.smartfreelance.microservice.organizationservice.enums.InvitationStatus;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.InvitationRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationMemberRepository;
import com.smartfreelance.microservice.organizationservice.service.TrustScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceImplTest {

    @Mock private InvitationRepository invitationRepo;
    @Mock private OrganizationMemberRepository memberRepo;
    @Mock private AuditLogRepository auditRepo;
    @Mock private TrustScoreService trustScoreService;

    @InjectMocks private InvitationServiceImpl service;

    private InviteMemberRequest req;

    @BeforeEach
    void setUp() {
        req = new InviteMemberRequest();
        req.setInviteeEmail("inv@example.com");
        req.setInviteeId(null);
        req.setRole(MemberRole.MEMBER);
    }

    @Test
    void invite_shouldSaveAndReturnResponse() {
        Invitation saved = Invitation.builder()
                .id("inv-1").organizationId("o1").inviteeEmail("inv@example.com")
                .status(InvitationStatus.PENDING).createdAt(LocalDateTime.now()).build();

        // inviter must be OWNER or MANAGER
        when(memberRepo.findByOrganizationIdAndUserId("o1", "owner-1"))
                .thenReturn(Optional.of(OrganizationMember.builder().role(MemberRole.OWNER).build()));
        when(invitationRepo.save(any(Invitation.class))).thenReturn(saved);

        InvitationResponse resp = service.invite("o1", req, "owner-1");

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo("inv-1");
        verify(invitationRepo).save(any(Invitation.class));
        verify(auditRepo).save(any());
    }

    @Test
    void invite_shouldThrowWhenDuplicatePendingForUser() {
        // simulate inviteeId provided and pending exists
        req.setInviteeId("user-7");
        // inviter is an OWNER
        when(memberRepo.findByOrganizationIdAndUserId("o1", "owner-1"))
                .thenReturn(Optional.of(OrganizationMember.builder().role(MemberRole.OWNER).build()));
        // invitee is not already a member
        when(memberRepo.findByOrganizationIdAndUserId("o1", "user-7"))
                .thenReturn(Optional.empty());
        when(invitationRepo.existsByOrganizationIdAndInviteeIdAndStatus("o1", "user-7", InvitationStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> service.invite("o1", req, "owner-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pending");
    }

    @Test
    void respond_acceptShouldCreateMemberAndReturn() {
        Invitation inv = Invitation.builder().id("inv-2").organizationId("o1").inviteeId("user-9").status(InvitationStatus.PENDING).expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(invitationRepo.findById("inv-2")).thenReturn(Optional.of(inv));
        when(invitationRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        InvitationResponse resp = service.respond("inv-2", true, "user-9");

        assertThat(resp).isNotNull();
        assertThat(resp.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(memberRepo).save(any(OrganizationMember.class));
        verify(auditRepo).save(any());
    }

    @Test
    void cancel_shouldMarkCancelled() {
        Invitation inv = Invitation.builder().id("inv-3").organizationId("o1").status(InvitationStatus.PENDING).build();
        when(invitationRepo.findById("inv-3")).thenReturn(Optional.of(inv));
        when(invitationRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.cancel("inv-3", "owner-1");

        verify(invitationRepo).save(argThat(i -> i.getStatus() == InvitationStatus.CANCELLED));
        verify(auditRepo).save(any());
    }

    @Test
    void getOrgInvitations_shouldReturnPage() {
        when(invitationRepo.findByOrganizationId(eq("o1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(Invitation.builder().id("inv-4").build())));

        var page = service.getOrgInvitations("o1", Pageable.unpaged());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}

