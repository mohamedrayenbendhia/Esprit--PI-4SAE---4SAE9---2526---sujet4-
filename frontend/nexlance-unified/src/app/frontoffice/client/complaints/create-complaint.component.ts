import { Component, OnInit, OnDestroy, signal, inject, ViewChild, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import {
  ReactiveFormsModule, FormBuilder, FormGroup,
  Validators, AbstractControl
} from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { Subject, Subscription, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, takeUntil, catchError, tap } from 'rxjs/operators';
import { ComplaintService } from '@core/services/complaint.service';
import { ComplaintAiService, AiSuggestion } from '@core/services/complaint-ai.service';
import { AuthService } from '@core/services/auth.service';
import {
  ComplaintCategory, ComplaintPriority,
  CATEGORY_LABELS, PRIORITY_LABELS, CreateComplaintRequest
} from '@core/models/complaint.model';
import { AttachmentManagerComponent, ManagedFile } from '@shared/components/attachment-manager/attachment-manager.component';

const MAX_FILES      = 5;
const MAX_FILE_SIZE_MB = 10;

@Component({
  selector: 'app-create-complaint',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatIconModule, MatSnackBarModule, TranslateModule,
    AttachmentManagerComponent
  ],
  templateUrl: './create-complaint.component.html',
  styleUrls: ['./create-complaint.component.scss']
})
export class CreateComplaintComponent implements OnInit, OnDestroy {

  @Input() userType: 'client' | 'freelancer' = 'client';

  @ViewChild(AttachmentManagerComponent) attachmentManager?: AttachmentManagerComponent;

  // ── State ─────────────────────────────────────────────────
  isSubmitting   = signal(false);
  isUploading    = signal(false);
  uploadProgress = signal<string>('');
  formSubmitted  = signal(false);

  // ── Email verification state ──────────────────────────────
  emailChecking  = signal(false);
  emailResolved  = signal<{firstName: string; lastName: string; type: string} | null>(null);
  emailNotFound  = signal(false);

  // ── AI suggestion state ───────────────────────────────────
  aiEnabled      = signal(false);
  aiChecking     = signal(true);   // true pendant la vérification initiale
  aiSuggestion   = signal<AiSuggestion | null>(null);
  aiLoading      = signal(false);
  aiDismissed    = signal(false);
  aiFilledFields = signal(false);

  private aiSubject = new Subject<{subject: string; description: string}>();
  private aiSub!: Subscription;

  // ── Données statiques ─────────────────────────────────────
  categories = Object.values(ComplaintCategory).map(v => ({
    value: v, label: CATEGORY_LABELS[v], icon: this.getCategoryIcon(v)
  }));
  priorities = Object.values(ComplaintPriority).map(v => ({
    value: v, label: PRIORITY_LABELS[v]
  }));

  maxFileSizeMb = MAX_FILE_SIZE_MB;
  maxFiles      = MAX_FILES;

  // ── Reactive Form ─────────────────────────────────────────
  form!: FormGroup;

  private fb            = inject(FormBuilder);
  private complaintSvc  = inject(ComplaintService);
  private aiService     = inject(ComplaintAiService);
  private authService   = inject(AuthService);
  private router        = inject(Router);
  private snackBar      = inject(MatSnackBar);

  private destroy$ = new Subject<void>();

  // ─────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.form = this.fb.group({
      category:       ['', Validators.required],
      priority:       [ComplaintPriority.MEDIUM, Validators.required],
      subject: ['', [
        Validators.required,
        Validators.minLength(5),
        Validators.maxLength(255)
      ]],
      description: ['', [
        Validators.required,
        Validators.minLength(20),
        Validators.maxLength(5000)
      ]],
      reportedUserEmail: ['', [
        Validators.maxLength(255),
        Validators.email,
        (control: AbstractControl) => {
          const currentEmail = this.authService.getCurrentUser()?.email;
          if (control.value && control.value === currentEmail) {
            return { selfReport: true };
          }
          return null;
        }
      ]],
      projectName: ['', [
        Validators.maxLength(255)
      ]]
    });

    // ── Vérification statut IA au démarrage ──────────────────
    this.aiService.checkStatus().pipe(takeUntil(this.destroy$)).subscribe(enabled => {
      this.aiEnabled.set(enabled);
      this.aiChecking.set(false);
    });

    // ── AI suggestion — debounce sur sujet + description ─────
    this.aiSub = this.aiSubject.pipe(
      debounceTime(1500),
      distinctUntilChanged((a, b) => a.subject === b.subject && a.description === b.description),
      switchMap(({ subject, description }) => {
        if (!this.aiEnabled()) return of(null);   // IA désactivée → pas d'appel
        this.aiLoading.set(true);
        this.aiSuggestion.set(null);
        this.aiDismissed.set(false);
        return this.aiService.suggest(subject, description).pipe(catchError(() => of(null)));
      }),
      takeUntil(this.destroy$)
    ).subscribe(suggestion => {
      this.aiLoading.set(false);
      this.aiSuggestion.set(suggestion);
    });

    this.form.valueChanges.pipe(
      debounceTime(1000),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      const desc    = this.ctrl('description').value?.trim() ?? '';
      const subject = this.ctrl('subject').value?.trim() ?? '';
      if (desc.length >= 20) {
        this.aiSubject.next({ subject, description: desc });
      }
    });

    // ── Debounced email verification ──
    this.ctrl('reportedUserEmail').valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(500),
      distinctUntilChanged(),
      tap(val => {
        // Reset state on every change
        this.emailResolved.set(null);
        this.emailNotFound.set(false);
        if (!val || val.trim().length === 0) {
          this.emailChecking.set(false);
        }
      }),
      switchMap(val => {
        const email = val?.trim();
        if (!email || this.ctrl('reportedUserEmail').hasError('email') || this.ctrl('reportedUserEmail').hasError('selfReport')) {
          return of(null);
        }
        this.emailChecking.set(true);
        return this.complaintSvc.checkUserByEmail(email).pipe(
          catchError(() => of(null))
        );
      })
    ).subscribe(result => {
      this.emailChecking.set(false);
      if (result) {
        this.emailResolved.set({ firstName: result.firstName, lastName: result.lastName, type: result.type });
        this.emailNotFound.set(false);
      } else if (this.ctrl('reportedUserEmail').value?.trim()?.length > 0
                 && !this.ctrl('reportedUserEmail').hasError('email')
                 && !this.ctrl('reportedUserEmail').hasError('selfReport')) {
        this.emailNotFound.set(true);
        this.emailResolved.set(null);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.aiSub?.unsubscribe();
  }

  // ── Helpers validation ─────────────────────────────────────

  ctrl(name: string): AbstractControl {
    return this.form.get(name)!;
  }

  showError(name: string): boolean {
    const c = this.ctrl(name);
    return c.invalid && (c.touched || c.dirty || this.formSubmitted());
  }

  errorMsg(name: string): string {
    const c = this.ctrl(name);
    if (!c.errors) return '';
    if (c.errors['required'])  return 'This field is required.';
    if (c.errors['minlength']) {
      const min = c.errors['minlength'].requiredLength;
      return `Minimum ${min} character${min > 1 ? 's' : ''} required.`;
    }
    if (c.errors['maxlength']) {
      const max = c.errors['maxlength'].requiredLength;
      return `Maximum ${max} characters allowed.`;
    }
    if (c.errors['pattern'])    return 'Invalid format.';
    if (c.errors['email'])      return 'Invalid email address.';
    if (c.errors['selfReport']) return 'You cannot file a complaint against yourself.';
    return 'Invalid value.';
  }

  /** true si un email a été saisi mais n'est pas encore validé */
  get emailPending(): boolean {
    const val = this.ctrl('reportedUserEmail').value?.trim();
    if (!val) return false;
    if (this.ctrl('reportedUserEmail').hasError('email') || this.ctrl('reportedUserEmail').hasError('selfReport')) return false;
    return this.emailChecking() || (!this.emailResolved() && !this.emailNotFound());
  }

  /** Soumission bloquée si email non résolu */
  get isSubmitBlocked(): boolean {
    const emailVal = this.ctrl('reportedUserEmail').value?.trim();
    if (!emailVal) return false; // pas d'email = pas de blocage
    return this.emailPending || this.emailNotFound() || this.emailChecking();
  }

  // ── IA : accepter / ignorer la suggestion ──────────────────

  acceptAiSuggestion(): void {
    const s = this.aiSuggestion();
    if (!s) return;
    this.form.patchValue({ category: s.category, priority: s.priority });
    this.aiDismissed.set(true);
    this.aiSuggestion.set(null);
    this.aiFilledFields.set(true);
    setTimeout(() => this.aiFilledFields.set(false), 3000);
  }

  dismissAiSuggestion(): void {
    this.aiDismissed.set(true);
    this.aiSuggestion.set(null);
  }

  // ── Soumission ─────────────────────────────────────────────

  submit(): void {
    this.formSubmitted.set(true);
    this.form.markAllAsTouched();

    if (this.form.invalid || this.isSubmitBlocked) {
      setTimeout(() => {
        const el = document.querySelector('.field-error, .email-status.not-found');
        el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 50);
      return;
    }

    this.isSubmitting.set(true);
    const managedFiles: ManagedFile[] = this.attachmentManager?.getFiles() ?? [];
    const files = managedFiles.map(f => f.file).filter((f): f is File => !!f);

    if (files.length > 0) {
      this.isUploading.set(true);
      this.uploadProgress.set(`Uploading ${files.length} file${files.length > 1 ? 's' : ''}...`);
      this.complaintSvc.uploadComplaintAttachments(files).subscribe({
        next: ({ urls }) => {
          this.isUploading.set(false);
          this.uploadProgress.set('');
          this.submitWithAttachments(urls);
        },
        error: err => {
          this.isUploading.set(false);
          this.uploadProgress.set('');
          const msg = err?.error?.error || 'Error uploading files.';
          this.snackBar.open(msg, 'Close', { duration: 5000 });
          this.isSubmitting.set(false);
        }
      });
    } else {
      this.submitWithAttachments([]);
    }
  }

  private submitWithAttachments(attachmentUrls: string[]): void {
    const v = this.form.value;

    const payload: CreateComplaintRequest = {
      category:    v.category,
      priority:    v.priority,
      subject:     v.subject.trim(),
      description: v.description.trim(),
      ...(v.reportedUserEmail?.trim() && { reportedUserEmail: v.reportedUserEmail.trim() }),
      ...(v.projectName?.trim()       && { projectId: v.projectName.trim() }),
      ...(attachmentUrls.length       && { attachments: attachmentUrls })
    };

    this.complaintSvc.createComplaint(payload).subscribe({
      next: complaint => {
        this.snackBar.open('Complaint submitted successfully!', 'Close', {
          duration: 4000, panelClass: ['snack-success']
        });
        const base = this.userType === 'freelancer'
          ? '/frontoffice/freelancer/my-complaints'
          : '/frontoffice/client/my-complaints';
        this.router.navigate([base, complaint.id]);
      },
      error: err => {
        const msg = err?.error?.message || err?.error?.error || 'Submission error. Please try again.';
        this.snackBar.open(msg, 'Close', { duration: 5000 });
        this.isSubmitting.set(false);
      }
    });
  }

  cancel(): void {
    const base = this.userType === 'freelancer'
      ? '/frontoffice/freelancer/my-complaints'
      : '/frontoffice/client/my-complaints';
    this.router.navigate([base]);
  }

  // ── Getters pratiques ─────────────────────────────────────

  get isFormInvalid(): boolean {
    return this.form?.invalid ?? true;
  }

  get subjectLength(): number   { return this.ctrl('subject').value?.length ?? 0; }
  get descLength(): number      { return this.ctrl('description').value?.length ?? 0; }
  get descProgressPct(): number { return Math.min((this.ctrl('description').value?.length ?? 0) / 5000 * 100, 100); }
  get selectedCategory(): string { return this.ctrl('category').value; }
  get selectedPriority(): string { return this.ctrl('priority').value; }
  get checklistPct(): number {
    let done = 0;
    if (this.selectedCategory) done++;
    if (this.selectedPriority) done++;
    if (this.subjectLength >= 5) done++;
    if (this.descLength >= 20) done++;
    return Math.round((done / 4) * 100);
  }

  // ── Icônes & utilitaires ──────────────────────────────────

  getCategoryIcon(cat: string): string {
    const map: Record<string, string> = {
      PAYMENT_ISSUE:          'payments',
      QUALITY_DISPUTE:        'star_half',
      COMMUNICATION_PROBLEM:  'chat_bubble_outline',
      HARASSMENT:             'report',
      SCAM:                   'warning',
      TECHNICAL_ISSUE:        'build',
      OTHER:                  'help_outline'
    };
    return map[cat] ?? 'help_outline';
  }

}
