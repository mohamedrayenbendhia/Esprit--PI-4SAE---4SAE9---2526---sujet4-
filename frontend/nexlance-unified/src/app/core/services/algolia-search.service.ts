import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, map, catchError } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AlgoliaSearchResult {
  hits: AlgoliaHit[];
  nbHits: number;
  page: number;
  nbPages: number;
  hitsPerPage: number;
  processingTimeMS: number;
}

export interface AlgoliaHit {
  objectID: string;
  title: string;
  description: string;
  category: string;
  budget: number;
  budgetType: string;
  requiredSkills: string[];
  experienceLevel: string;
  location: string;
  isRemote: boolean;
  status: string;
  _highlightResult?: any;
}

@Injectable({
  providedIn: 'root'
})
export class AlgoliaSearchService {
  private algoliaUrl: string;
  private appId: string;
  private searchApiKey: string;
  private indexName: string;

  constructor(private http: HttpClient) {
    this.appId = environment.algolia.appId;
    this.searchApiKey = environment.algolia.searchApiKey;
    this.indexName = environment.algolia.indexName;
    this.algoliaUrl = `https://${this.appId}-dsn.algolia.net/1/indexes/${this.indexName}`;
  }

  /**
   * Live search job offers via Algolia
   * Uses Algolia REST API directly (no SDK needed)
   */
  search(query: string, options: {
    page?: number;
    hitsPerPage?: number;
    filters?: string;
    facetFilters?: string[][];
  } = {}): Observable<AlgoliaSearchResult> {
    const body: any = {
      query,
      page: options.page || 0,
      hitsPerPage: options.hitsPerPage || 20,
      attributesToRetrieve: [
        'objectID', 'title', 'description', 'category', 'budget',
        'budgetType', 'requiredSkills', 'experienceLevel', 'location',
        'isRemote', 'status'
      ],
      attributesToHighlight: ['title', 'description', 'requiredSkills']
    };

    if (options.filters) {
      body.filters = options.filters;
    }
    if (options.facetFilters) {
      body.facetFilters = options.facetFilters;
    }

    return this.http.post<AlgoliaSearchResult>(
      `${this.algoliaUrl}/query`,
      body,
      {
        headers: {
          'X-Algolia-Application-Id': this.appId,
          'X-Algolia-API-Key': this.searchApiKey,
          'Content-Type': 'application/json'
        }
      }
    ).pipe(
      catchError(err => {
        console.warn('Algolia search failed, falling back to local search:', err);
        return of({ hits: [], nbHits: 0, page: 0, nbPages: 0, hitsPerPage: 20, processingTimeMS: 0 });
      })
    );
  }

  /**
   * Get search suggestions (autocomplete)
   */
  getSuggestions(query: string, limit: number = 5): Observable<AlgoliaHit[]> {
    return this.search(query, { hitsPerPage: limit }).pipe(
      map(result => result.hits)
    );
  }

  /**
   * Search with category filter
   */
  searchByCategory(query: string, category: string): Observable<AlgoliaSearchResult> {
    return this.search(query, {
      facetFilters: [['category:' + category]]
    });
  }

  /**
   * Search with multiple filters
   */
  searchWithFilters(query: string, filters: {
    category?: string;
    experienceLevel?: string;
    isRemote?: boolean;
    minBudget?: number;
    maxBudget?: number;
  }): Observable<AlgoliaSearchResult> {
    const facetFilters: string[][] = [];
    let numericFilters = '';

    if (filters.category) {
      facetFilters.push(['category:' + filters.category]);
    }
    if (filters.experienceLevel) {
      facetFilters.push(['experienceLevel:' + filters.experienceLevel]);
    }
    if (filters.isRemote !== undefined) {
      facetFilters.push(['isRemote:' + filters.isRemote]);
    }

    const numParts: string[] = [];
    if (filters.minBudget) {
      numParts.push(`budget >= ${filters.minBudget}`);
    }
    if (filters.maxBudget) {
      numParts.push(`budget <= ${filters.maxBudget}`);
    }
    numericFilters = numParts.join(' AND ');

    return this.search(query, {
      facetFilters: facetFilters.length > 0 ? facetFilters : undefined,
      filters: numericFilters || undefined
    });
  }
}
