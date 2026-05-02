import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { FreelanceInvitationService } from '@core/services/freelance-invitation.service';
import { FreelanceInvitation, InvitationStatus } from '@core/models/freelance-invitation.model';
import { UserService } from '@core/services/user.service';
import { JobOfferService } from '@core/services/job-offer.service';

@Component({
  selector: 'app-invitation-detail-freelancer',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule
  ],
  templateUrl: './invitation-detail.component.html',
  styleUrls: ['./invitation-detail.component.scss']
})
export class InvitationDetailFreelancerComponent implements OnInit {
  invitation!: FreelanceInvitation;
  isLoading = true;
  isProcessing = false;
  InvitationStatus = InvitationStatus;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private invitationService: FreelanceInvitationService,
    private userService: UserService,
    private jobOfferService: JobOfferService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadInvitation(id);
    }
  }

  loadInvitation(id: string): void {
    this.isLoading = true;
    this.invitationService.getInvitationById(id).subscribe({
      next: (invitation) => {
        this.invitation = invitation;
        this.isLoading = false;
        this.populateRelations(invitation);
      },
      error: (error) => {
        console.error('Error loading invitation:', error);
        this.isLoading = false;
        alert('Error loading invitation');
        this.goBack();
      }
    });
  }

  private populateRelations(invitation: FreelanceInvitation): void {
    if (invitation.clientId) {
      this.userService.getUserById(invitation.clientId).subscribe({
        next: (user) => this.invitation.client = user,
        error: (err) => console.error('Error loading client:', err)
      });
    }
    if (invitation.jobOfferId) {
      this.jobOfferService.getJobOfferById(invitation.jobOfferId).subscribe({
        next: (offer) => this.invitation.jobOffer = offer,
        error: (err) => console.error('Error loading job offer:', err)
      });
    }
  }

  acceptInvitation(): void {
    if (confirm('Are you sure you want to accept this invitation and apply to this offer?')) {
      this.isProcessing = true;
      this.invitationService.acceptInvitation(this.invitation.id).subscribe({
        next: () => {
          alert('Invitation accepted! You will be redirected to the application form.');
          // Redirect to the job offer detail to apply
          this.router.navigate(['/frontoffice/freelancer/jobs', this.invitation.jobOfferId]);
        },
        error: (error) => {
          console.error('Error accepting invitation:', error);
          alert('Error accepting invitation');
          this.isProcessing = false;
        }
      });
    }
  }

  declineInvitation(): void {
    const reason = prompt('Why are you declining this invitation? (optional)');
    if (reason !== null) { // null = cancelled
      this.isProcessing = true;
      this.invitationService.declineInvitation(this.invitation.id, reason).subscribe({
        next: () => {
          alert('Invitation declined');
          this.goBack();
        },
        error: (error) => {
          console.error('Error declining invitation:', error);
          alert('Error declining invitation');
          this.isProcessing = false;
        }
      });
    }
  }

  viewJobOffer(): void {
    this.router.navigate(['/frontoffice/freelancer/jobs', this.invitation.jobOfferId]);
  }

  goBack(): void {
    this.router.navigate(['/frontoffice/freelancer/my-invitations']);
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  isExpired(): boolean {
    if (!this.invitation.deadlineResponse) return false;
    return new Date(this.invitation.deadlineResponse) < new Date();
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'PENDING': 'Pending',
      'ACCEPTED': 'Accepted',
      'DECLINED': 'Declined',
      'EXPIRED': 'Expired'
    };
    return labels[status] || status;
  }

  getDaysRemaining(): number {
    if (!this.invitation.deadlineResponse) return 0;
    const today = new Date();
    const deadline = new Date(this.invitation.deadlineResponse);
    const diff = deadline.getTime() - today.getTime();
    return Math.ceil(diff / (1000 * 60 * 60 * 24));
  }
}
