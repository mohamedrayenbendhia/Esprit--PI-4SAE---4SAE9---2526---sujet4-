import { Injectable, inject, OnDestroy } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { ComplaintService } from './complaint.service';
import { AuthService } from './auth.service';

/**
 * Service de ping d'activité.
 *
 * Rôle : signaler au backend que l'utilisateur est actif,
 * afin d'éviter l'envoi d'emails de "nouveau message" quand
 * il est déjà connecté (règle anti-spam).
 *
 * Comportement :
 *  - Ping toutes les PING_INTERVAL_MS (2 minutes)
 *  - Ping immédiat à chaque navigation vers une page "réclamations"
 *  - S'arrête si l'utilisateur se déconnecte
 *
 * Usage : injecter dans AppComponent ou un layout component (singleton).
 */
@Injectable({ providedIn: 'root' })
export class ActivityPingService implements OnDestroy {

  private readonly PING_INTERVAL_MS = 2 * 60 * 1000; // 2 minutes

  private complaintService = inject(ComplaintService);
  private authService      = inject(AuthService);
  private router           = inject(Router);

  private intervalId: ReturnType<typeof setInterval> | null = null;
  private routerSub: Subscription | null = null;
  private started = false;

  /** Démarrer le ping — appelé après login */
  start(): void {
    if (this.started) return;
    this.started = true;

    // Ping immédiat au démarrage
    this.ping();

    // Ping périodique toutes les 2 minutes
    this.intervalId = setInterval(() => this.ping(), this.PING_INTERVAL_MS);

    // Ping immédiat à chaque navigation vers une page réclamations
    this.routerSub = this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe((e: any) => {
      if (this.isComplaintsPage(e.urlAfterRedirects)) {
        this.ping();
      }
    });
  }

  /** Arrêter le ping — appelé au logout */
  stop(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
    this.routerSub?.unsubscribe();
    this.routerSub = null;
    this.started = false;
  }

  private ping(): void {
    if (!this.authService.isAuthenticated()) return;

    this.complaintService.pingActivity().subscribe({
      next: () => {},
      error: (err) => {
        if (err.status === 401) this.stop(); // token expiré → arrêt du ping
      }
    });
  }

  private isComplaintsPage(url: string): boolean {
    return url.includes('complaints') ||
           url.includes('agent/queue') ||
           url.includes('agent/my-assigned');
  }

  ngOnDestroy(): void {
    this.stop();
  }
}