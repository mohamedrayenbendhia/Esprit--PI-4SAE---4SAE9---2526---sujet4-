package com.esprit.microservice.serviceTest;

import com.esprit.microservice.contrat_backend.entities.Contract;
import com.esprit.microservice.contrat_backend.entities.Milestone;
import com.esprit.microservice.contrat_backend.entities.MilestoneStatus;
import com.esprit.microservice.contrat_backend.repositories.ContractRepository;
import com.esprit.microservice.contrat_backend.repositories.MilestoneRepository;
import com.esprit.microservice.contrat_backend.services.MilestoneService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilestoneTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private MilestoneService milestoneService;

    @Nested
    @DisplayName("Tests de Création")
    class CreationTests {

        @Test
        @DisplayName("Devrait créer un milestone avec un numéro de séquence automatique")
        void shouldCreateMilestoneWithSequence() {
            // Arrange
            Long contractId = 1L;
            Contract contract = new Contract();
            contract.setId(contractId);

            Milestone milestone = new Milestone();
            milestone.setTitle("Phase 1");

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
            // Simuler qu'il y a déjà 2 milestones existants
            when(milestoneRepository.findByContractIdOrderBySequenceNumber(contractId))
                    .thenReturn(new ArrayList<>(List.of(new Milestone(), new Milestone())));
            when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            Milestone result = milestoneService.create(contractId, milestone);

            // Assert
            assertThat(result.getContract()).isEqualTo(contract);
            assertThat(result.getSequenceNumber()).isEqualTo(3); // 2 existants + 1
            assertThat(result.getStatus()).isEqualTo(MilestoneStatus.PENDING);
            verify(milestoneRepository).save(milestone);
        }

        @Test
        @DisplayName("Devrait échouer si le contrat n'existe pas")
        void shouldThrowExceptionWhenContractNotFound() {
            when(contractRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> milestoneService.create(1L, new Milestone()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Contract not found");
        }
    }

    @Nested
    @DisplayName("Tests de Validation Workflow")
    class ValidationTests {

        @Test
        @DisplayName("Devrait valider un milestone en attente")
        void shouldValidateMilestone() {
            // Arrange
            Long id = 10L;
            Milestone milestone = new Milestone();
            milestone.setStatus(MilestoneStatus.AWAITING_VALIDATION);

            when(milestoneRepository.findById(id)).thenReturn(Optional.of(milestone));
            when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            Milestone validated = milestoneService.validate(id, "ADMIN_1", "Travail bien fait");

            // Assert
            assertThat(validated.getStatus()).isEqualTo(MilestoneStatus.VALIDATED);
            assertThat(validated.getValidatedBy()).isEqualTo("ADMIN_1");
            assertThat(validated.getValidatedAt()).isNotNull();
            assertThat(validated.getValidationComment()).isEqualTo("Travail bien fait");
        }

        @Test
        @DisplayName("Devrait rejeter un milestone avec un motif")
        void shouldRejectMilestone() {
            // Arrange
            Long id = 10L;
            Milestone milestone = new Milestone();
            milestone.setStatus(MilestoneStatus.AWAITING_VALIDATION);

            when(milestoneRepository.findById(id)).thenReturn(Optional.of(milestone));
            when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            Milestone rejected = milestoneService.reject(id, "Document incomplet");

            // Assert
            assertThat(rejected.getStatus()).isEqualTo(MilestoneStatus.REJECTED);
            assertThat(rejected.getValidationComment()).isEqualTo("Document incomplet");
        }

        @Test
        @DisplayName("Devrait bloquer la validation si le statut n'est pas AWAITING_VALIDATION")
        void shouldThrowExceptionOnInvalidStatusForValidation() {
            // Arrange
            Milestone milestone = new Milestone();
            milestone.setStatus(MilestoneStatus.PENDING); // Pas encore soumis pour validation

            when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

            // Act & Assert
            assertThatThrownBy(() -> milestoneService.validate(1L, "UID", "OK"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("must be awaiting validation");
        }
    }
}