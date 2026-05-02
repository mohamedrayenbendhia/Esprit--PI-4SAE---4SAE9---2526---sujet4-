import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class PosthogService {
  private apiKey: string;
  private apiHost: string;
  private initialized = false;
  private authService = inject(AuthService);
  private distinctId: string = '';

  constructor(private http: HttpClient) {
    this.apiKey = environment.posthog.apiKey;
    this.apiHost = environment.posthog.apiHost;
  }

  /**
   * Initialize PostHog with user identity
   */
  init(): void {
    if (this.initialized) return;

    const user = this.authService.getCurrentUser();
    this.distinctId = user?.id || 'anonymous-' + Math.random().toString(36).substring(7);

    // Identify the user
    if (user) {
      this.identify(user.id, {
        email: user.email,
        name: `${user.firstName} ${user.lastName}`,
        role: user.type
      });
    }

    this.initialized = true;
  }

  /**
   * Identify a user for analytics
   */
  identify(userId: string, properties: Record<string, any> = {}): void {
    this.distinctId = userId;
    this.sendEvent('$identify', {
      $set: properties
    });
  }

  /**
   * Track a custom event
   */
  capture(eventName: string, properties: Record<string, any> = {}): void {
    if (!this.initialized) this.init();
    this.sendEvent(eventName, properties);
  }

  /**
   * Track page views
   */
  trackPageView(pageName: string, properties: Record<string, any> = {}): void {
    this.capture('$pageview', {
      $current_url: window.location.href,
      page_name: pageName,
      ...properties
    });
  }

  // ---- Admin Dashboard Analytics Events ----

  /**
   * Track admin dashboard load with KPI data
   */
  trackDashboardView(stats: Record<string, any>): void {
    this.capture('admin_dashboard_viewed', {
      total_users: stats['totalUsers'],
      active_projects: stats['activeProjects'],
      total_job_offers: stats['totalJobOffers'],
      open_job_offers: stats['openJobOffers'],
      pending_kyc: stats['pendingKyc']
    });
  }

  /**
   * Track job offer events
   */
  trackJobOfferEvent(action: string, jobData: Record<string, any> = {}): void {
    this.capture('job_offer_' + action, {
      job_id: jobData['id'],
      category: jobData['category'],
      budget: jobData['budget'],
      experience_level: jobData['experienceLevel'],
      ...jobData
    });
  }

  /**
   * Track search events
   */
  trackSearch(query: string, resultCount: number, filters: Record<string, any> = {}): void {
    this.capture('search_performed', {
      query,
      result_count: resultCount,
      ...filters
    });
  }

  /**
   * Track application events
   */
  trackApplication(action: string, applicationData: Record<string, any> = {}): void {
    this.capture('application_' + action, applicationData);
  }

  /**
   * Track file upload events
   */
  trackFileUpload(context: string, fileInfo: Record<string, any> = {}): void {
    this.capture('file_uploaded', {
      upload_context: context,
      ...fileInfo
    });
  }

  /**
   * Track project milestone events
   */
  trackMilestone(action: string, milestoneData: Record<string, any> = {}): void {
    this.capture('milestone_' + action, milestoneData);
  }

  /**
   * Send event to PostHog via REST API (no SDK needed)
   */
  private sendEvent(eventName: string, properties: Record<string, any> = {}): void {
    const payload = {
      api_key: this.apiKey,
      event: eventName,
      properties: {
        distinct_id: this.distinctId,
        $lib: 'nexlance-angular',
        ...properties
      },
      timestamp: new Date().toISOString()
    };

    this.http.post(`${this.apiHost}/capture/`, payload, {
      headers: { 'Content-Type': 'application/json' }
    }).subscribe({
      error: (err) => console.warn('PostHog event failed:', eventName, err)
    });
  }
}
