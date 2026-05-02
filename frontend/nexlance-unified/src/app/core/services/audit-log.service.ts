import { Injectable, inject, NgZone } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';

export interface AuditLog {
  id: number;
  action: string;
  entityType: string;
  entityId: string;
  userId: string;
  userRole: string;
  details: string;
  oldValue: string;
  newValue: string;
  ipAddress: string;
  timestamp: string;
}

export interface AuditStats {
  totalLogs: number;
  todayLogs: number;
  topActions: { [key: string]: number };
  topEntityTypes: { [key: string]: number };
}

@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private http = inject(HttpClient);
  private ngZone = inject(NgZone);
  private baseUrl = `${environment.jobOffersApiUrl}/audit-logs`;

  private stompClient: Client | null = null;
  private connected = false;

  /** Emits each new audit log received via WebSocket in real-time */
  private _newLog$ = new Subject<AuditLog>();
  newLog$ = this._newLog$.asObservable();

  /** Connect to WebSocket and subscribe to /topic/audit-logs for real-time updates */
  connectWebSocket(): void {
    if (this.connected || this.stompClient) return;

    const wsUrl = environment.jobOffersWsUrl;
    console.log('[AuditLog WS] Connecting to', wsUrl);

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      debug: (msg) => console.debug('[AuditLog WS]', msg),
      onConnect: () => {
        console.log('[AuditLog WS] Connected');
        this.connected = true;
        this.stompClient!.subscribe('/topic/audit-logs', (message) => {
          try {
            const log: AuditLog = JSON.parse(message.body);
            console.log('[AuditLog WS] New log received:', log.action, log.entityType);
            this.ngZone.run(() => this._newLog$.next(log));
          } catch (e) {
            console.error('[AuditLog WS] Parse error:', e);
          }
        });
      },
      onStompError: (frame) => {
        console.error('[AuditLog WS] STOMP error:', frame.headers['message']);
      },
      onDisconnect: () => {
        console.log('[AuditLog WS] Disconnected');
        this.connected = false;
      }
    });

    this.stompClient.activate();
  }

  /** Disconnect WebSocket */
  disconnectWebSocket(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
      this.connected = false;
    }
  }

  getRecentLogs(): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(this.baseUrl);
  }

  getEntityAuditTrail(entityType: string, entityId: string): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.baseUrl}/entity/${entityType}/${entityId}`);
  }

  getUserAuditTrail(userId: string): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.baseUrl}/user/${userId}`);
  }

  getLogsByEntityType(entityType: string): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.baseUrl}/type/${entityType}`);
  }

  getLogsByDateRange(start: string, end: string): Observable<AuditLog[]> {
    const params = new HttpParams().set('start', start).set('end', end);
    return this.http.get<AuditLog[]>(`${this.baseUrl}/range`, { params });
  }

  getAuditStats(): Observable<AuditStats> {
    return this.http.get<AuditStats>(`${this.baseUrl}/stats`);
  }
}
