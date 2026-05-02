import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, switchMap } from 'rxjs';
import { environment } from 'src/environments/environment';

import {
  Contract,
  ContractStatus,
  ContractStatistics,
  FreelancerEarnings,
  Milestone,
  MilestoneStatus,
  PaymentSchedule,
  PaymentStatus,
  CustomClause,
  Currency,
  CurrencyConversion
} from '../../shared/models/contract.model';
import { CurrencyExchangeService, ConvertedAmount } from './currency-exchange.service';

@Injectable({
  providedIn: 'root'
})
export class ContractService {

  private clientUrl = `${environment.contractApiUrl}/api/client/contracts`;
  private freelancerUrl = `${environment.contractApiUrl}/api/freelancer/contracts`;
  private currencyService = inject(CurrencyExchangeService);

  constructor(private http: HttpClient) {}

  // ===== CLIENT - CONTRATS =====

  createContract(contract: Contract): Observable<Contract> {
    return this.http.post<Contract>(this.clientUrl, contract);
  }

  getMyContractsAsClient(clientId: string): Observable<Contract[]> {
    const params = new HttpParams().set('clientId', clientId);
    return this.http.get<Contract[]>(`${this.clientUrl}/my-contracts`, { params });
  }

  getContractById(id: number): Observable<Contract> {
    return this.http.get<Contract>(`${this.clientUrl}/${id}`);
  }

  updateContract(id: number, contract: Contract): Observable<Contract> {
    return this.http.put<Contract>(`${this.clientUrl}/${id}`, contract);
  }

  deleteContract(id: number): Observable<void> {
    return this.http.delete<void>(`${this.clientUrl}/${id}`);
  }

  searchContractsAsClient(clientId: string, status?: ContractStatus, keyword?: string): Observable<Contract[]> {
    let params = new HttpParams().set('clientId', clientId);
    if (status !== undefined && status !== null) {
      params = params.set('status', String(status));
    }
    if (keyword) {
      params = params.set('keyword', keyword);
    }
    return this.http.get<Contract[]>(`${this.clientUrl}/search`, { params });
  }

  getClientStatistics(clientId: string): Observable<ContractStatistics> {
    const params = new HttpParams().set('clientId', clientId);
    return this.http.get<ContractStatistics>(`${this.clientUrl}/statistics`, { params });
  }

  // ===== CLIENT - WORKFLOW =====

  signByClient(id: number, signatureHash: string): Observable<Contract> {
    return this.http.post<Contract>(`${this.clientUrl}/${id}/sign`, { signatureHash });
  }

  signByClientWithCode(id: number, signatureCode: string): Observable<Contract> {
    return this.http.post<Contract>(`${this.clientUrl}/${id}/sign-with-code`, { signatureCode });
  }

  getClientSignatureCode(id: number): Observable<{ code: string; signed: string }> {
    return this.http.get<{ code: string; signed: string }>(`${this.clientUrl}/${id}/my-signature-code`);
  }

  sendToFreelancer(id: number): Observable<Contract> {
    return this.http.post<Contract>(`${this.clientUrl}/${id}/send-to-freelancer`, null);
  }

  activateContract(id: number): Observable<Contract> {
    return this.http.post<Contract>(`${this.clientUrl}/${id}/activate`, null);
  }

  completeContract(id: number): Observable<Contract> {
    return this.http.post<Contract>(`${this.clientUrl}/${id}/complete`, null);
  }

  cancelContract(id: number): Observable<Contract> {
    return this.http.post<Contract>(`${this.clientUrl}/${id}/cancel`, null);
  }

  // ===== CLIENT - PAIEMENTS =====

  addPayment(contractId: number, payment: PaymentSchedule): Observable<PaymentSchedule> {
    return this.http.post<PaymentSchedule>(`${this.clientUrl}/${contractId}/payments`, payment);
  }

  getPayments(contractId: number): Observable<PaymentSchedule[]> {
    return this.http.get<PaymentSchedule[]>(`${this.clientUrl}/${contractId}/payments`);
  }

  markPaymentAsPaid(paymentId: number, invoiceNumber?: string): Observable<PaymentSchedule> {
    let params = new HttpParams();
    if (invoiceNumber) {
      params = params.set('invoiceNumber', invoiceNumber);
    }
    return this.http.put<PaymentSchedule>(`${this.clientUrl}/payments/${paymentId}/mark-paid`, null, { params });
  }

  updatePaymentStatus(paymentId: number, status: PaymentStatus): Observable<PaymentSchedule> {
    const params = new HttpParams().set('status', String(status));
    return this.http.put<PaymentSchedule>(`${this.clientUrl}/payments/${paymentId}/status`, null, { params });
  }

  deletePayment(paymentId: number): Observable<void> {
    return this.http.delete<void>(`${this.clientUrl}/payments/${paymentId}`);
  }

  // ===== CLIENT - JALONS =====

  addMilestone(contractId: number, milestone: Milestone): Observable<Milestone> {
    return this.http.post<Milestone>(`${this.clientUrl}/${contractId}/milestones`, milestone);
  }

  getMilestones(contractId: number): Observable<Milestone[]> {
    return this.http.get<Milestone[]>(`${this.clientUrl}/${contractId}/milestones`);
  }

  validateMilestone(milestoneId: number, validatorId: string, comment?: string): Observable<Milestone> {
    let params = new HttpParams().set('validatorId', validatorId);
    if (comment) {
      params = params.set('comment', comment);
    }
    return this.http.put<Milestone>(`${this.clientUrl}/milestones/${milestoneId}/validate`, null, { params });
  }

  rejectMilestone(milestoneId: number, reason: string): Observable<Milestone> {
    const params = new HttpParams().set('reason', reason);
    return this.http.put<Milestone>(`${this.clientUrl}/milestones/${milestoneId}/reject`, null, { params });
  }

  updateMilestoneStatus(milestoneId: number, status: MilestoneStatus): Observable<Milestone> {
    const params = new HttpParams().set('status', String(status));
    return this.http.put<Milestone>(`${this.clientUrl}/milestones/${milestoneId}/status`, null, { params });
  }

  deleteMilestone(milestoneId: number): Observable<void> {
    return this.http.delete<void>(`${this.clientUrl}/milestones/${milestoneId}`);
  }

  // ===== CLIENT - CLAUSES =====

  addCustomClause(contractId: number, clause: CustomClause): Observable<CustomClause> {
    return this.http.post<CustomClause>(`${this.clientUrl}/${contractId}/clauses`, clause);
  }

  getCustomClauses(contractId: number): Observable<CustomClause[]> {
    return this.http.get<CustomClause[]>(`${this.clientUrl}/${contractId}/clauses`);
  }

  deleteCustomClause(clauseId: number): Observable<void> {
    return this.http.delete<void>(`${this.clientUrl}/clauses/${clauseId}`);
  }

  // ===== FREELANCER - CONTRATS =====

  getMyContractsAsFreelancer(freelancerId: string): Observable<Contract[]> {
    const params = new HttpParams().set('freelancerId', freelancerId);
    return this.http.get<Contract[]>(`${this.freelancerUrl}/my-contracts`, { params });
  }

  getContractByIdAsFreelancer(id: number): Observable<Contract> {
    return this.http.get<Contract>(`${this.freelancerUrl}/${id}`);
  }

  searchContractsAsFreelancer(freelancerId: string, status?: ContractStatus, keyword?: string): Observable<Contract[]> {
    let params = new HttpParams().set('freelancerId', freelancerId);
    if (status !== undefined && status !== null) {
      params = params.set('status', String(status));
    }
    if (keyword) {
      params = params.set('keyword', keyword);
    }
    return this.http.get<Contract[]>(`${this.freelancerUrl}/search`, { params });
  }

  getFreelancerStatistics(freelancerId: string): Observable<ContractStatistics> {
    const params = new HttpParams().set('freelancerId', freelancerId);
    return this.http.get<ContractStatistics>(`${this.freelancerUrl}/statistics`, { params });
  }

  getFreelancerEarnings(freelancerId: string): Observable<FreelancerEarnings> {
    const params = new HttpParams().set('freelancerId', freelancerId);
    return this.http.get<FreelancerEarnings>(`${this.freelancerUrl}/earnings`, { params });
  }

  // ===== FREELANCER - WORKFLOW =====

  signByFreelancer(id: number, signatureHash: string): Observable<Contract> {
    return this.http.post<Contract>(`${this.freelancerUrl}/${id}/sign`, { signatureHash });
  }

  signByFreelancerWithCode(id: number, signatureCode: string): Observable<Contract> {
    return this.http.post<Contract>(`${this.freelancerUrl}/${id}/sign-with-code`, { signatureCode });
  }

  getFreelancerSignatureCode(id: number): Observable<{ code: string; signed: string }> {
    return this.http.get<{ code: string; signed: string }>(`${this.freelancerUrl}/${id}/my-signature-code`);
  }

  acceptContract(id: number): Observable<Contract> {
    return this.http.post<Contract>(`${this.freelancerUrl}/${id}/accept`, null);
  }

  declineContract(id: number, reason: string): Observable<Contract> {
    const params = new HttpParams().set('reason', reason);
    return this.http.post<Contract>(`${this.freelancerUrl}/${id}/decline`, null, { params });
  }

  requestModification(id: number, reason: string): Observable<Contract> {
    const params = new HttpParams().set('reason', reason);
    return this.http.post<Contract>(`${this.freelancerUrl}/${id}/request-modification`, null, { params });
  }

  // ===== FREELANCER - JALONS =====

  startMilestone(milestoneId: number): Observable<Milestone> {
    return this.http.put<Milestone>(`${this.freelancerUrl}/milestones/${milestoneId}/start`, null);
  }

  submitMilestoneForValidation(milestoneId: number): Observable<Milestone> {
    return this.http.put<Milestone>(`${this.freelancerUrl}/milestones/${milestoneId}/submit-for-validation`, null);
  }

  getPendingMilestones(contractId: number): Observable<Milestone[]> {
    return this.http.get<Milestone[]>(`${this.freelancerUrl}/${contractId}/milestones/pending`);
  }

  getPendingPayments(contractId: number): Observable<PaymentSchedule[]> {
    return this.http.get<PaymentSchedule[]>(`${this.freelancerUrl}/${contractId}/payments/pending`);
  }

// ===== GÉNÉRATION PDF =====

/**
 * Génère un PDF du contrat
 * @param contractId ID du contrat
 * @returns Blob du PDF
 */
generatePdf(contractId: number): Observable<Blob> {
  return this.http.get(`${this.clientUrl}/${contractId}/pdf`, {
    responseType: 'blob'
  });
}

  // ===== CONVERSION DE MONNAIE (TAUX DE CHANGE EN TEMPS RÉEL) =====

  /**
   * Convertir le montant d'un contrat via le backend (Frankfurter API)
   */
  convertAmount(contractId: number, toCurrency: string, isFreelancer = false): Observable<{
    from: string;
    to: string;
    originalAmount: number;
    convertedAmount: number;
    rate: number;
    date: string;
  }> {
    const baseUrl = isFreelancer ? this.freelancerUrl : this.clientUrl;
    const params = new HttpParams().set('to', toCurrency);
    return this.http.get<any>(`${baseUrl}/${contractId}/convert`, { params });
  }

  /**
   * Convertir le montant total d'un contrat vers une devise cible
   */
  convertContractAmount(contract: Contract, targetCurrency: Currency): Observable<ConvertedAmount> {
    const fromCurrency = contract.currency ?? Currency.EUR;
    return this.currencyService.convert(contract.totalAmount, fromCurrency, targetCurrency);
  }

  /**
   * Obtenir un contrat avec ses montants convertis dans la devise choisie
   */
  getContractWithConversion(id: number, targetCurrency: Currency): Observable<Contract & { conversion: ConvertedAmount }> {
    return this.getContractById(id).pipe(
      switchMap(contract => {
        const fromCurrency = contract.currency ?? Currency.EUR;
        return this.currencyService.convert(contract.totalAmount, fromCurrency, targetCurrency).pipe(
          map(conversion => ({ ...contract, conversion }))
        );
      })
    );
  }

  /**
   * Obtenir un contrat (freelancer) avec ses montants convertis
   */
  getContractWithConversionAsFreelancer(id: number, targetCurrency: Currency): Observable<Contract & { conversion: ConvertedAmount }> {
    return this.getContractByIdAsFreelancer(id).pipe(
      switchMap(contract => {
        const fromCurrency = contract.currency ?? Currency.EUR;
        return this.currencyService.convert(contract.totalAmount, fromCurrency, targetCurrency).pipe(
          map(conversion => ({ ...contract, conversion }))
        );
      })
    );
  }

  /**
   * Convertir les paiements d'un contrat vers une devise cible
   */
  getPaymentsWithConversion(contractId: number, targetCurrency: Currency): Observable<(PaymentSchedule & { convertedAmount: number; rate: number })[]> {
    return this.getPayments(contractId).pipe(
      switchMap(payments => {
        if (payments.length === 0) return [[]];
        // Use first payment to get the contract currency context
        return this.getContractById(contractId).pipe(
          switchMap(contract => {
            const fromCurrency = contract.currency ?? Currency.EUR;
            const items = payments.map(p => ({ amount: p.amount, from: fromCurrency }));
            return this.currencyService.convertBatch(items, targetCurrency).pipe(
              map(conversions => payments.map((payment, i) => ({
                ...payment,
                convertedAmount: conversions[i].convertedAmount,
                rate: conversions[i].rate
              })))
            );
          })
        );
      })
    );
  }

  /**
   * Convertir les gains du freelancer vers une devise cible
   */
  getFreelancerEarningsWithConversion(
    freelancerId: string,
    fromCurrency: Currency,
    targetCurrency: Currency
  ): Observable<FreelancerEarnings & { convertedTotalEarnings: number; convertedPendingEarnings: number; rate: number }> {
    return this.getFreelancerEarnings(freelancerId).pipe(
      switchMap(earnings =>
        this.currencyService.convert(earnings.totalEarnings, fromCurrency, targetCurrency).pipe(
          switchMap(totalConv =>
            this.currencyService.convert(earnings.pendingEarnings, fromCurrency, targetCurrency).pipe(
              map(pendingConv => ({
                ...earnings,
                convertedTotalEarnings: totalConv.convertedAmount,
                convertedPendingEarnings: pendingConv.convertedAmount,
                rate: totalConv.rate
              }))
            )
          )
        )
      )
    );
  }

  /**
   * Obtenir les contrats client avec montants convertis
   */
  getMyContractsWithConversion(clientId: string, targetCurrency: Currency): Observable<(Contract & { convertedAmount: number; rate: number })[]> {
    return this.getMyContractsAsClient(clientId).pipe(
      switchMap(contracts => {
        if (contracts.length === 0) return [[]];
        const items = contracts.map(c => ({ amount: c.totalAmount, from: c.currency ?? Currency.EUR }));
        return this.currencyService.convertBatch(items, targetCurrency).pipe(
          map(conversions => contracts.map((contract, i) => ({
            ...contract,
            convertedAmount: conversions[i].convertedAmount,
            rate: conversions[i].rate
          })))
        );
      })
    );
  }

  /**
   * Obtenir les contrats freelancer avec montants convertis
   */
  getMyContractsAsFreelancerWithConversion(freelancerId: string, targetCurrency: Currency): Observable<(Contract & { convertedAmount: number; rate: number })[]> {
    return this.getMyContractsAsFreelancer(freelancerId).pipe(
      switchMap(contracts => {
        if (contracts.length === 0) return [[]];
        const items = contracts.map(c => ({ amount: c.totalAmount, from: c.currency ?? Currency.EUR }));
        return this.currencyService.convertBatch(items, targetCurrency).pipe(
          map(conversions => contracts.map((contract, i) => ({
            ...contract,
            convertedAmount: conversions[i].convertedAmount,
            rate: conversions[i].rate
          })))
        );
      })
    );
  }

}