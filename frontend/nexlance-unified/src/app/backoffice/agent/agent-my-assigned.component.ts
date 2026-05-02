import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ComplaintService } from '@core/services/complaint.service';
import {
  Complaint, ComplaintStatus,
  STATUS_LABELS, STATUS_COLORS, PRIORITY_LABELS, PRIORITY_COLORS, CATEGORY_LABELS
} from '@core/models/complaint.model';

@Component({
  selector: 'app-agent-my-assigned',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatSnackBarModule],
  templateUrl: './agent-my-assigned.component.html',
  styleUrls: ['./agent-my-assigned.component.scss']
})
export class AgentMyAssignedComponent implements OnInit {
  complaints = signal<Complaint[]>([]);
  isLoading  = signal(true);

  constructor(
    private complaintService: ComplaintService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading.set(true);
    this.complaintService.getMyAssignedComplaints().subscribe({
      next: data => { this.complaints.set(data); this.isLoading.set(false); },
      error: () => {
        this.snackBar.open('Loading error', 'Close', { duration: 3000 });
        this.isLoading.set(false);
      }
    });
  }

  viewDetail(id: string): void {
    this.router.navigate(['/backoffice/agent/complaints', id]);
  }

  goToQueue(): void {
    this.router.navigate(['/backoffice/agent/queue']);
  }

  countByStatus(status: string): number {
    return this.complaints().filter(c => c.status === status).length;
  }


// APRÈS — cast sur l'objet Record
getStatusLabel   = (s: any) => (STATUS_LABELS   as Record<string, string>)[s] || s;
getStatusColor   = (s: any) => (STATUS_COLORS   as Record<string, string>)[s] || '#999';
getPriorityLabel = (p: any) => (PRIORITY_LABELS as Record<string, string>)[p] || p;
getPriorityColor = (p: any) => (PRIORITY_COLORS as Record<string, string>)[p] || '#999';
getCategoryLabel = (c: any) => (CATEGORY_LABELS as Record<string, string>)[c] || c;

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-GB', {
      day: '2-digit', month: 'short',
      hour: '2-digit', minute: '2-digit'
    });
  }
}