package com.esprit.microservice.serviceTest;

import com.esprit.microservice.contrat_backend.entities.*;
import com.esprit.microservice.contrat_backend.repositories.ContractRepository;
import com.esprit.microservice.contrat_backend.repositories.CustomClauseRepository;
import com.esprit.microservice.contrat_backend.services.ContractService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private CustomClauseRepository customClauseRepository;

    @InjectMocks
    private ContractService contractService;

    @Nested
    @DisplayName("Tests pour la création de contrat")
    class CreateTests {

        @Test
        @DisplayName("Devrait initialiser le numéro et le statut DRAFT lors de la création")
        void shouldInitializeContractWhenCreated() {
            // Arrange
            Contract inputContract = new Contract();
            inputContract.setClientId("CLIENT-123");

            when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            Contract created = contractService.create(inputContract);

            // Assert
            assertThat(created.getContractNumber()).startsWith("CTR-");
            assertThat(created.getStatus()).isEqualTo(ContractStatus.DRAFT);
            verify(contractRepository).save(any(Contract.class));
        }
    }

    @Nested
    @DisplayName("Tests pour le Workflow de Signature")
    class SignatureWorkflow {

        @Test
        @DisplayName("Le client peut signer si le contrat est en DRAFT")
        void clientShouldBeAbleToSignDraftContract() {
            // Arrange
            Long contractId = 1L;
            Contract draftContract = new Contract();
            draftContract.setId(contractId);
            draftContract.setStatus(ContractStatus.DRAFT);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
            when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            Contract signed = contractService.signByClient(contractId, "base64-img-data");

            // Assert
            assertThat(signed.getStatus()).isEqualTo(ContractStatus.SIGNED_BY_CLIENT);
            assertThat(signed.getClientSignatureImage()).isEqualTo("base64-img-data");
            assertThat(signed.getClientSignedAt()).isNotNull();
        }

        @Test
        @DisplayName("Devrait lever une exception si le client signe un contrat non DRAFT")
        void shouldThrowExceptionWhenClientSignsNonDraftContract() {
            // Arrange
            Long contractId = 1L;
            Contract activeContract = new Contract();
            activeContract.setStatus(ContractStatus.ACTIVE);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(activeContract));

            // Act & Assert
            assertThatThrownBy(() -> contractService.signByClient(contractId, "img"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("must be in DRAFT status");
        }
    }

    @Nested
    @DisplayName("Tests pour la mise à jour et suppression")
    class UpdateDeleteTests {

        @Test
        @DisplayName("Devrait supprimer un contrat s'il est en statut CANCELLED")
        void shouldDeleteCancelledContract() {
            // Arrange
            Long id = 1L;
            Contract cancelledContract = new Contract();
            cancelledContract.setStatus(ContractStatus.CANCELLED);

            when(contractRepository.findById(id)).thenReturn(Optional.of(cancelledContract));

            // Act
            contractService.delete(id);

            // Assert
            verify(contractRepository).deleteById(id);
        }

        @Test
        @DisplayName("Ne devrait pas mettre à jour un contrat déjà ACTIVE")
        void shouldNotUpdateActiveContract() {
            // Arrange
            Long id = 1L;
            Contract activeContract = new Contract();
            activeContract.setStatus(ContractStatus.ACTIVE);

            when(contractRepository.findById(id)).thenReturn(Optional.of(activeContract));

            // Act & Assert
            assertThatThrownBy(() -> contractService.update(id, new Contract()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Cannot update contract that is not in DRAFT status");
        }
    }

    @Test
    @DisplayName("Test de récupération par ID - Cas introuvable")
    void shouldThrowExceptionWhenContractNotFound() {
        // Arrange
        when(contractRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> contractService.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Contract not found: 99");
    }
}