import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';
import { AppNotification } from '../models/notification.model';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {

  // REST → via Gateway (8765) ; WebSocket → direct 9090 (STOMP ne passe pas par HTTP Gateway)
  private apiUrl = `${environment.notificationsApiUrl}/notifications`;
  private wsUrl  = environment.notificationsWsUrl;

  private stompClient: Client | null = null;
  private connected = false;
  private currentUserId: string | null = null;

  // ── Streams publics ──────────────────────────────────────
  private notificationsSubject  = new BehaviorSubject<AppNotification[]>([]);
  private unreadCountSubject    = new BehaviorSubject<number>(0);

  /** Emitted on each real-time notification → triggers the toast */
  private newNotificationSubject = new Subject<AppNotification>();

  notifications$      = this.notificationsSubject.asObservable();
  unreadCount$        = this.unreadCountSubject.asObservable();
  newNotification$    = this.newNotificationSubject.asObservable();

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  // =========================================================
  // CONNEXION WEBSOCKET
  // =========================================================

  connect(): void {
    const user = this.authService.getCurrentUser();
    if (!user?.id) return;

    this.currentUserId = user.id;
    this.loadNotifications(user.id);
    this.loadUnreadCount(user.id);

    if (this.connected && this.stompClient?.active) return;
    this.disconnectWs();

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(this.wsUrl) as any,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: str => { if (!environment.production) console.log('[WS]', str); }
    });

    this.stompClient.onConnect = () => {
      this.connected = true;
      if (!environment.production) console.log('[WS] Connected to notification service');

      this.stompClient?.subscribe(
        `/topic/notifications/${user.id}`,
        (msg: IMessage) => {
          const notif: AppNotification = JSON.parse(msg.body);
          this.onNewNotification(notif);
        }
      );
    };

    this.stompClient.onStompError = (frame) => {
      this.connected = false;
      console.warn('[WS] STOMP error:', frame.headers['message']);
    };

    this.stompClient.onWebSocketClose = () => {
      this.connected = false;
      // STOMP client handles reconnection via reconnectDelay
    };

    try { this.stompClient.activate(); } catch (err) {
      console.warn('[WS] Failed to activate STOMP client:', err);
    }
  }

  private onNewNotification(notif: AppNotification): void {
    // Ajouter en tête de liste
    const current = this.notificationsSubject.getValue();
    this.notificationsSubject.next([notif, ...current]);
    this.unreadCountSubject.next(this.unreadCountSubject.getValue() + 1);

    // Émettre pour le toast
    this.newNotificationSubject.next(notif);
  }

  // =========================================================
  // REST
  // =========================================================

  /**
   * Side-effect method: fetches the notification list and pushes results
   * directly into `notificationsSubject` (updates all subscribers of `notifications$`).
   * Use this for the initial load on connect() and after key actions.
   * @see getNotifications for the Observable variant (used when you need to react to the result).
   */
  /**
   * Side-effect method: fetches the notification list and pushes results
   * directly into `notificationsSubject` (updates all subscribers of `notifications$`).
   * Used by connect() for initial load.
   */
  loadNotifications(userId: string): void {
    // The backend returns Page<T> — extract the .content array
    this.http.get<{ content: AppNotification[] }>(`${this.apiUrl}/user/${userId}?page=0&size=20`).subscribe({
      next: page => this.notificationsSubject.next(page.content ?? []),
      error: err => {
        if (!environment.production) console.error('[Notifications] load error:', err);
      }
    });
  }

  loadUnreadCount(userId: string): void {
    this.http.get<{ count: number }>(`${this.apiUrl}/user/${userId}/unread-count`).subscribe({
      next: r => this.unreadCountSubject.next(r.count),
      error: () => {}
    });
  }

  /**
   * Marks one notification as read AND synchronises `notificationsSubject`
   * and `unreadCountSubject` so the bell and dropdown update immediately.
   * Preferred over a raw HTTP call in all UI scenarios.
   */
  markAsReadAndUpdate(notif: AppNotification): void {
    if (notif.read || !notif.id) return;
    this.http.put<AppNotification>(`${this.apiUrl}/${notif.id}/read`, {}).subscribe({
      next: updated => {
        const list = this.notificationsSubject.getValue()
          .map(n => n.id === updated.id ? { ...n, read: true } : n);
        this.notificationsSubject.next(list);
        this.unreadCountSubject.next(Math.max(0, this.unreadCountSubject.getValue() - 1));
      },
      error: err => {
        if (!environment.production) console.warn('[Notifications] mark read error:', err);
      }
    });
  }

  /**
   * Marks every notification for the current user as read and syncs the local streams.
   * Returns the raw Observable so callers can chain UI feedback (snackbar, toast, etc.).
   */
  markAllAsRead(userId: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/user/${userId}/read-all`, {});
  }

  /**
   * Fire-and-forget notification creation. Used by project-chat to notify the other party
   * of a new chat message. Errors are silenced in production.
   */
  createNotification(
    recipientId: string, type: string, title: string,
    message: string, referenceId?: string, referenceType?: string
  ): void {
    this.http.post<AppNotification>(this.apiUrl, {
      recipientId, type, title, message,
      referenceId: referenceId || '',
      referenceType: referenceType || ''
    }).subscribe({
      error: err => {
        if (!environment.production) console.warn('[Notifications] create error:', err);
      }
    });
  }

  // =========================================================
  // HELPERS
  // =========================================================

  private disconnectWs(): void {
    try { this.stompClient?.deactivate(); } catch {}
    this.stompClient = null;
    this.connected = false;
  }

  disconnect(): void {
    this.disconnectWs();
    this.currentUserId = null;
  }

  ngOnDestroy(): void { this.disconnect(); }
}