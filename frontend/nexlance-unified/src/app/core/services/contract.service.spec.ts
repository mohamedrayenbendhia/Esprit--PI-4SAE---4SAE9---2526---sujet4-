import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ContractService } from './contract.service';
import { CurrencyExchangeService } from './currency-exchange.service';
import {
  Contract,
  ContractType,
  ContractStatus,
  Currency,
  PaymentSchedule,
  FreelancerEarnings
} from '../../shared/models/contract.model';
import { environment } from '../../../environments/environment';

describe('ContractService', () => {
  let service: ContractService;
  let httpMock: HttpTestingController;

  const clientUrl = `${environment.contractApiUrl}/api/client/contracts`;
  const freelancerUrl = `${environment.contractApiUrl}/api/freelancer/contracts`;

  const mockContract: Contract = {
    id: 1,
    contractNumber: 'CTR-001',
    contractType: ContractType.STANDARD,
    clientId: 'client-1',
    freelancerId: 'freelancer-1',
    missionTitle: 'Web App Development',
    totalAmount: 5000,
    currency: Currency.EUR,
    status: ContractStatus.ACTIVE
  };

  const mockExchangeResponse = {
    result: 'success',
    base_code: 'EUR',
    time_last_update_utc: '2026-04-16T00:00:00Z',
    time_next_update_utc: '2026-04-17T00:00:00Z',
    conversion_rates: { EUR: 1, USD: 1.08, TND: 3.37, GBP: 0.86, CAD: 1.48 }
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ContractService, CurrencyExchangeService]
    });

    service = TestBed.inject(ContractService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ===== CLIENT - CONTRATS =====

  describe('Client - Contracts CRUD', () => {
    it('createContract should POST a new contract', () => {
      service.createContract(mockContract).subscribe(result => {
        expect(result.id).toBe(1);
      });

      const req = httpMock.expectOne(clientUrl);
      expect(req.request.method).toBe('POST');
      req.flush(mockContract);
    });

    it('getMyContractsAsClient should GET with clientId param', () => {
      service.getMyContractsAsClient('client-1').subscribe(contracts => {
        expect(contracts.length).toBe(1);
      });

      const req = httpMock.expectOne(r => r.url === `${clientUrl}/my-contracts`);
      expect(req.request.params.get('clientId')).toBe('client-1');
      req.flush([mockContract]);
    });

    it('getContractById should GET by id', () => {
      service.getContractById(1).subscribe(c => {
        expect(c.contractNumber).toBe('CTR-001');
      });

      const req = httpMock.expectOne(`${clientUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockContract);
    });

    it('updateContract should PUT', () => {
      service.updateContract(1, { ...mockContract, missionTitle: 'Updated' }).subscribe();

      const req = httpMock.expectOne(`${clientUrl}/1`);
      expect(req.request.method).toBe('PUT');
      req.flush(mockContract);
    });

    it('deleteContract should DELETE', () => {
      service.deleteContract(1).subscribe();

      const req = httpMock.expectOne(`${clientUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('searchContractsAsClient should GET with params', () => {
      service.searchContractsAsClient('client-1', ContractStatus.ACTIVE, 'web').subscribe();

      const req = httpMock.expectOne(r => r.url === `${clientUrl}/search`);
      expect(req.request.params.get('clientId')).toBe('client-1');
      expect(req.request.params.get('status')).toBe('ACTIVE');
      expect(req.request.params.get('keyword')).toBe('web');
      req.flush([mockContract]);
    });

    it('getClientStatistics should GET with clientId', () => {
      service.getClientStatistics('client-1').subscribe();

      const req = httpMock.expectOne(r => r.url === `${clientUrl}/statistics`);
      expect(req.request.params.get('clientId')).toBe('client-1');
      req.flush({ totalContracts: 5, draftContracts: 1, activeContracts: 2, completedContracts: 2, pendingSignature: 0 });
    });
  });

  // ===== CLIENT - WORKFLOW =====

  describe('Client - Workflow', () => {
    it('signByClient should POST with signatureHash', () => {
      service.signByClient(1, 'hash123').subscribe();

      const req = httpMock.expectOne(`${clientUrl}/1/sign`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.signatureHash).toBe('hash123');
      req.flush(mockContract);
    });

    it('sendToFreelancer should POST', () => {
      service.sendToFreelancer(1).subscribe();

      const req = httpMock.expectOne(`${clientUrl}/1/send-to-freelancer`);
      expect(req.request.method).toBe('POST');
      req.flush(mockContract);
    });

    it('activateContract should POST', () => {
      service.activateContract(1).subscribe();

      const req = httpMock.expectOne(`${clientUrl}/1/activate`);
      expect(req.request.method).toBe('POST');
      req.flush(mockContract);
    });

    it('completeContract should POST', () => {
      service.completeContract(1).subscribe();

      const req = httpMock.expectOne(`${clientUrl}/1/complete`);
      expect(req.request.method).toBe('POST');
      req.flush(mockContract);
    });

    it('cancelContract should POST', () => {
      service.cancelContract(1).subscribe();

      const req = httpMock.expectOne(`${clientUrl}/1/cancel`);
      expect(req.request.method).toBe('POST');
      req.flush(mockContract);
    });
  });

  // ===== FREELANCER =====

  describe('Freelancer - Contracts', () => {
    it('getMyContractsAsFreelancer should GET with freelancerId', () => {
      service.getMyContractsAsFreelancer('freelancer-1').subscribe();

      const req = httpMock.expectOne(r => r.url === `${freelancerUrl}/my-contracts`);
      expect(req.request.params.get('freelancerId')).toBe('freelancer-1');
      req.flush([mockContract]);
    });

    it('acceptContract should POST', () => {
      service.acceptContract(1).subscribe();

      const req = httpMock.expectOne(`${freelancerUrl}/1/accept`);
      expect(req.request.method).toBe('POST');
      req.flush(mockContract);
    });

    it('declineContract should POST with reason param', () => {
      service.declineContract(1, 'Too busy').subscribe();

      const req = httpMock.expectOne(r => r.url === `${freelancerUrl}/1/decline`);
      expect(req.request.params.get('reason')).toBe('Too busy');
      req.flush(mockContract);
    });

    it('getFreelancerEarnings should GET earnings', () => {
      const earnings: FreelancerEarnings = { totalEarnings: 10000, pendingEarnings: 3000, completedContracts: 5 };

      service.getFreelancerEarnings('freelancer-1').subscribe(result => {
        expect(result.totalEarnings).toBe(10000);
      });

      const req = httpMock.expectOne(r => r.url === `${freelancerUrl}/earnings`);
      req.flush(earnings);
    });
  });

  // ===== CONVERSION DE MONNAIE =====

  describe('Currency Conversion - Contract Amounts', () => {
    it('convertContractAmount should convert total amount to target currency', () => {
      service.convertContractAmount(mockContract, Currency.USD).subscribe(result => {
        expect(result.originalAmount).toBe(5000);
        expect(result.convertedAmount).toBe(5400);
        expect(result.rate).toBe(1.08);
        expect(result.originalCurrency).toBe(Currency.EUR);
        expect(result.targetCurrency).toBe(Currency.USD);
      });

      const req = httpMock.expectOne(r => r.url.includes('exchangerate'));
      req.flush(mockExchangeResponse);
    });

    it('convertContractAmount should return same amount for same currency', () => {
      service.convertContractAmount(mockContract, Currency.EUR).subscribe(result => {
        expect(result.convertedAmount).toBe(5000);
        expect(result.rate).toBe(1);
      });

      // No exchange API call expected
    });

    it('getContractWithConversion should return contract with conversion data', () => {
      service.getContractWithConversion(1, Currency.TND).subscribe(result => {
        expect(result.id).toBe(1);
        expect(result.totalAmount).toBe(5000);
        expect(result.conversion.convertedAmount).toBe(16850);
        expect(result.conversion.rate).toBe(3.37);
      });

      // First: contract fetch
      const contractReq = httpMock.expectOne(`${clientUrl}/1`);
      contractReq.flush(mockContract);

      // Then: exchange rate fetch
      const rateReq = httpMock.expectOne(r => r.url.includes('exchangerate'));
      rateReq.flush(mockExchangeResponse);
    });

    it('getContractWithConversionAsFreelancer should work for freelancer view', () => {
      service.getContractWithConversionAsFreelancer(1, Currency.USD).subscribe(result => {
        expect(result.id).toBe(1);
        expect(result.conversion.convertedAmount).toBe(5400);
      });

      const contractReq = httpMock.expectOne(`${freelancerUrl}/1`);
      contractReq.flush(mockContract);

      const rateReq = httpMock.expectOne(r => r.url.includes('exchangerate'));
      rateReq.flush(mockExchangeResponse);
    });
  });

  describe('Currency Conversion - Payments', () => {
    it('getPaymentsWithConversion should convert payment amounts', () => {
      const payments: PaymentSchedule[] = [
        { label: 'Phase 1', amount: 2000, dueDate: '2026-06-01' },
        { label: 'Phase 2', amount: 3000, dueDate: '2026-07-01' }
      ];

      service.getPaymentsWithConversion(1, Currency.USD).subscribe(results => {
        expect(results.length).toBe(2);
        expect(results[0].convertedAmount).toBe(2160);
        expect(results[1].convertedAmount).toBe(3240);
      });

      // Payments fetch
      const paymentsReq = httpMock.expectOne(`${clientUrl}/1/payments`);
      paymentsReq.flush(payments);

      // Contract fetch (to get currency)
      const contractReq = httpMock.expectOne(`${clientUrl}/1`);
      contractReq.flush(mockContract);

      // Exchange rate fetch
      const rateReq = httpMock.expectOne(r => r.url.includes('exchangerate'));
      rateReq.flush(mockExchangeResponse);
    });
  });

  describe('Currency Conversion - Earnings', () => {
    it('getFreelancerEarningsWithConversion should convert earnings', () => {
      const earnings: FreelancerEarnings = { totalEarnings: 10000, pendingEarnings: 3000, completedContracts: 5 };

      service.getFreelancerEarningsWithConversion('freelancer-1', Currency.EUR, Currency.TND).subscribe(result => {
        expect(result.convertedTotalEarnings).toBe(33700);
        expect(result.convertedPendingEarnings).toBe(10110);
        expect(result.rate).toBe(3.37);
        expect(result.completedContracts).toBe(5);
      });

      // Earnings fetch
      const earningsReq = httpMock.expectOne(r => r.url === `${freelancerUrl}/earnings`);
      earningsReq.flush(earnings);

      // Two exchange rate calls (total + pending) - first uses API, second uses cache
      const rateReq = httpMock.expectOne(r => r.url.includes('exchangerate'));
      rateReq.flush(mockExchangeResponse);
    });
  });

  describe('Currency Conversion - Contract Lists', () => {
    it('getMyContractsWithConversion should convert all contract amounts', () => {
      const contracts = [
        { ...mockContract, id: 1, totalAmount: 5000 },
        { ...mockContract, id: 2, totalAmount: 3000 }
      ];

      service.getMyContractsWithConversion('client-1', Currency.USD).subscribe(results => {
        expect(results.length).toBe(2);
        expect(results[0].convertedAmount).toBe(5400);
        expect(results[1].convertedAmount).toBe(3240);
      });

      const contractsReq = httpMock.expectOne(r => r.url === `${clientUrl}/my-contracts`);
      contractsReq.flush(contracts);

      const rateReq = httpMock.expectOne(r => r.url.includes('exchangerate'));
      rateReq.flush(mockExchangeResponse);
    });

    it('getMyContractsAsFreelancerWithConversion should convert freelancer contracts', () => {
      service.getMyContractsAsFreelancerWithConversion('freelancer-1', Currency.GBP).subscribe(results => {
        expect(results.length).toBe(1);
        expect(results[0].convertedAmount).toBe(4300);
      });

      const contractsReq = httpMock.expectOne(r => r.url === `${freelancerUrl}/my-contracts`);
      contractsReq.flush([mockContract]);

      const rateReq = httpMock.expectOne(r => r.url.includes('exchangerate'));
      rateReq.flush(mockExchangeResponse);
    });
  });

  // ===== PDF =====

  describe('generatePdf', () => {
    it('should GET PDF as blob', () => {
      service.generatePdf(1).subscribe(blob => {
        expect(blob).toBeTruthy();
      });

      const req = httpMock.expectOne(`${clientUrl}/1/pdf`);
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(new Blob(['pdf content']));
    });
  });
});
