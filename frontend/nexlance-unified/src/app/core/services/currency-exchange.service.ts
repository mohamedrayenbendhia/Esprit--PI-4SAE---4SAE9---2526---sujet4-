import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, shareReplay, of, catchError, BehaviorSubject } from 'rxjs';
import { Currency } from '../../shared/models/contract.model';

export interface ExchangeRateResponse {
  result: string;
  base_code: string;
  time_last_update_utc: string;
  time_next_update_utc: string;
  conversion_rates: { [currency: string]: number };
}

export interface ConvertedAmount {
  originalAmount: number;
  originalCurrency: Currency;
  convertedAmount: number;
  targetCurrency: Currency;
  rate: number;
  lastUpdated: string;
}

@Injectable({
  providedIn: 'root'
})
export class CurrencyExchangeService {

  // ExchangeRate-API (free tier: 1500 requests/month)
  private readonly API_URL = 'https://v6.exchangerate-api.com/v6/e0e6e99f2bce0e09ed5fc948/latest';

  private ratesCache = new Map<string, { rates: ExchangeRateResponse; timestamp: number }>();
  private readonly CACHE_DURATION = 10 * 60 * 1000; // 10 minutes

  private selectedCurrencySubject = new BehaviorSubject<Currency>(Currency.EUR);
  selectedCurrency$ = this.selectedCurrencySubject.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Get the user's preferred display currency
   */
  getSelectedCurrency(): Currency {
    return this.selectedCurrencySubject.value;
  }

  /**
   * Set the user's preferred display currency
   */
  setSelectedCurrency(currency: Currency): void {
    this.selectedCurrencySubject.next(currency);
  }

  /**
   * Fetch live exchange rates for a base currency
   */
  getExchangeRates(baseCurrency: Currency): Observable<ExchangeRateResponse> {
    const cached = this.ratesCache.get(baseCurrency);
    if (cached && Date.now() - cached.timestamp < this.CACHE_DURATION) {
      return of(cached.rates);
    }

    return this.http.get<ExchangeRateResponse>(`${this.API_URL}/${baseCurrency}`).pipe(
      map(response => {
        this.ratesCache.set(baseCurrency, { rates: response, timestamp: Date.now() });
        return response;
      }),
      catchError(() => {
        // Fallback static rates if API is unreachable
        return of(this.getFallbackRates(baseCurrency));
      }),
      shareReplay(1)
    );
  }

  /**
   * Convert an amount from one currency to another in real-time
   */
  convert(amount: number, from: Currency, to: Currency): Observable<ConvertedAmount> {
    if (from === to) {
      return of({
        originalAmount: amount,
        originalCurrency: from,
        convertedAmount: amount,
        targetCurrency: to,
        rate: 1,
        lastUpdated: new Date().toISOString()
      });
    }

    return this.getExchangeRates(from).pipe(
      map(response => {
        const rate = response.conversion_rates[to] ?? 1;
        return {
          originalAmount: amount,
          originalCurrency: from,
          convertedAmount: Math.round(amount * rate * 100) / 100,
          targetCurrency: to,
          rate,
          lastUpdated: response.time_last_update_utc
        };
      })
    );
  }

  /**
   * Convert multiple amounts at once (batch)
   */
  convertBatch(amounts: { amount: number; from: Currency }[], to: Currency): Observable<ConvertedAmount[]> {
    const uniqueFromCurrencies = [...new Set(amounts.map(a => a.from))];

    // If all same currency as target, return immediately
    if (uniqueFromCurrencies.every(c => c === to)) {
      return of(amounts.map(a => ({
        originalAmount: a.amount,
        originalCurrency: a.from,
        convertedAmount: a.amount,
        targetCurrency: to,
        rate: 1,
        lastUpdated: new Date().toISOString()
      })));
    }

    // Fetch rates for the first unique base and cross-convert
    const baseCurrency = uniqueFromCurrencies[0];
    return this.getExchangeRates(baseCurrency).pipe(
      map(response => {
        return amounts.map(a => {
          if (a.from === to) {
            return {
              originalAmount: a.amount,
              originalCurrency: a.from,
              convertedAmount: a.amount,
              targetCurrency: to,
              rate: 1,
              lastUpdated: response.time_last_update_utc
            };
          }

          // Cross rate: FROM -> BASE -> TO
          const fromToBase = a.from === baseCurrency ? 1 : (1 / (response.conversion_rates[a.from] ?? 1));
          const baseToTarget = response.conversion_rates[to] ?? 1;
          const rate = fromToBase * baseToTarget;

          return {
            originalAmount: a.amount,
            originalCurrency: a.from,
            convertedAmount: Math.round(a.amount * rate * 100) / 100,
            targetCurrency: to,
            rate: Math.round(rate * 10000) / 10000,
            lastUpdated: response.time_last_update_utc
          };
        });
      })
    );
  }

  /**
   * Get supported currencies for display
   */
  getSupportedCurrencies(): { code: Currency; label: string; symbol: string }[] {
    return [
      { code: Currency.EUR, label: 'Euro', symbol: '€' },
      { code: Currency.USD, label: 'US Dollar', symbol: '$' },
      { code: Currency.TND, label: 'Dinar Tunisien', symbol: 'DT' },
      { code: Currency.GBP, label: 'British Pound', symbol: '£' },
      { code: Currency.CAD, label: 'Canadian Dollar', symbol: 'CA$' }
    ];
  }

  /**
   * Get the symbol for a currency
   */
  getCurrencySymbol(currency: Currency): string {
    const found = this.getSupportedCurrencies().find(c => c.code === currency);
    return found?.symbol ?? currency;
  }

  /**
   * Fallback static rates when API is unavailable
   */
  private getFallbackRates(base: Currency): ExchangeRateResponse {
    const staticRates: { [key: string]: { [key: string]: number } } = {
      EUR: { EUR: 1, USD: 1.08, TND: 3.37, GBP: 0.86, CAD: 1.48 },
      USD: { EUR: 0.93, USD: 1, TND: 3.12, GBP: 0.79, CAD: 1.37 },
      TND: { EUR: 0.30, USD: 0.32, TND: 1, GBP: 0.25, CAD: 0.44 },
      GBP: { EUR: 1.16, USD: 1.26, TND: 3.92, GBP: 1, CAD: 1.72 },
      CAD: { EUR: 0.68, USD: 0.73, TND: 2.28, GBP: 0.58, CAD: 1 }
    };

    return {
      result: 'fallback',
      base_code: base,
      time_last_update_utc: new Date().toISOString(),
      time_next_update_utc: new Date().toISOString(),
      conversion_rates: staticRates[base] ?? staticRates['EUR']
    };
  }
}
