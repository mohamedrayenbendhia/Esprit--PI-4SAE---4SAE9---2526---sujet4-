import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, timeout } from 'rxjs/operators';
import { ComplaintCategory, ComplaintPriority } from '../models/complaint.model';
import { environment } from '../../../environments/environment';

export interface AiSuggestion {
  category: ComplaintCategory;
  priority: ComplaintPriority;
  confidence: number;   // 0–1
  categoryLabel: string;
  priorityLabel: string;
}

/**
 * Service de classification automatique des réclamations.
 *
 * Le modèle Hugging Face (joeddav/xlm-roberta-large-xnli) est appelé via un
 * proxy Spring Boot (/api/complaints/ai/suggest) pour éviter les restrictions
 * CORS du navigateur.  Le token HF reste 100 % côté serveur.
 *
 * Dégradation gracieuse : retourne null si l'API est absente, lente (> 9s)
 * ou si le texte est trop court.
 */
@Injectable({ providedIn: 'root' })
export class ComplaintAiService {

  /** Proxy backend — passe par le Gateway */
  private readonly PROXY_URL = `${environment.complaintsApiUrl}/complaints/ai/suggest`;

  // Classification label → frontend enum mapping (must match AiProxyController.CANDIDATE_LABELS)
  private readonly CATEGORY_MAP: Record<string, ComplaintCategory> = {
    "le client refuse de payer ou le freelance n'a pas reçu sa rémunération après livraison":              ComplaintCategory.PAYMENT_ISSUE,
    "le travail livré par le prestataire est incomplet, incorrect ou ne respecte pas le cahier des charges": ComplaintCategory.QUALITY_DISPUTE,
    "l'interlocuteur ne répond plus aux messages et est totalement injoignable depuis plusieurs jours":      ComplaintCategory.COMMUNICATION_PROBLEM,
    "une personne harcèle, insulte ou envoie des messages menaçants de façon répétée":                      ComplaintCategory.HARASSMENT,
    "le freelance a reçu un acompte et a disparu sans livrer le moindre travail":                           ComplaintCategory.SCAM,
    "la plateforme elle-même a un bug ou une panne qui bloque l'accès à une fonctionnalité":                ComplaintCategory.TECHNICAL_ISSUE,
    "le litige ne correspond à aucune des catégories précédentes":                                          ComplaintCategory.OTHER
  };

  private readonly CATEGORY_LABELS: Record<ComplaintCategory, string> = {
    [ComplaintCategory.PAYMENT_ISSUE]:         'Payment issue',
    [ComplaintCategory.QUALITY_DISPUTE]:       'Quality dispute',
    [ComplaintCategory.COMMUNICATION_PROBLEM]: 'Communication problem',
    [ComplaintCategory.HARASSMENT]:            'Harassment',
    [ComplaintCategory.SCAM]:                  'Scam',
    [ComplaintCategory.TECHNICAL_ISSUE]:       'Technical issue',
    [ComplaintCategory.OTHER]:                 'Other'
  };

  // Keywords for heuristic priority estimation
  private readonly HIGH_PRIORITY_KEYWORDS = [
    'urgent', 'harassment', 'scam', 'fraud', 'threat', 'swindle',
    'blocked', 'impossible', 'critical', 'emergency', 'extortion'
  ];
  private readonly CRITICAL_PRIORITY_KEYWORDS = [
    'physical threat', 'violence', 'phishing', 'data theft', 'blackmail'
  ];

  constructor(private http: HttpClient) {}

  /**
   * Suggère une catégorie et une priorité à partir du sujet + description.
   * Appelle le proxy backend (pas HF directement) pour éviter le CORS.
   * Retourne null si le texte est trop court ou si le backend ne répond pas.
   */
  /** Vérifie si l'IA est activée côté serveur (token HF configuré). */
  checkStatus(): Observable<boolean> {
    return this.http
      .get<{ enabled: boolean }>(`${environment.complaintsApiUrl}/complaints/ai/status`)
      .pipe(
        map(r => r.enabled),
        catchError(() => of(false))
      );
  }

  suggest(subject: string, description: string): Observable<AiSuggestion | null> {
    const text = `${subject}. ${description}`.trim();
    if (text.length < 20) return of(null);

    return this.http.post<any>(this.PROXY_URL, { subject, description }).pipe(
      timeout(9000),
      map(response => this.mapResponse(response, text)),
      catchError(() => of(null))
    );
  }

  private mapResponse(response: any, text: string): AiSuggestion | null {
    // Le proxy retourne 204 No Content (body null) si désactivé ou modèle en chargement
    if (!response?.labels?.length || !response?.scores?.length) return null;

    const topLabel = response.labels[0] as string;
    const topScore = response.scores[0] as number;
    const category = this.CATEGORY_MAP[topLabel] ?? ComplaintCategory.OTHER;

    // Confiance insuffisante (< 14 % = en dessous du hasard sur 7 labels) → pas de suggestion
    if (topScore < 0.14) return null;

    const priority = this.inferPriority(text, category);

    return {
      category,
      priority,
      confidence: topScore,
      categoryLabel: this.CATEGORY_LABELS[category],
      priorityLabel: this.priorityLabel(priority)
    };
  }

  private inferPriority(text: string, category: ComplaintCategory): ComplaintPriority {
    const lower = text.toLowerCase();

    if (this.CRITICAL_PRIORITY_KEYWORDS.some(k => lower.includes(k))) {
      return ComplaintPriority.CRITICAL;
    }
    if (category === ComplaintCategory.HARASSMENT || category === ComplaintCategory.SCAM) {
      return ComplaintPriority.HIGH;
    }
    if (this.HIGH_PRIORITY_KEYWORDS.some(k => lower.includes(k))) {
      return ComplaintPriority.HIGH;
    }

    return ComplaintPriority.MEDIUM;
  }

  private priorityLabel(p: ComplaintPriority): string {
    const labels: Record<ComplaintPriority, string> = {
      [ComplaintPriority.LOW]:      'Low',
      [ComplaintPriority.MEDIUM]:   'Medium',
      [ComplaintPriority.HIGH]:     'High',
      [ComplaintPriority.CRITICAL]: 'Critical'
    };
    return labels[p];
  }
}
