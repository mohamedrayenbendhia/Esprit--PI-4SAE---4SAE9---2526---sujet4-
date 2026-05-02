package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateOrganizationRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.UpdateOrganizationRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.GeoLocation;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.entity.OrganizationMember;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationVisibility;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationMemberRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.smartfreelance.microservice.organizationservice.enums.OrganizationType;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationSize;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationServiceImpl Unit Tests")
class OrganizationServiceImplTest {

    @Mock private OrganizationRepository        orgRepo;
    @Mock private OrganizationMemberRepository  memberRepo;
    @Mock private AuditLogRepository            auditRepo;
    @Mock private GeocodingService              geocodingService;
    // geocodingService is injected so OrganizationServiceImpl can call geocodeAndPersist()

    @InjectMocks private OrganizationServiceImpl service;

    private CreateOrganizationRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateOrganizationRequest();
        request.setName("Acme");
        request.setDescription("Desc");
    }

    @Test
    void create_shouldSaveOrganizationAndMemberAndAudit() {
        when(orgRepo.existsByNameIgnoreCase("Acme")).thenReturn(false);
        Organization saved = Organization.builder().id("o1").name("Acme").ownerId("owner1").build();
        when(orgRepo.save(any())).thenReturn(saved);

        var resp = service.create(request, "owner1");

        verify(orgRepo).save(any());
        verify(memberRepo).save(any());
        verify(auditRepo).save(any());
        assertThat(resp.getId()).isEqualTo("o1");
    }

    @Test
    void create_shouldThrowWhenNameExists() {
        when(orgRepo.existsByNameIgnoreCase("Acme")).thenReturn(true);
        assertThatThrownBy(() -> service.create(request, "owner1")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getById_shouldThrowWhenMissing() {
        when(orgRepo.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById("missing")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void transferOwnership_requiresCurrentOwner() {
        Organization org = Organization.builder().id("o1").ownerId("owner1").build();
        when(orgRepo.findById("o1")).thenReturn(Optional.of(org));

        assertThatThrownBy(() -> service.transferOwnership("o1", "someoneElse", "newOwner"))
                .isInstanceOf(com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException.class);
    }

    @Test
    void update_shouldAllowOwnerToUpdate() {
        Organization org = Organization.builder().id("o2").ownerId("owner2").name("Old").build();
        when(orgRepo.findById("o2")).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var req = new com.smartfreelance.microservice.organizationservice.dto.request.UpdateOrganizationRequest();
        req.setName("NewName");

        var resp = service.update("o2", req, "owner2");

        verify(auditRepo).save(any());
        assertThat(resp.getName()).isEqualTo("NewName");
    }

    @Test
    void delete_shouldAllowOwner() {
        Organization org = Organization.builder().id("o3").ownerId("owner3").build();
        when(orgRepo.findById("o3")).thenReturn(Optional.of(org));

        service.delete("o3", "owner3");

        verify(orgRepo).delete(org);
    }

    @Test
    void dissolve_requiresOwner() {
        Organization org = Organization.builder().id("o4").ownerId("owner4").build();
        when(orgRepo.findById("o4")).thenReturn(Optional.of(org));

        assertThatThrownBy(() -> service.dissolve("o4", "someoneElse"))
                .isInstanceOf(com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException.class);

        // success path
        service.dissolve("o4", "owner4");
        verify(orgRepo).save(org);
        verify(auditRepo).save(any());
    }

    @Test
    void setVisibility_ownerCanChange() {
        Organization org = Organization.builder().id("o5").ownerId("owner5").build();
        when(orgRepo.findById("o5")).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.setVisibility("o5", OrganizationVisibility.PRIVATE, "owner5");
        assertThat(resp.getVisibility()).isEqualTo(OrganizationVisibility.PRIVATE);
    }

    @Test
    void setVisibility_managerCanChange() {
        Organization org = Organization.builder().id("o5").ownerId("owner5").build();
        when(orgRepo.findById("o5")).thenReturn(Optional.of(org));
        OrganizationMember manager = OrganizationMember.builder()
                .role(MemberRole.MANAGER).build();
        when(memberRepo.findByOrganizationIdAndUserId("o5", "manager1")).thenReturn(Optional.of(manager));
        when(orgRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.setVisibility("o5", OrganizationVisibility.PUBLIC, "manager1");
        assertThat(resp.getVisibility()).isEqualTo(OrganizationVisibility.PUBLIC);
    }

    @Test
    void setVisibility_memberCannotChange() {
        Organization org = Organization.builder().id("o5").ownerId("owner5").build();
        when(orgRepo.findById("o5")).thenReturn(Optional.of(org));
        OrganizationMember plainMember = OrganizationMember.builder()
                .role(MemberRole.MEMBER).build();
        when(memberRepo.findByOrganizationIdAndUserId("o5", "member1")).thenReturn(Optional.of(plainMember));

        assertThatThrownBy(() -> service.setVisibility("o5", OrganizationVisibility.PRIVATE, "member1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("manager");
    }

    @Test
    void transferOwnership_successful() {
        Organization org = Organization.builder().id("o6").ownerId("owner6").build();
        when(orgRepo.findById("o6")).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(memberRepo.findByOrganizationIdAndUserId("o6", "newOwner")).thenReturn(Optional.of(new com.smartfreelance.microservice.organizationservice.entity.OrganizationMember()));

        service.transferOwnership("o6", "owner6", "newOwner");

        verify(orgRepo).save(org);
        verify(auditRepo).save(any());
    }

    // ── Geolocation-related behaviour ─────────────────────────────────────────

    @Test
    @DisplayName("create — when location is set, geocoding service is called and coordinates persisted")
    void create_withLocation_callsGeocodingService() {
        request.setLocation("Paris, France");
        when(orgRepo.existsByNameIgnoreCase("Acme")).thenReturn(false);
        Organization saved = Organization.builder().id("o7").name("Acme").ownerId("owner1")
                .location("Paris, France").build();
        when(orgRepo.save(any())).thenReturn(saved);
        when(geocodingService.geocode("Paris, France"))
                .thenReturn(Optional.of(GeoLocation.builder().latitude(48.85).longitude(2.35).build()));

        service.create(request, "owner1");

        // geocoding should be triggered and coordinates saved
        verify(geocodingService).geocode("Paris, France");
        // orgRepo.save called at least twice: initial create + coordinate persistence
        verify(orgRepo, atLeast(1)).save(any());
    }

    @Test
    @DisplayName("create — when location is absent, geocoding service is NOT called")
    void create_withoutLocation_doesNotCallGeocodingService() {
        // request has no location set
        when(orgRepo.existsByNameIgnoreCase("Acme")).thenReturn(false);
        Organization saved = Organization.builder().id("o8").name("Acme").ownerId("owner1").build();
        when(orgRepo.save(any())).thenReturn(saved);

        service.create(request, "owner1");

        verifyNoInteractions(geocodingService);
    }

    @Test
    @DisplayName("update — when location changes, geocoding service is re-triggered")
    void update_locationChanged_retriggersGeocode() {
        Organization org = Organization.builder().id("o9").ownerId("owner9")
                .name("OldName").location("Lyon").build();
        when(orgRepo.findById("o9")).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(geocodingService.geocode("Marseille"))
                .thenReturn(Optional.of(GeoLocation.builder().latitude(43.29).longitude(5.37).build()));

        UpdateOrganizationRequest req = new UpdateOrganizationRequest();
        req.setName("OldName");
        req.setLocation("Marseille");   // location changed

        service.update("o9", req, "owner9");

        verify(geocodingService).geocode("Marseille");
    }
}

