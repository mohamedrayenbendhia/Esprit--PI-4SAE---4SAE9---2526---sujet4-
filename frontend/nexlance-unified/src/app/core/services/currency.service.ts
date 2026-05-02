import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, shareReplay, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ExchangeRates {
  base: string;
  rates: Record<string, number>;
  lastUpdated: string;
}

@Injectable({
  providedIn: 'root'
})
export class CurrencyService {
  private apiUrl = environment.exchangeRate.apiUrl;
  private apiKey = environment.exchangeRate.apiKey;
  private ratesCache$?: Observable<ExchangeRates>;
  private baseCurrency = 'TND'; // Tunisian Dinar as base

  constructor(private http: HttpClient) {}

  /**
   * Get exchange rates for TND (cached)
   */
  getRates(): Observable<ExchangeRates> {
    if (!this.ratesCache$) {
      this.ratesCache$ = this.http.get<any>(
        `${this.apiUrl}/${this.apiKey}/latest/${this.baseCurrency}`
      ).pipe(
        map(response => ({
          base: response.base_code,
          rates: response.conversion_rates,
          lastUpdated: response.time_last_update_utc
        })),
        shareReplay(1),
        catchError(() => of({
          base: 'TND',
          rates: { USD: 0.32, EUR: 0.29, GBP: 0.25 },
          lastUpdated: new Date().toISOString()
        }))
      );
    }
    return this.ratesCache$;
  }

  /**
   * Convert amount from TND to target currency
   */
  convert(amountTND: number, targetCurrency: string): Observable<number> {
    return this.getRates().pipe(
      map(rates => {
        const rate = rates.rates[targetCurrency];
        return rate ? Math.round(amountTND * rate * 100) / 100 : amountTND;
      })
    );
  }

  /**
   * Get formatted converted amounts for display
   */
  getConvertedDisplay(amountTND: number, currencies: string[] = ['USD', 'EUR', 'GBP']): Observable<{ currency: string; amount: number; symbol: string }[]> {
    const symbols: Record<string, string> = {
      USD: '$', EUR: '€', GBP: '£', TND: 'DT',
      CAD: 'C$', AUD: 'A$', CHF: 'CHF', JPY: '¥'
    };

    return this.getRates().pipe(
      map(rates => currencies.map(currency => ({
        currency,
        amount: Math.round((amountTND * (rates.rates[currency] || 1)) * 100) / 100,
        symbol: symbols[currency] || currency
      })))
    );
  }
}
