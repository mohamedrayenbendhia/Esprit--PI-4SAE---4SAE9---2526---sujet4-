import {
  Component, Input, OnInit, OnDestroy, OnChanges,
  SimpleChanges, ChangeDetectorRef, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { interval, Subscription } from 'rxjs';
import { ComplaintService } from '../../../core/services/complaint.service';
import { AuthService } from '../../../core/services/auth.service';
import { ComplaintAdvancedService } from '../../../core/services/complaint-advanced.service';
import {
  Complaint, SupportMessage, ConversationType,
  MessageType, SenderType, InvolveReportedRequest
} from '../../../core/models/complaint.model';
import { ResponseTemplate } from '../../../core/models/complaint-advanced.model';
import { UserRole } from '../../models/user.model';

/**
 * Composant de messagerie pour une réclamation.
 *
 * Affiche jusqu'à deux fils de conversation selon le rôle :
 *
 * FREELANCE / CLIENT (plaignant) :
 *   → Onglet unique : sa conversation avec le support
 *
 * FREELANCE / CLIENT (partie mise en cause) :
 *   → Onglet unique : sa conversation avec le support (fil REPORTED)
 *
 * SUPPORT_AGENT / ADMIN :
 *   → Onglet A : fil COMPLAINANT (plaignant ↔ support)
 *   → Onglet B : fil REPORTED (partie mise en cause ↔ support)
 *                + bouton "Impliquer la partie" si pas encore activé
 */
@Component({
  selector: 'app-complaint-conversation',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatIconModule, MatButtonModule, MatTabsModule,
    MatInputModule, MatFormFieldModule, MatProgressSpinnerModule,
    MatTooltipModule, MatDialogModule, MatChipsModule, MatSelectModule
  ],
  template: `
    <div class="conversation-wrap">

      <!-- ── Support/Admin : deux onglets ──────────────────── -->
      @if (isPrivileged) {
        <mat-tab-group animationDuration="200ms" (selectedIndexChange)="onTabChange($event)">

          <!-- Tab A — Complainant ↔ Support -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon class="tab-icon complainant-icon">person</mat-icon>
              Complainant
              @if (unreadComplainant > 0) {
                <span class="tab-badge">{{ unreadComplainant }}</span>
              }
            </ng-template>
            <ng-template matTabContent>
              <div class="conversation-hint complainant-hint">
                <mat-icon>lock</mat-icon>
                Confidential conversation — visible only to the complainant and support
              </div>
              <ng-container *ngTemplateOutlet="messageThread; context: {
                messages: complainantMessages,
                loading: loadingComplainant,
                conv: 'COMPLAINANT'
              }"></ng-container>
            </ng-template>
          </mat-tab>

          <!-- Tab B — Reported party ↔ Support -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon class="tab-icon reported-icon">gavel</mat-icon>
              Reported party
              @if (unreadReported > 0) {
                <span class="tab-badge reported">{{ unreadReported }}</span>
              }
            </ng-template>
            <ng-template matTabContent>
              @if (!reportedConvExists) {
                <!-- Pas encore impliqué -->
                <div class="involve-panel">
                  <mat-icon class="involve-icon">person_add</mat-icon>
                  <h3>Involve the reported party</h3>
                  <p class="involve-desc">
                    The reported party has not yet been involved in this complaint.
                    They cannot see the current exchanges. You can involve them by sending
                    an invitation message.
                  </p>
                  @if (complaint.reportedUserId) {
                    <div class="involve-form">
                      <mat-form-field appearance="outline" class="full-width">
                        <mat-label>Invitation message</mat-label>
                        <textarea matInput [(ngModel)]="invitationMessage" rows="4"
                                  placeholder="Explain to the reported party why they are involved…">
                        </textarea>
                      </mat-form-field>
                      <button mat-flat-button color="primary"
                              [disabled]="!invitationMessage.trim() || involvingReported"
                              (click)="involveReportedUser()">
                        @if (involvingReported) {
                          <mat-spinner diameter="18"></mat-spinner>
                        } @else {
                          <ng-container>
                          <mat-icon>person_add</mat-icon>
                          Involve the party
                          </ng-container>
                        }
                      </button>
                    </div>
                  } @else {
                    <p class="no-reported">
                      <mat-icon>info</mat-icon>
                      No reported party has been designated in this complaint.
                    </p>
                  }
                </div>
              } @else {
                <div class="conversation-hint reported-hint">
                  <mat-icon>gavel</mat-icon>
                  Confidential conversation — visible only to the reported party and support
                </div>
                <ng-container *ngTemplateOutlet="messageThread; context: {
                  messages: reportedMessages,
                  loading: loadingReported,
                  conv: 'REPORTED'
                }"></ng-container>
              }
            </ng-template>
          </mat-tab>
        </mat-tab-group>

      } @else {
        <!-- ── Utilisateur : fil unique ──────────────────── -->
        <div class="single-conv">
          <div class="conversation-hint user-hint">
            <mat-icon>chat_bubble</mat-icon>
            Chat directly with our support team
          </div>
          <ng-container *ngTemplateOutlet="messageThread; context: {
            messages: userMessages,
            loading: loadingUser,
            conv: userConvType
          }"></ng-container>
        </div>
      }

      <!-- ── Template : fil de messages ────────────────────── -->
      <ng-template #messageThread let-messages="messages" let-loading="loading" let-conv="conv">
        <div class="thread-wrap">

          <!-- Messages -->
          <div class="messages-area" #messagesContainer>
            @if (loading) {
              <div class="loading-wrap">
                <mat-spinner diameter="32"></mat-spinner>
              </div>
            } @else if (messages.length === 0) {
              <div class="empty-conv">
                <mat-icon>chat_bubble_outline</mat-icon>
                <p>No messages yet</p>
              </div>
            } @else {
              @for (msg of messages; track msg.id) {
                <div class="message-bubble"
                     [class.outgoing]="msg.senderId === currentUserId"
                     [class.incoming]="msg.senderId !== currentUserId"
                     [class.note-interne]="msg.messageType === 'NOTE_INTERNE'"
                     [class.auto]="msg.messageType === 'AUTO_RESPONSE'">

                  <!-- Badge type -->
                  @if (msg.messageType === 'NOTE_INTERNE') {
                    <div class="msg-badge note"><mat-icon>visibility_off</mat-icon> Internal note</div>
                  }
                  @if (msg.messageType === 'AUTO_RESPONSE') {
                    <div class="msg-badge auto"><mat-icon>smart_toy</mat-icon> Automatic</div>
                  }

                  <div class="bubble-content">
                    <span class="sender-label">
                      {{ msg.senderId === currentUserId ? 'You' :
                         msg.senderType === 'SUPPORT' ? 'NexLance Support' : 'User' }}
                    </span>
                    <p class="msg-text">{{ msg.content }}</p>
                    <div class="msg-meta">
                      <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
                      @if (msg.senderId === currentUserId) {
                        <mat-icon class="read-icon" [class.read]="msg.isRead">
                          {{ msg.isRead ? 'done_all' : 'done' }}
                        </mat-icon>
                      }
                    </div>
                  </div>
                </div>
              }
            }
          </div>

          <!-- Zone de saisie / message de blocage -->
          @if (!isConversationLocked) {
            <div class="input-area" [class.note-mode]="selectedMessageType === 'NOTE_INTERNE'">

              @if (isPrivileged) {
                <div class="type-toggle">
                  <button class="type-btn" [class.active]="selectedMessageType === 'TEXT'"
                          (click)="selectedMessageType = 'TEXT'" [disabled]="sending">
                    <mat-icon>chat_bubble_outline</mat-icon> Message
                  </button>
                  <button class="type-btn note" [class.active]="selectedMessageType === 'NOTE_INTERNE'"
                          (click)="selectedMessageType = 'NOTE_INTERNE'" [disabled]="sending">
                    <mat-icon>visibility_off</mat-icon> Internal note
                  </button>
                  <div class="template-picker-wrap">
                    <button class="type-btn template-btn" (click)="toggleTemplatePicker()" [disabled]="sending"
                            matTooltip="Insert a response template">
                      <mat-icon>article</mat-icon> Templates
                    </button>
                    @if (showTemplatePicker) {
                      <div class="template-dropdown">
                        @if (templatesLoading) {
                          <div class="tpl-loading"><mat-spinner diameter="20"></mat-spinner></div>
                        } @else if (templates.length === 0) {
                          <div class="tpl-empty">No templates available</div>
                        } @else {
                          @for (tpl of templates; track tpl.id) {
                            <button class="tpl-item" (click)="applyTemplate(tpl)">
                              <span class="tpl-title">{{ tpl.title }}</span>
                              @if (tpl.category) {
                                <span class="tpl-cat">{{ tpl.category }}</span>
                              }
                            </button>
                          }
                        }
                      </div>
                    }
                  </div>
                </div>
              }

              <div class="composer">
                <textarea class="composer-textarea"
                          [(ngModel)]="newMessage"
                          [placeholder]="getInputPlaceholder(conv)"
                          rows="2"
                          (keydown.enter)="onEnterKey($event, conv)">
                </textarea>
                <button class="send-btn"
                        [disabled]="!newMessage.trim() || sending"
                        (click)="sendMessage(conv)"
                        title="Send (Enter)">
                  @if (sending) {
                    <span class="send-spinner"></span>
                  } @else {
                    <mat-icon>send</mat-icon>
                  }
                </button>
              </div>

              <div class="composer-hint">
                <mat-icon>keyboard_return</mat-icon> Enter to send · Shift+Enter for a new line
              </div>
            </div>
          } @else {
            <div class="conversation-locked">
              <mat-icon>lock</mat-icon>
              <span>{{ lockMessage }}</span>
            </div>
          }
        </div>
      </ng-template>

    </div>
  `,
  styles: [`
    .conversation-wrap { display: flex; flex-direction: column; height: 100%; }

    /* ── Onglets ──────────────────────────────────────────── */
    .tab-icon { font-size: 16px; width: 16px; height: 16px; margin-right: 4px; vertical-align: middle; }
    .complainant-icon { color: #1565C0; }
    .reported-icon    { color: #6A1B9A; }
    .tab-badge {
      background: #1565C0; color: #fff; border-radius: 10px;
      padding: 0 6px; font-size: 10px; font-weight: 700;
      margin-left: 4px; line-height: 16px;
      &.reported { background: #6A1B9A; }
    }

    /* ── Hints de confidentialité ─────────────────────────── */
    .conversation-hint {
      display: flex; align-items: center; gap: 6px;
      padding: 6px 14px; font-size: 11.5px; font-weight: 500;
      mat-icon { font-size: 14px; width: 14px; height: 14px; }
    }
    .complainant-hint { background: #E3F2FD; color: #1565C0; }
    .reported-hint    { background: #EDE7F6; color: #6A1B9A; }
    .user-hint        { background: #E8F5E9; color: #2E7D32; }

    /* ── Panel "Impliquer" ────────────────────────────────── */
    .involve-panel {
      display: flex; flex-direction: column; align-items: center;
      padding: 32px 24px; text-align: center; gap: 12px;
    }
    .involve-icon { font-size: 48px; width: 48px; height: 48px; color: #6A1B9A; opacity: 0.7; }
    .involve-panel h3 { margin: 0; font-size: 16px; font-weight: 700; color: #1a1a1a; }
    .involve-desc { font-size: 13px; color: #666; line-height: 1.6; max-width: 420px; margin: 0; }
    .involve-form { width: 100%; max-width: 480px; display: flex; flex-direction: column; gap: 12px; }
    .full-width { width: 100%; }
    .no-reported {
      display: flex; align-items: center; gap: 6px; color: #999; font-size: 13px;
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
    }

    /* ── Fil de messages ──────────────────────────────────── */
    .thread-wrap { display: flex; flex-direction: column; height: 480px; }
    .messages-area {
      flex: 1; overflow-y: auto; padding: 16px;
      display: flex; flex-direction: column; gap: 10px;
      scrollbar-width: thin; scrollbar-color: #ccc transparent;
    }
    .loading-wrap { display: flex; justify-content: center; align-items: center; height: 100%; }
    .empty-conv {
      display: flex; flex-direction: column; align-items: center;
      justify-content: center; height: 100%; color: #bbb; gap: 8px;
      mat-icon { font-size: 36px; width: 36px; height: 36px; }
      p { font-size: 13px; margin: 0; }
    }

    /* ── Bulles ───────────────────────────────────────────── */
    .message-bubble { display: flex; flex-direction: column; max-width: 75%; }
    .message-bubble.outgoing { align-self: flex-end; align-items: flex-end; }
    .message-bubble.incoming { align-self: flex-start; align-items: flex-start; }
    .message-bubble.note-interne { max-width: 90%; align-self: center; }
    .message-bubble.auto         { max-width: 90%; align-self: center; }

    .msg-badge {
      display: flex; align-items: center; gap: 4px;
      font-size: 10px; font-weight: 600; padding: 2px 8px;
      border-radius: 10px; margin-bottom: 2px;
      mat-icon { font-size: 12px; width: 12px; height: 12px; }
      &.note { background: #FFF3E0; color: #E65100; }
      &.auto  { background: #E3F2FD; color: #1565C0; }
    }

    .bubble-content {
      padding: 8px 12px; border-radius: 12px; position: relative;
      .outgoing > & { background: #1565C0; color: #fff; border-bottom-right-radius: 2px; }
      .incoming > & { background: #F5F5F5; color: #1a1a1a; border-bottom-left-radius: 2px; }
      .note-interne > & { background: #FFF8E1; color: #5D4037;
                          border: 1px dashed #FFB74D; border-radius: 8px; }
      .auto > & { background: #E3F2FD; color: #1565C0;
                  border: 1px solid #BBDEFB; border-radius: 8px; }
    }
    .sender-label { font-size: 10px; font-weight: 700; opacity: 0.7; display: block; margin-bottom: 2px; }
    .msg-text { margin: 0; font-size: 13px; line-height: 1.5; white-space: pre-wrap; word-break: break-word; }
    .msg-meta { display: flex; align-items: center; gap: 4px; margin-top: 4px; justify-content: flex-end; }
    .msg-time { font-size: 10px; opacity: 0.65; }
    .read-icon { font-size: 14px; width: 14px; height: 14px; opacity: 0.6;
                 &.read { opacity: 1; color: #4FC3F7; } }

    /* ── Zone de saisie ───────────────────────────────────── */
    .input-area {
      display: flex;
      flex-direction: column;
      gap: 8px;
      padding: 12px 14px 10px;
      border-top: 1px solid #e5e7eb;
      background: #f8fafc;
    }

    /* Toggle Message / Note interne */
    .type-toggle {
      display: flex;
      gap: 6px;
    }
    .type-btn {
      display: inline-flex; align-items: center; gap: 5px;
      padding: 4px 12px;
      border: 1.5px solid #e5e7eb;
      border-radius: 999px;
      background: white;
      font-size: 12px; font-weight: 500;
      color: #6b7280; cursor: pointer;
      transition: all 150ms ease;
      mat-icon { font-size: 14px; width: 14px; height: 14px; }

      &:hover:not(:disabled):not(.active) { border-color: #0ea5e9; color: #0ea5e9; }

      &.active {
        background: #0ea5e9; border-color: #0ea5e9;
        color: white;
        box-shadow: 0 2px 6px rgba(14,165,233,.3);
      }
      &.note.active {
        background: #f59e0b; border-color: #f59e0b;
        box-shadow: 0 2px 6px rgba(245,158,11,.3);
      }
      &:disabled { opacity: .5; cursor: not-allowed; }
    }

    /* Composer : textarea + bouton send côte à côte */
    .composer {
      display: flex;
      align-items: flex-end;
      gap: 10px;
      background: white;
      border: 1.5px solid #e5e7eb;
      border-radius: 14px;
      padding: 10px 10px 10px 14px;
      transition: border-color 150ms ease, box-shadow 150ms ease;

      &:focus-within {
        border-color: #0ea5e9;
        box-shadow: 0 0 0 3px rgba(14,165,233,.1);
      }
    }

    /* Variante note interne */
    .note-mode .composer {
      border-color: #fde68a;
      background: #fffbeb;
      &:focus-within { border-color: #f59e0b; box-shadow: 0 0 0 3px rgba(245,158,11,.1); }
    }

    .composer-textarea {
      flex: 1;
      border: none; outline: none;
      background: transparent;
      font-size: 13.5px;
      color: #1f2937;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      line-height: 1.55;
      resize: none;
      min-height: 38px;
      max-height: 120px;
      overflow-y: auto;
      &::placeholder { color: #9ca3af; }
    }

    /* Bouton envoyer natif */
    .send-btn {
      width: 38px; height: 38px;
      border-radius: 50%;
      background: linear-gradient(135deg, #0ea5e9, #0284c7);
      color: white; border: none; cursor: pointer;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
      transition: all 150ms ease;
      box-shadow: 0 2px 8px rgba(14,165,233,.35);

      mat-icon { font-size: 18px; width: 18px; height: 18px; }

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 5px 14px rgba(14,165,233,.45);
      }
      &:disabled {
        background: #e5e7eb;
        box-shadow: none;
        cursor: not-allowed;
        transform: none;
        mat-icon { color: #9ca3af; }
      }
    }

    .send-spinner {
      width: 16px; height: 16px;
      border: 2px solid rgba(white, .3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin .7s linear infinite;
      display: block;
    }

    /* Hint raccourci clavier */
    .composer-hint {
      display: flex; align-items: center; gap: 4px;
      font-size: 10.5px; color: #9ca3af;
      padding-left: 4px;
      mat-icon { font-size: 12px; width: 12px; height: 12px; }
    }

    .conversation-locked {
      display: flex; align-items: center; gap: 8px;
      padding: 12px 16px; border-top: 1px solid #e5e7eb;
      background: #f9fafb; color: #9ca3af; font-size: 13px;
      mat-icon { font-size: 16px; width: 16px; height: 16px; flex-shrink: 0; color: #d1d5db; }
    }

    /* ── Template picker ──────────────────────────────────── */
    .template-picker-wrap { position: relative; }
    .template-btn { color: #6366f1 !important; border-color: #6366f1 !important;
                    mat-icon { color: #6366f1; } }
    .template-dropdown {
      position: absolute; top: calc(100% + 6px); left: 0; z-index: 200;
      background: white; border: 1px solid #e5e7eb; border-radius: 10px;
      box-shadow: 0 8px 24px rgba(0,0,0,.12); min-width: 260px; max-height: 280px;
      overflow-y: auto; display: flex; flex-direction: column;
    }
    .tpl-loading { display: flex; justify-content: center; padding: 16px; }
    .tpl-empty { padding: 14px 16px; font-size: 12px; color: #9ca3af; text-align: center; }
    .tpl-item {
      display: flex; flex-direction: column; align-items: flex-start; gap: 2px;
      padding: 10px 14px; border: none; background: transparent; cursor: pointer;
      text-align: left; border-bottom: 1px solid #f3f4f6;
      transition: background 120ms;
      &:last-child { border-bottom: none; }
      &:hover { background: #f0f4ff; }
    }
    .tpl-title { font-size: 13px; font-weight: 500; color: #1a1a1a; }
    .tpl-cat { font-size: 10px; color: #6366f1; font-weight: 600;
               background: #eef2ff; padding: 1px 6px; border-radius: 8px; }

    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class ComplaintConversationComponent implements OnInit, OnDestroy, OnChanges {

  @Input() complaint!: Complaint;

  // Messages par fil
  complainantMessages: SupportMessage[] = [];
  reportedMessages:    SupportMessage[] = [];
  userMessages:        SupportMessage[] = [];

  // États
  loadingComplainant = true;
  loadingReported    = true;
  loadingUser        = true;

  reportedConvExists = false;
  unreadComplainant  = 0;
  unreadReported     = 0;

  // Formulaire
  newMessage          = '';
  selectedMessageType = 'TEXT';
  sending             = false;
  invitationMessage   = '';
  involvingReported   = false;

  // Rôle
  isPrivileged = false;
  currentUserId = '';
  userConvType: string = 'COMPLAINANT';

  private pollSub: Subscription | null = null;
  private subs: Subscription[] = [];

  // Templates
  templates: ResponseTemplate[]    = [];
  templatesLoading                 = false;
  showTemplatePicker               = false;

  constructor(
    private complaintService: ComplaintService,
    private authService: AuthService,
    private advancedService: ComplaintAdvancedService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.currentUserId = user.id;
      this.isPrivileged  = user.role === UserRole.ADMIN || user.role === UserRole.SUPPORT_AGENT;

      // Déterminer le fil de l'utilisateur
      if (!this.isPrivileged && this.complaint) {
        this.userConvType = user.id === this.complaint.reportedUserId
          ? 'REPORTED'
          : 'COMPLAINANT';
      }
    }

    this.loadMessages();
    this.startPolling();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['complaint'] && !changes['complaint'].firstChange) {
      this.loadMessages();
    }
  }

  // ── Chargement ─────────────────────────────────────────────

  loadMessages(): void {
    if (!this.complaint?.id) return;

    if (this.isPrivileged) {
      this.loadComplainantThread();
      this.loadReportedThread();
    } else {
      this.loadUserThread();
    }
  }

  private loadComplainantThread(): void {
    this.loadingComplainant = true;
    this.complaintService.getMessages(this.complaint.id, ConversationType.COMPLAINANT)
      .subscribe({
        next: msgs => {
          this.complainantMessages  = msgs;
          this.unreadComplainant    = msgs.filter(m => !m.isRead).length;
          this.loadingComplainant   = false;
          this.cdr.markForCheck();
        },
        error: () => { this.loadingComplainant = false; this.cdr.markForCheck(); }
      });
  }

  private loadReportedThread(): void {
    this.loadingReported = true;
    this.complaintService.getMessages(this.complaint.id, ConversationType.REPORTED)
      .subscribe({
        next: msgs => {
          this.reportedMessages    = msgs;
          this.reportedConvExists  = msgs.length > 0;
          this.unreadReported      = msgs.filter(m => !m.isRead).length;
          this.loadingReported     = false;
          this.cdr.markForCheck();
        },
        error: () => { this.loadingReported = false; this.cdr.markForCheck(); }
      });
  }

  private loadUserThread(): void {
    this.loadingUser = true;
    const convType = this.userConvType === 'REPORTED'
      ? ConversationType.REPORTED
      : ConversationType.COMPLAINANT;

    this.complaintService.getMessages(this.complaint.id, convType)
      .subscribe({
        next: msgs => {
          this.userMessages = msgs;
          this.loadingUser  = false;
          this.cdr.markForCheck();
        },
        error: () => { this.loadingUser = false; this.cdr.markForCheck(); }
      });
  }

  // ── Polling (rafraîchissement toutes les 15s) ───────────────

  private startPolling(): void {
    this.pollSub = interval(15000).subscribe(() => this.loadMessages());
  }

  // ── Envoi ──────────────────────────────────────────────────

  sendMessage(conv: string): void {
    if (!this.newMessage.trim() || this.sending) return;

    this.sending = true;
    const convType = conv === 'REPORTED'
      ? ConversationType.REPORTED
      : ConversationType.COMPLAINANT;

    const msgType = this.isPrivileged && this.selectedMessageType === 'NOTE_INTERNE'
      ? MessageType.NOTE_INTERNE
      : MessageType.TEXT;

    this.complaintService.sendMessage({
      complaintId:      this.complaint.id,
      content:          this.newMessage.trim(),
      messageType:      msgType,
      conversationType: convType
    }).subscribe({
      next: () => {
        this.newMessage = '';
        this.sending    = false;
        this.loadMessages();
        this.cdr.markForCheck();
      },
      error: () => { this.sending = false; this.cdr.markForCheck(); }
    });
  }

  onEnterKey(event: Event, conv: string): void {
  const ke = event as KeyboardEvent;
  if (!ke.shiftKey) {
    event.preventDefault();
    this.sendMessage(conv);
    }
  }

  // ── Impliquer la partie mise en cause ──────────────────────

  involveReportedUser(): void {
    if (!this.invitationMessage.trim() || this.involvingReported) return;

    this.involvingReported = true;
    const req: InvolveReportedRequest = { invitationMessage: this.invitationMessage.trim() };

    this.complaintService.involveReportedUser(this.complaint.id, req).subscribe({
      next: () => {
        this.involvingReported = false;
        this.invitationMessage = '';
        this.reportedConvExists = true;
        this.loadReportedThread();
        this.cdr.markForCheck();
      },
      error: () => { this.involvingReported = false; this.cdr.markForCheck(); }
    });
  }

  // ── Navigation onglets ─────────────────────────────────────

  onTabChange(index: number): void {
    if (index === 0) this.loadComplainantThread();
    else             this.loadReportedThread();
  }

  // ── Helpers ────────────────────────────────────────────────

  get isConversationLocked(): boolean {
    const status = this.complaint?.status as string;
    if (!status) return false;
    if (status === 'CLOSED') return true;
    if (status === 'RESOLVED' && !this.isPrivileged) return true;
    return false;
  }

  get lockMessage(): string {
    const status = this.complaint?.status as string;
    if (status === 'CLOSED') {
      return 'This complaint has been closed. The conversation is archived.';
    }
    if (status === 'RESOLVED' && !this.isPrivileged) {
      return 'This complaint has been resolved. It will be closed shortly by the administrator.';
    }
    return '';
  }

  getInputPlaceholder(conv: string): string {
    if (this.isPrivileged) {
      return conv === 'REPORTED'
        ? 'Reply to the reported party…'
        : 'Reply to the complainant…';
    }
    return 'Your message…';
  }

  formatTime(date: Date | string): string {
    if (!date) return '';
    const d = new Date(date);
    const now = new Date();
    const isToday = d.toDateString() === now.toDateString();
    return isToday
      ? d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
      : d.toLocaleDateString('en-US', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
  }

  // ── Template picker ────────────────────────────────────────

  toggleTemplatePicker(): void {
    this.showTemplatePicker = !this.showTemplatePicker;
    if (this.showTemplatePicker && this.templates.length === 0) {
      this.templatesLoading = true;
      this.advancedService.getAllTemplates().subscribe({
        next: list => { this.templates = list; this.templatesLoading = false; this.cdr.markForCheck(); },
        error: ()   => { this.templatesLoading = false; this.cdr.markForCheck(); }
      });
    }
    this.cdr.markForCheck();
  }

  applyTemplate(tpl: ResponseTemplate): void {
    this.newMessage = tpl.content;
    this.showTemplatePicker = false;
    this.cdr.markForCheck();
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
    this.subs.forEach(s => s.unsubscribe());
  }
}