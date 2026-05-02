import { Injectable, inject, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface ChatMessage {
  id?: string;
  projectId: string;
  senderId: string;
  senderName: string;
  content: string;
  type: 'TEXT' | 'FILE' | 'SYSTEM' | 'MILESTONE_UPDATE';
  attachmentUrl?: string;
  createdAt?: string;
  edited?: boolean;
}

/** Helper to build a chat message with required defaults */
export function buildChatMessage(partial: Omit<ChatMessage, 'edited'>): ChatMessage {
  return { ...partial, edited: false };
}

@Injectable({ providedIn: 'root' })
export class ChatService implements OnDestroy {
  private http = inject(HttpClient);
  private baseUrl = `${environment.projectsApiUrl || 'http://localhost:8080/api'}/chat`;
  private wsBaseUrl = environment.projectsWsUrl ? environment.projectsWsUrl.replace('/ws', '') : 'http://localhost:9091';

  private stompClient: Client | null = null;
  private messageSubject = new Subject<ChatMessage>();
  private connectedSubject = new BehaviorSubject<boolean>(false);

  messages$ = this.messageSubject.asObservable();
  connected$ = this.connectedSubject.asObservable();

  private subscribedProject: string | null = null;

  ngOnDestroy(): void {
    this.disconnect();
  }

  connect(): void {
    if (this.stompClient?.active) return;

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(`${this.wsBaseUrl}/ws`),
      reconnectDelay: 5000,
      onConnect: () => {
        this.connectedSubject.next(true);
        // Re-subscribe to existing project if any
        if (this.subscribedProject) {
          this.subscribeToProject(this.subscribedProject);
        }
      },
      onDisconnect: () => {
        this.connectedSubject.next(false);
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
        this.connectedSubject.next(false);
      }
    });

    this.stompClient.activate();
  }

  disconnect(): void {
    this.subscribedProject = null;
    this.stompClient?.deactivate();
    this.connectedSubject.next(false);
  }

  subscribeToProject(projectId: string): void {
    this.subscribedProject = projectId;
    
    // If client doesn't exist or isn't connected, initiate connection
    if (!this.stompClient?.connected) {
      if (!this.stompClient?.active) {
        this.connect();
      }
      // The subscription will happen in onConnect callback
      return;
    }

    // Only subscribe if we have an active and connected client
    try {
      this.stompClient.subscribe(`/topic/chat/${projectId}`, (message) => {
        const chatMessage: ChatMessage = JSON.parse(message.body);
        this.messageSubject.next(chatMessage);
      });
    } catch (error) {
      console.error('Error subscribing to project chat:', error);
      // Reconnect and try again
      this.connect();
    }
  }

  sendMessageWs(projectId: string, message: ChatMessage): void {
    if (this.stompClient?.connected) {
      try {
        this.stompClient.publish({
          destination: `/app/chat/${projectId}`,
          body: JSON.stringify(message)
        });
      } catch (error) {
        console.error('Error sending message via WebSocket:', error);
      }
    } else {
      console.warn('Cannot send message: WebSocket not connected');
    }
  }

  // REST fallback methods
  getProjectMessages(projectId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.baseUrl}/project/${projectId}`);
  }

  getRecentMessages(projectId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.baseUrl}/project/${projectId}/recent`);
  }

  postMessage(projectId: string, message: ChatMessage): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.baseUrl}/project/${projectId}`, message);
  }

  getMessageCount(projectId: string): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/project/${projectId}/count`);
  }
}
