import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CurrencyExchangeService, ExchangeRateResponse } from './currency-exchange.service';
import { Currency } from '../../shared/models/contract.model';

describe('CurrencyExchangeService', () => {
  let service: CurrencyExchangeService;
  let httpMock: HttpTestingController;

  const mockResponse: ExchangeRateResponse = {
    result: 'success',
    base_code: 'EUR',
    time_last_update_utc: '2026-04-16T00:00:00Z',
    time_next_update_utc: '2026-04-17T00:00:00Z',
    conversion_rates: {
      EUR: 1,
      USD: 1.08,
      TND: 3.37,
      GBP: 0.86,
      CAD: 1.48
    }
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CurrencyExchangeService]
    });

    service = TestBed.inject(CurrencyExchangeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ===== Selected Currency =====

  describe('selectedCurrency', () => {
    it('should default to EUR', () => {
      expect(service.getSelectedCurrency()).toBe(Currency.EUR);
    });

    it('should update selected currency', () => {
      service.setSelectedCurrency(Currency.USD);
      expect(service.getSelectedCurrency()).toBe(Currency.USD);
    });

    it('should emit on selectedCurrency$ observable', (done) => {
      service.setSelectedCurrency(Currency.TND);
      service.selectedCurrency$.subscribe(currency => {
        expect(currency).toBe(Currency.TND);
        done();
      });
    });
  });

  // ===== Exchange Rates =====

  describe('getExchangeRates', () => {
    it('should fetch rates from API', () => {
      service.getExchangeRates(Currency.EUR).subscribe(response => {
        expect(response.base_code).toBe('EUR');
        expect(response.conversion_rates['USD']).toBe(1.08);
        expect(response.conversion_rates['TND']).toBe(3.37);
      });

      const req = httpMock.expectOne(r => r.url.includes('/EUR'));
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should cache rates and not call API again within cache period', () => {
      // First call
      service.getExchangeRates(Currency.EUR).subscribe();
      const req = httpMock.expectOne(r => r.url.includes('/EUR'));
      req.flush(mockResponse);

      // Second call (should use cache)
      service.getExchangeRates(Currency.EUR).subscribe(response => {
        expect(response.base_code).toBe('EUR');
      });

      httpMock.expectNone(r => r.url.includes('/EUR'));
    });

    it('should return fallback rates on API error', () => {
      service.getExchangeRates(Currency.EUR).subscribe(response => {
        expect(response.result).toBe('fallback');
        expect(response.conversion_rates['USD']).toBeDefined();
      });

      const req = httpMock.expectOne(r => r.url.includes('/EUR'));
      req.error(new ProgressEvent('Network error'));
    });
  });

  // ===== Convert =====

  describe('convert', () => {
    it('should return same amount when currencies are identical', () => {
      service.convert(1000, Currency.EUR, Currency.EUR).subscribe(result => {
        expect(result.convertedAmount).toBe(1000);
        expect(result.rate).toBe(1);
        expect(result.originalCurrency).toBe(Currency.EUR);
        expect(result.targetCurrency).toBe(Currency.EUR);
      });

      // No HTTP request expected
      httpMock.expectNone(r => true);
    });

    it('should convert EUR to USD', () => {
      service.convert(1000, Currency.EUR, Currency.USD).subscribe(result => {
        expect(result.originalAmount).toBe(1000);
        expect(result.convertedAmount).toBe(1080);
        expect(result.rate).toBe(1.08);
        expect(result.originalCurrency).toBe(Currency.EUR);
        expect(result.targetCurrency).toBe(Currency.USD);
      });

      const req = httpMock.expectOne(r => r.url.includes('/EUR'));
      req.flush(mockResponse);
    });

    it('should convert EUR to TND', () => {
      service.convert(500, Currency.EUR, Currency.TND).subscribe(result => {
        expect(result.convertedAmount).toBe(1685);
        expect(result.rate).toBe(3.37);
      });

      const req = httpMock.expectOne(r => r.url.includes('/EUR'));
      req.flush(mockResponse);
    });

    it('should convert EUR to GBP', () => {
      service.convert(1000, Currency.EUR, Currency.GBP).subscribe(result => {
        expect(result.convertedAmount).toBe(860);
        expect(result.rate).toBe(0.86);
      });

      const req = httpMock.expectOne(r => r.url.includes('/EUR'));
      req.flush(mockResponse);
    });

    it('should round to 2 decimal places', () => {
      const oddRateResponse = {
        ...mockResponse,
        conversion_rates: { ...mockResponse.conversion_rates, USD: 1.0753 }
      };

      service.convert(333, Currency.EUR, Currency.USD).subscribe(result => {
        // 333 * 1.0753 = 358.0749 → rounded to 358.07
        expect(result.convertedAmount).toBe(358.07);
      });

      const req = httpMock.expectOne(r => r.url.includes('/EUR'));
      req.flush(oddRateResponse);
    });
  });

  // ===== Batch Convert =====

  describe('convertBatch', () => {
    it('should return original amounts when all currencies match target', () => {
      const amounts = [
        { amount: 100, from: Currency.EUR },
        { amount: 200, from: Currency.EUR }
      ];

      service.convertBatch(amounts, Currency.EUR).subscribe(results => {
        expect(results.length).toBe(2);
        expect(results[0].convertedAmount).toBe(100);
        expect(results[1].convertedAmount).toBe(200);
        expect(results[0].rate).toBe(1);
      });

      httpMock.expectNone(r => true);
    });

    it('should convert multiple amounts to target currency', () => {
      const amounts = [
        { amount: 1000, from: Currency.EUR },
        { amount: 500, from: Currency.EUR }
      ];

      service.convertBatch(amounts, Currency.USD).subscribe(results => {
        expect(results.length).toBe(2);
        expect(results[0].convertedAmount).toBe(1080);
        expect(results[1].convertedAmount).toBe(540);
      });

      const req = httpMock.expectOne(r => r.url.includes('/EUR'));
      req.flush(mockResponse);
    });

    it('should handle empty array', () => {
      service.convertBatch([], Currency.USD).subscribe(results => {
        expect(results.length).toBe(0);
      });

      httpMock.expectNone(r => true);
    });
  });

  // ===== Supported Currencies =====

  describe('getSupportedCurrencies', () => {
    it('should return all supported currencies', () => {
      const currencies = service.getSupportedCurrencies();
      expect(currencies.length).toBe(5);
      expect(currencies.map(c => c.code)).toContain(Currency.EUR);
      expect(currencies.map(c => c.code)).toContain(Currency.USD);
      expect(currencies.map(c => c.code)).toContain(Currency.TND);
      expect(currencies.map(c => c.code)).toContain(Currency.GBP);
      expect(currencies.map(c => c.code)).toContain(Currency.CAD);
    });

    it('should have symbols for each currency', () => {
      const currencies = service.getSupportedCurrencies();
      currencies.forEach(c => {
        expect(c.symbol).toBeTruthy();
        expect(c.label).toBeTruthy();
      });
    });
  });

  describe('getCurrencySymbol', () => {
    it('should return € for EUR', () => {
      expect(service.getCurrencySymbol(Currency.EUR)).toBe('€');
    });

    it('should return $ for USD', () => {
      expect(service.getCurrencySymbol(Currency.USD)).toBe('$');
    });

    it('should return DT for TND', () => {
      expect(service.getCurrencySymbol(Currency.TND)).toBe('DT');
    });

    it('should return £ for GBP', () => {
      expect(service.getCurrencySymbol(Currency.GBP)).toBe('£');
    });

    it('should return CA$ for CAD', () => {
      expect(service.getCurrencySymbol(Currency.CAD)).toBe('CA$');
    });
  });
});
