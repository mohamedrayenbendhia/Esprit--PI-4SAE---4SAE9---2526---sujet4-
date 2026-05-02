import { Component, Input, OnInit, OnDestroy, inject, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { ChatService, ChatMessage } from '../../../core/services/chat.service';

@Component({
  selector: 'app-project-chat',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatBadgeModule,
    MatProgressSpinnerModule,
    TranslateModule
  ],
  template: `
    <mat-card class="chat-container" [class.expanded]="isExpanded">
      <!-- Chat Header -->
      <div class="chat-header" (click)="toggleChat()">
        <div class="header-left">
          <mat-icon>chat</mat-icon>
          <span class="header-title">Project Chat</span>
          <span class="unread-badge" *ngIf="unreadCount() > 0">{{ unreadCount() }}</span>
        </div>
        <div class="header-right">
          <span class="connection-status" [class.connected]="isConnected()">
            <mat-icon class="status-dot">circle</mat-icon>
            {{ isConnected() ? 'Connected' : 'Offline' }}
          </span>
          <mat-icon class="toggle-icon">{{ isExpanded ? 'expand_more' : 'expand_less' }}</mat-icon>
        </div>
      </div>

      <!-- Chat Body -->
      <div class="chat-body" *ngIf="isExpanded">
        <!-- Loading -->
        <div *ngIf="loading()" class="chat-loading">
          <mat-spinner diameter="24"></mat-spinner>
        </div>

        <!-- Messages -->
        <div class="messages-container" #messagesContainer>
          <div *ngIf="messages().length === 0 && !loading()" class="no-messages">
            <mat-icon>chat_bubble_outline</mat-icon>
            <p>No messages yet. Start the conversation!</p>
          </div>

          <div *ngFor="let msg of messages()" 
               class="message-wrapper"
               [class.own-message]="msg.senderId === currentUserId"
               [class.system-message]="msg.type === 'SYSTEM' || msg.type === 'MILESTONE_UPDATE'">
            
            <!-- System message -->
            <div *ngIf="msg.type === 'SYSTEM' || msg.type === 'MILESTONE_UPDATE'" class="system-msg">
              <mat-icon class="sys-icon">{{ msg.type === 'MILESTONE_UPDATE' ? 'flag' : 'info' }}</mat-icon>
              {{ msg.content }}
            </div>

            <!-- User message -->
            <div *ngIf="msg.type === 'TEXT' || msg.type === 'FILE'" class="user-msg">
              <div class="msg-header">
                <span class="sender-name">{{ msg.senderName }}</span>
                <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
              </div>
              <div class="msg-content">{{ msg.content }}</div>
              <a *ngIf="msg.attachmentUrl" [href]="msg.attachmentUrl" target="_blank" class="attachment-link">
                <mat-icon>attach_file</mat-icon> Attachment
              </a>
            </div>
          </div>
        </div>

        <!-- Input -->
        <div class="chat-input">
          <input
            type="text"
            [(ngModel)]="newMessage"
            (keyup.enter)="sendMessage()"
            placeholder="Type a message..."
            class="message-input"
            [disabled]="!isConnected()"
          />
          <button mat-icon-button 
                  color="primary" 
                  (click)="sendMessage()" 
                  [disabled]="!newMessage.trim() || !isConnected()">
            <mat-icon>send</mat-icon>
          </button>
        </div>
      </div>
    </mat-card>
  `,
  styles: [`
    .chat-container {
      position: fixed;
      bottom: 20px;
      right: 20px;
      width: 380px;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 8px 30px rgba(0,0,0,0.15);
      z-index: 1000;
      transition: all 0.3s ease;

      &.expanded {
        height: 480px;
        display: flex;
        flex-direction: column;
      }
    }

    .chat-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      background: linear-gradient(135deg, #0EA5E9, #0284C7);
      color: white;
      cursor: pointer;
      user-select: none;

      .header-left {
        display: flex;
        align-items: center;
        gap: 8px;

        .header-title { font-weight: 600; font-size: 0.875rem; }

        .unread-badge {
          background: #EF4444;
          color: white;
          border-radius: 10px;
          padding: 2px 6px;
          font-size: 0.688rem;
          font-weight: 700;
          min-width: 18px;
          text-align: center;
        }
      }

      .header-right {
        display: flex;
        align-items: center;
        gap: 8px;

        .connection-status {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 0.688rem;
          opacity: 0.8;

          .status-dot { font-size: 8px; width: 8px; height: 8px; color: #EF4444; }
          &.connected .status-dot { color: #10B981; }
        }

        .toggle-icon { transition: transform 0.3s; }
      }
    }

    .chat-body {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    .chat-loading {
      display: flex;
      justify-content: center;
      padding: 24px;
    }

    .messages-container {
      flex: 1;
      overflow-y: auto;
      padding: 12px;
      display: flex;
      flex-direction: column;
      gap: 8px;

      .no-messages {
        text-align: center;
        padding: 40px 20px;
        color: #94a3b8;
        mat-icon { font-size: 40px; width: 40px; height: 40px; opacity: 0.5; }
        p { margin-top: 8px; font-size: 0.875rem; }
      }
    }

    .message-wrapper {
      &.own-message {
        align-self: flex-end;
        .user-msg {
          background: #0EA5E9;
          color: white;
          border-radius: 12px 12px 4px 12px;
          .msg-header .msg-time { color: rgba(255,255,255,0.7); }
          .msg-header .sender-name { color: rgba(255,255,255,0.9); }
        }
      }

      &:not(.own-message):not(.system-message) {
        align-self: flex-start;
        .user-msg {
          background: #f1f5f9;
          border-radius: 12px 12px 12px 4px;
        }
      }
    }

    .user-msg {
      max-width: 280px;
      padding: 8px 12px;

      .msg-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 4px;

        .sender-name { font-size: 0.688rem; font-weight: 600; color: #0EA5E9; }
        .msg-time { font-size: 0.625rem; color: #94a3b8; }
      }

      .msg-content {
        font-size: 0.875rem;
        line-height: 1.4;
        word-break: break-word;
      }

      .attachment-link {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 0.75rem;
        margin-top: 4px;
        color: inherit;
        opacity: 0.8;
        text-decoration: none;
        &:hover { opacity: 1; text-decoration: underline; }
        mat-icon { font-size: 14px; width: 14px; height: 14px; }
      }
    }

    .system-msg {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      background: #FEF3C7;
      border-radius: 8px;
      font-size: 0.75rem;
      color: #92400E;
      align-self: center;
      text-align: center;

      .sys-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }
    }

    .chat-input {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      border-top: 1px solid #e2e8f0;
      background: white;

      .message-input {
        flex: 1;
        border: 1px solid #e2e8f0;
        border-radius: 20px;
        padding: 8px 16px;
        font-size: 0.875rem;
        outline: none;
        transition: border-color 0.2s;

        &:focus { border-color: #0EA5E9; }
        &:disabled { opacity: 0.5; }
      }
    }
  `]
})
export class ProjectChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  @Input() projectId!: string;
  @Input() currentUserId!: string;
  @Input() currentUserName!: string;
  @Input() recipientId?: string;

  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  private chatService = inject(ChatService);
  private messageSubscription?: Subscription;
  private connectionSubscription?: Subscription;

  messages = signal<ChatMessage[]>([]);
  loading = signal(true);
  isConnected = signal(false);
  unreadCount = signal(0);
  isExpanded = false;
  newMessage = '';
  private shouldScrollToBottom = false;

  ngOnInit(): void {
    // Subscribe to connection status
    this.connectionSubscription = this.chatService.connected$.subscribe(
      connected => this.isConnected.set(connected)
    );

    // Subscribe to new messages
    this.messageSubscription = this.chatService.messages$.subscribe(msg => {
      if (msg.projectId === this.projectId) {
        this.messages.update(msgs => [...msgs, msg]);
        this.shouldScrollToBottom = true;
        if (!this.isExpanded) {
          this.unreadCount.update(c => c + 1);
        }
      }
    });

    // Load existing messages and subscribe to project
    this.loadMessages();
    // subscribeToProject will handle connection automatically
    this.chatService.subscribeToProject(this.projectId);
  }

  ngOnDestroy(): void {
    this.messageSubscription?.unsubscribe();
    this.connectionSubscription?.unsubscribe();
    this.chatService.disconnect();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  loadMessages(): void {
    this.loading.set(true);
    this.chatService.getRecentMessages(this.projectId).subscribe({
      next: (msgs) => {
        this.messages.set(msgs);
        this.loading.set(false);
        this.shouldScrollToBottom = true;
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  sendMessage(): void {
    if (!this.newMessage.trim()) return;

    const message: ChatMessage = {
      projectId: this.projectId,
      senderId: this.currentUserId,
      senderName: this.currentUserName,
      content: this.newMessage.trim(),
      type: 'TEXT',
      edited: false
    };

    if (this.isConnected()) {
      this.chatService.sendMessageWs(this.projectId, message);
    } else {
      // Fallback to REST
      this.chatService.postMessage(this.projectId, message).subscribe({
        next: (saved) => {
          this.messages.update(msgs => [...msgs, saved]);
          this.shouldScrollToBottom = true;
        }
      });
    }

    this.newMessage = '';
  }

  toggleChat(): void {
    this.isExpanded = !this.isExpanded;
    if (this.isExpanded) {
      this.unreadCount.set(0);
      this.shouldScrollToBottom = true;
    }
  }

  formatTime(dateStr?: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  }

  private scrollToBottom(): void {
    try {
      if (this.messagesContainer) {
        const el = this.messagesContainer.nativeElement;
        el.scrollTop = el.scrollHeight;
      }
    } catch {}
  }
}
