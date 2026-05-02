package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.CreateTemplateRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.ResponseTemplateResponse;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.ResponseTemplate;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.repository.ResponseTemplateRepository;
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
class ResponseTemplateServiceImplTest {

    @Mock private ResponseTemplateRepository templateRepo;

    @InjectMocks private ResponseTemplateServiceImpl templateService;

    @Test
    void create_Success() {
        CreateTemplateRequest req = CreateTemplateRequest.builder()
                .title("Politesse")
                .content("Bonjour, merci pour votre retour.")
                .category(Complaint.ComplaintCategory.OTHER)
                .build();

        when(templateRepo.save(any(ResponseTemplate.class))).thenAnswer(i -> {
            ResponseTemplate saved = i.getArgument(0);
            saved.setId("tmpl-1");
            return saved;
        });

        ResponseTemplateResponse res = templateService.create(req, "admin-007");

        assertNotNull(res.getId());
        assertEquals("Politesse", res.getTitle());
        verify(templateRepo).save(any());
    }

    @Test
    void update_Success() {
        ResponseTemplate existing = ResponseTemplate.builder().id("t1").title("Old").build();
        CreateTemplateRequest req = CreateTemplateRequest.builder().title("New").build();

        when(templateRepo.findById("t1")).thenReturn(Optional.of(existing));
        when(templateRepo.save(any())).thenReturn(existing);

        ResponseTemplateResponse res = templateService.update("t1", req);

        assertEquals("New", res.getTitle());
    }

    @Test
    void delete_ShouldPerformSoftDelete() {
        ResponseTemplate template = ResponseTemplate.builder().id("t1").active(true).build();
        when(templateRepo.findById("t1")).thenReturn(Optional.of(template));

        templateService.delete("t1");

        assertFalse(template.isActive());
        verify(templateRepo).save(template);
    }

    @Test
    void getByCategory_ShouldCombineSpecificAndGeneral() {
        ResponseTemplate specific = ResponseTemplate.builder().title("Specific").build();
        ResponseTemplate general = ResponseTemplate.builder().title("General").build();

        when(templateRepo.findByCategoryAndActiveTrue(Complaint.ComplaintCategory.SCAM))
                .thenReturn(List.of(specific));
        when(templateRepo.findByCategoryIsNullAndActiveTrue())
                .thenReturn(List.of(general));

        List<ResponseTemplateResponse> results = templateService.getByCategory(Complaint.ComplaintCategory.SCAM);

        assertEquals(2, results.size());
        verify(templateRepo).findByCategoryAndActiveTrue(any());
        verify(templateRepo).findByCategoryIsNullAndActiveTrue();
    }

    @Test
    void recordUsage_Success() {
        ResponseTemplate template = ResponseTemplate.builder().id("t1").usageCount(5).build();
        when(templateRepo.findById("t1")).thenReturn(Optional.of(template));
        when(templateRepo.save(any())).thenReturn(template);

        ResponseTemplateResponse res = templateService.recordUsage("t1");

        assertEquals(6, res.getUsageCount());
    }

    @Test
    void getAll_ReturnsActiveTemplates() {
        ResponseTemplate t = ResponseTemplate.builder().active(true).build();
        when(templateRepo.findByActiveTrueOrderByUsageCountDesc()).thenReturn(List.of(t));

        List<ResponseTemplateResponse> results = templateService.getAll();

        assertFalse(results.isEmpty());
        verify(templateRepo).findByActiveTrueOrderByUsageCountDesc();
    }

    @Test
    void findById_NotFound_ThrowsException() {
        when(templateRepo.findById("unknown")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> templateService.recordUsage("unknown"));
    }
}