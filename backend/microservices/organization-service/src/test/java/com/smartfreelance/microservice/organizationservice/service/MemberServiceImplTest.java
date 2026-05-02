package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.response.MemberResponse;
import com.smartfreelance.microservice.organizationservice.entity.OrganizationMember;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.enums.MemberStatus;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberServiceImpl Unit Tests")
class MemberServiceImplTest {

    @Mock private OrganizationMemberRepository memberRepo;
    @Mock private AuditLogRepository           auditRepo;

    @InjectMocks private MemberServiceImpl service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OrganizationMember member(String id, String orgId, String userId, MemberRole role) {
        return OrganizationMember.builder()
                .id(id).organizationId(orgId).userId(userId)
                .role(role).status(MemberStatus.ACTIVE).build();
    }

    // ── getMembers ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMembers — returns page mapped to response DTOs")
    void getMembers_returnsPage() {
        var m = member("m1", "org1", "u1", MemberRole.MEMBER);
        when(memberRepo.findByOrganizationId(eq("org1"), any()))
                .thenReturn(new PageImpl<>(List.of(m)));

        var page = service.getMembers("org1", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getUserId()).isEqualTo("u1");
    }

    // ── updateRole ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateRole — owner can change a member's role to MANAGER")
    void updateRole_ownerCanChange() {
        var owner  = member("mOwner", "org1", "owner1", MemberRole.OWNER);
        var target = member("mTarget", "org1", "user2", MemberRole.MEMBER);

        when(memberRepo.findByOrganizationIdAndUserId("org1", "owner1")).thenReturn(Optional.of(owner));
        when(memberRepo.findById("mTarget")).thenReturn(Optional.of(target));
        when(memberRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        MemberResponse resp = service.updateRole("org1", "mTarget", MemberRole.MANAGER, "owner1");

        assertThat(resp.getRole()).isEqualTo(MemberRole.MANAGER);
        verify(auditRepo).save(any());
    }

    @Test
    @DisplayName("updateRole — plain member cannot change roles")
    void updateRole_memberCannotChange() {
        var plainMember = member("mPlain", "org1", "plain1", MemberRole.MEMBER);
        when(memberRepo.findByOrganizationIdAndUserId("org1", "plain1")).thenReturn(Optional.of(plainMember));

        assertThatThrownBy(() -> service.updateRole("org1", "mTarget", MemberRole.MANAGER, "plain1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("owners and managers");
    }

    @Test
    @DisplayName("updateRole — cannot assign OWNER role via updateRole")
    void updateRole_cannotAssignOwnerRole() {
        var manager = member("mMgr", "org1", "mgr1", MemberRole.MANAGER);
        when(memberRepo.findByOrganizationIdAndUserId("org1", "mgr1")).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> service.updateRole("org1", "mTarget", MemberRole.OWNER, "mgr1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("transfer ownership");
    }

    @Test
    @DisplayName("updateRole — throws ResourceNotFoundException when target member missing")
    void updateRole_throwsWhenTargetMissing() {
        var owner = member("mOwner", "org1", "owner1", MemberRole.OWNER);
        when(memberRepo.findByOrganizationIdAndUserId("org1", "owner1")).thenReturn(Optional.of(owner));
        when(memberRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRole("org1", "missing", MemberRole.MEMBER, "owner1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── removeMember ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("removeMember — owner can set a member as INACTIVE")
    void removeMember_ownerCanRemoveMember() {
        var owner  = member("mOwner", "org1", "owner1", MemberRole.OWNER);
        var target = member("mTarget", "org1", "user2", MemberRole.MEMBER);

        when(memberRepo.findByOrganizationIdAndUserId("org1", "owner1")).thenReturn(Optional.of(owner));
        when(memberRepo.findById("mTarget")).thenReturn(Optional.of(target));

        service.removeMember("org1", "mTarget", "owner1");

        ArgumentCaptor<OrganizationMember> captor = ArgumentCaptor.forClass(OrganizationMember.class);
        verify(memberRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MemberStatus.INACTIVE);
        assertThat(captor.getValue().getLeftAt()).isNotNull();
        verify(auditRepo).save(any());
    }

    @Test
    @DisplayName("removeMember — cannot remove the owner")
    void removeMember_cannotRemoveOwner() {
        var manager     = member("mMgr", "org1", "mgr1", MemberRole.MANAGER);
        var targetOwner = member("mOwner", "org1", "owner1", MemberRole.OWNER);

        when(memberRepo.findByOrganizationIdAndUserId("org1", "mgr1")).thenReturn(Optional.of(manager));
        when(memberRepo.findById("mOwner")).thenReturn(Optional.of(targetOwner));

        assertThatThrownBy(() -> service.removeMember("org1", "mOwner", "mgr1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("owner");
    }

    @Test
    @DisplayName("removeMember — plain member cannot remove anyone")
    void removeMember_plainMemberCannotRemove() {
        var plain = member("mPlain", "org1", "plain1", MemberRole.MEMBER);
        when(memberRepo.findByOrganizationIdAndUserId("org1", "plain1")).thenReturn(Optional.of(plain));

        assertThatThrownBy(() -> service.removeMember("org1", "mTarget", "plain1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("owners and managers");
    }

    // ── leave ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("leave — regular member can leave the organization")
    void leave_memberCanLeave() {
        var m = member("m1", "org1", "user1", MemberRole.MEMBER);
        when(memberRepo.findByOrganizationIdAndUserId("org1", "user1")).thenReturn(Optional.of(m));

        service.leave("org1", "user1");

        ArgumentCaptor<OrganizationMember> captor = ArgumentCaptor.forClass(OrganizationMember.class);
        verify(memberRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MemberStatus.INACTIVE);
        verify(auditRepo).save(any());
    }

    @Test
    @DisplayName("leave — owner cannot leave without transferring ownership first")
    void leave_ownerCannotLeave() {
        var owner = member("mOwner", "org1", "owner1", MemberRole.OWNER);
        when(memberRepo.findByOrganizationIdAndUserId("org1", "owner1")).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.leave("org1", "owner1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Transfer ownership");
    }

    @Test
    @DisplayName("leave — throws ResourceNotFoundException when user is not a member")
    void leave_throwsWhenNotMember() {
        when(memberRepo.findByOrganizationIdAndUserId("org1", "stranger")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.leave("org1", "stranger"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getMyMemberships ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyMemberships — returns only active memberships for a user")
    void getMyMemberships_returnsActiveMemberships() {
        var m = member("m1", "org1", "user1", MemberRole.MEMBER);
        when(memberRepo.findByUserIdAndStatus("user1", MemberStatus.ACTIVE)).thenReturn(List.of(m));

        var result = service.getMyMemberships("user1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrganizationId()).isEqualTo("org1");
    }

    // ── toggleProfileDisplay ─────────────────────────────────────────────────

    @Test
    @DisplayName("toggleProfileDisplay — updates the displayOnProfile flag")
    void toggleProfileDisplay_updatesFlag() {
        var m = member("m1", "org1", "user1", MemberRole.MEMBER);
        m.setDisplayOnProfile(false);

        when(memberRepo.findByOrganizationIdAndUserId("org1", "user1")).thenReturn(Optional.of(m));
        when(memberRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        MemberResponse resp = service.toggleProfileDisplay("org1", "user1", true);

        assertThat(resp.isDisplayOnProfile()).isTrue();
    }

    @Test
    @DisplayName("toggleProfileDisplay — throws ResourceNotFoundException when member missing")
    void toggleProfileDisplay_throwsWhenMissing() {
        when(memberRepo.findByOrganizationIdAndUserId("org1", "ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggleProfileDisplay("org1", "ghost", true))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
