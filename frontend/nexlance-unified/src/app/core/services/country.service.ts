import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, shareReplay, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Country {
  name: string;
  code: string;
  flag: string;
  region: string;
  currencies: string[];
}

@Injectable({
  providedIn: 'root'
})
export class CountryService {
  private apiUrl = environment.restCountriesUrl;
  private countriesCache$?: Observable<Country[]>;

  constructor(private http: HttpClient) {}

  /**
   * Get all countries (cached)
   * Used for location autocomplete in job offer creation & search
   */
  getAllCountries(): Observable<Country[]> {
    if (!this.countriesCache$) {
      this.countriesCache$ = this.http.get<any[]>(
        `${this.apiUrl}/all?fields=name,cca2,flags,region,currencies`
      ).pipe(
        map(countries => countries.map(c => ({
          name: c.name?.common || '',
          code: c.cca2 || '',
          flag: c.flags?.svg || c.flags?.png || '',
          region: c.region || '',
          currencies: c.currencies ? Object.keys(c.currencies) : []
        })).sort((a, b) => a.name.localeCompare(b.name))),
        shareReplay(1),
        catchError(() => of([]))
      );
    }
    return this.countriesCache$;
  }

  /**
   * Search countries by name (for autocomplete)
   */
  searchCountries(query: string): Observable<Country[]> {
    if (!query || query.length < 2) return of([]);

    return this.getAllCountries().pipe(
      map(countries => countries.filter(c =>
        c.name.toLowerCase().includes(query.toLowerCase()) ||
        c.code.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 10))
    );
  }

  /**
   * Get countries by region
   */
  getCountriesByRegion(region: string): Observable<Country[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/region/${encodeURIComponent(region)}?fields=name,cca2,flags`
    ).pipe(
      map(countries => countries.map(c => ({
        name: c.name?.common || '',
        code: c.cca2 || '',
        flag: c.flags?.svg || '',
        region: region,
        currencies: []
      }))),
      catchError(() => of([]))
    );
  }

  /**
   * Get country details by code
   */
  getCountryByCode(code: string): Observable<Country | null> {
    return this.http.get<any[]>(
      `${this.apiUrl}/alpha/${encodeURIComponent(code)}?fields=name,cca2,flags,region,currencies`
    ).pipe(
      map(data => {
        const c = Array.isArray(data) ? data[0] : data;
        return {
          name: c.name?.common || '',
          code: c.cca2 || '',
          flag: c.flags?.svg || '',
          region: c.region || '',
          currencies: c.currencies ? Object.keys(c.currencies) : []
        };
      }),
      catchError(() => of(null))
    );
  }
}
