package com.esprit.microservice.serviceTest;

import com.esprit.microservice.contrat_backend.entities.Contract;
import com.esprit.microservice.contrat_backend.entities.PaymentSchedule;
import com.esprit.microservice.contrat_backend.entities.PaymentStatus;
import com.esprit.microservice.contrat_backend.repositories.ContractRepository;
import com.esprit.microservice.contrat_backend.repositories.PaymentScheduleRepository;
import com.esprit.microservice.contrat_backend.services.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayementTest {

    @Mock
    private PaymentScheduleRepository paymentScheduleRepository;

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Nested
    @DisplayName("Tests de Création de Paiement")
    class CreationTests {

        @Test
        @DisplayName("Devrait créer un paiement avec le bon numéro de séquence")
        void shouldCreatePaymentWithCorrectSequence() {
            // Arrange
            Long contractId = 1L;
            Contract contract = new Contract();
            contract.setId(contractId);

            PaymentSchedule newPayment = new PaymentSchedule();

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
            // Simuler qu'il existe déjà 1 paiement
            when(paymentScheduleRepository.findByContractIdOrderBySequenceNumber(contractId))
                    .thenReturn(new ArrayList<>(List.of(new PaymentSchedule())));
            when(paymentScheduleRepository.save(any(PaymentSchedule.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            PaymentSchedule result = paymentService.create(contractId, newPayment);

            // Assert
            assertThat(result.getSequenceNumber()).isEqualTo(2);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(result.getContract()).isEqualTo(contract);
            verify(paymentScheduleRepository).save(any(PaymentSchedule.class));
        }
    }

    @Nested
    @DisplayName("Tests de Gestion des Statuts")
    class StatusTests {

        @Test
        @DisplayName("Devrait marquer comme payé avec une date et une facture")
        void shouldMarkAsPaidWithInvoice() {
            // Arrange
            Long paymentId = 100L;
            PaymentSchedule payment = new PaymentSchedule();
            payment.setStatus(PaymentStatus.PENDING);

            when(paymentScheduleRepository.findById(paymentId)).thenReturn(Optional.of(payment));
            when(paymentScheduleRepository.save(any(PaymentSchedule.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            PaymentSchedule result = paymentService.markAsPaid(paymentId, "INV-2024-001");

            // Assert
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(result.getPaidAt()).isNotNull();
            assertThat(result.getInvoiceNumber()).isEqualTo("INV-2024-001");
        }

        @Test
        @DisplayName("Devrait mettre à jour le statut en OVERDUE")
        void shouldUpdateStatusToOverdue() {
            // Arrange
            Long paymentId = 100L;
            PaymentSchedule payment = new PaymentSchedule();

            when(paymentScheduleRepository.findById(paymentId)).thenReturn(Optional.of(payment));
            when(paymentScheduleRepository.save(any(PaymentSchedule.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            PaymentSchedule result = paymentService.markAsOverdue(paymentId);

            // Assert
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.OVERDUE);
            verify(paymentScheduleRepository).save(payment);
        }
    }

    @Test
    @DisplayName("Test de suppression")
    void shouldDeletePayment() {
        // Act
        paymentService.delete(1L);

        // Assert
        verify(paymentScheduleRepository, times(1)).deleteById(1L);
    }
}