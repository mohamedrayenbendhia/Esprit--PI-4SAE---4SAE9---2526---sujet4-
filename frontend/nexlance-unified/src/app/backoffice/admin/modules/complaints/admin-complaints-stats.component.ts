import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ComplaintService } from '@core/services/complaint.service';
import { STATUS_LABELS, STATUS_COLORS, PRIORITY_LABELS, PRIORITY_COLORS, CATEGORY_LABELS } from '@core/models/complaint.model';

@Component({
  selector: 'app-admin-complaints-stats',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatSnackBarModule],
  templateUrl: './admin-complaints-stats.component.html',
  styleUrls: ['./admin-complaints-stats.component.scss']
})
export class AdminComplaintsStatsComponent implements OnInit {
  statsByStatus      = signal<Record<string, number>>({});
  statsByPriority    = signal<Record<string, number>>({});
  agents             = signal<any[]>([]);
  agentComplaintCounts = signal<Record<string, number>>({});
  isLoading          = signal(true);

  constructor(
    private complaintService: ComplaintService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading.set(true);
    let loaded = 0;
    const done = () => { if (++loaded === 3) this.isLoading.set(false); };

    this.complaintService.getStatsByStatus().subscribe({ next: d => { this.statsByStatus.set(d); done(); }, error: done });
    this.complaintService.getStatsByPriority().subscribe({ next: d => { this.statsByPriority.set(d); done(); }, error: done });
    this.complaintService.getAgents().subscribe({
      next: d => {
        this.agents.set(d);
        done();
        // Charger le nombre de plaintes assignées pour chaque agent
        const counts: Record<string, number> = {};
        let pending = d.length;
        if (pending === 0) { this.agentComplaintCounts.set(counts); return; }
        d.forEach((agent: any) => {
          this.complaintService.getComplaintsByAssignedAgent(agent.id).subscribe({
            next: complaints => {
              counts[agent.id] = complaints.length;
              if (--pending === 0) this.agentComplaintCounts.set({ ...counts });
            },
            error: () => { if (--pending === 0) this.agentComplaintCounts.set({ ...counts }); }
          });
        });
      },
      error: done
    });
  }

  totalComplaints(): number {
    return Object.values(this.statsByStatus()).reduce((a, b) => a + b, 0);
  }

  getStatusEntries(): { key: string; label: string; count: number; color: string; pct: number }[] {
    const total = this.totalComplaints() || 1;
    return Object.entries(this.statsByStatus()).map(([key, count]) => ({
      key, count,
      label: (STATUS_LABELS as Record<string, string>)[key] || key,
      color: (STATUS_COLORS as Record<string, string>)[key] || '#999',
      pct: Math.round((count / total) * 100)
    })).sort((a, b) => b.count - a.count);
  }

  getPriorityEntries(): { key: string; label: string; count: number; color: string; pct: number }[] {
    const total = this.totalComplaints() || 1;
    return Object.entries(this.statsByPriority()).map(([key, count]) => ({
      key, count,
      label: (STATUS_LABELS as Record<string, string>)[key] || key,
      color: (STATUS_COLORS as Record<string, string>)[key] || '#999',
      pct: Math.round((count / total) * 100)
    })).sort((a, b) => b.count - a.count);
  }

  goToList(): void { this.router.navigate(['/backoffice/admin/complaints']); }
}