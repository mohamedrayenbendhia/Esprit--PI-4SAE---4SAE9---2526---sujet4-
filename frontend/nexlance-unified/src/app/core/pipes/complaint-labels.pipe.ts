import { Pipe, PipeTransform } from '@angular/core';

export type ComplaintStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'ESCALATED' | 'PENDING_USER';
export type ComplaintPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type ComplaintCategory = 'PAYMENT_ISSUE' | 'QUALITY_DISPUTE' | 'HARASSMENT' | 'TECHNICAL_ISSUE' | 'SCAM' | 'COMMUNICATION_PROBLEM' | 'OTHER';

const STATUS_LABELS: Record<string, string> = {
  OPEN: 'Open', IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved',
  CLOSED: 'Closed', ESCALATED: 'Escalated', PENDING_USER: 'Pending User'
};
const STATUS_COLORS: Record<string, string> = {
  OPEN: 'info', IN_PROGRESS: 'warning', RESOLVED: 'success',
  CLOSED: 'neutral', ESCALATED: 'error', PENDING_USER: 'neutral'
};
const PRIORITY_LABELS: Record<string, string> = {
  LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', CRITICAL: 'Critical'
};
const PRIORITY_COLORS: Record<string, string> = {
  LOW: 'neutral', MEDIUM: 'info', HIGH: 'warning', CRITICAL: 'error'
};
const CATEGORY_LABELS: Record<string, string> = {
  PAYMENT_ISSUE: 'Payment Issue', QUALITY_DISPUTE: 'Quality Dispute',
  HARASSMENT: 'Harassment', TECHNICAL_ISSUE: 'Technical Issue',
  SCAM: 'Scam', COMMUNICATION_PROBLEM: 'Communication Problem', OTHER: 'Other'
};
const CATEGORY_ICONS: Record<string, string> = {
  PAYMENT_ISSUE: 'payments', QUALITY_DISPUTE: 'star_half',
  HARASSMENT: 'report', TECHNICAL_ISSUE: 'build',
  SCAM: 'warning', COMMUNICATION_PROBLEM: 'chat_bubble', OTHER: 'help'
};

@Pipe({ name: 'statusLabel', standalone: true, pure: true })
export class StatusLabelPipe implements PipeTransform {
  transform(value: string): string { return STATUS_LABELS[value] ?? value; }
}

@Pipe({ name: 'statusColor', standalone: true, pure: true })
export class StatusColorPipe implements PipeTransform {
  transform(value: string): string { return STATUS_COLORS[value] ?? 'neutral'; }
}

@Pipe({ name: 'priorityLabel', standalone: true, pure: true })
export class PriorityLabelPipe implements PipeTransform {
  transform(value: string): string { return PRIORITY_LABELS[value] ?? value; }
}

@Pipe({ name: 'priorityColor', standalone: true, pure: true })
export class PriorityColorPipe implements PipeTransform {
  transform(value: string): string { return PRIORITY_COLORS[value] ?? 'neutral'; }
}

@Pipe({ name: 'categoryLabel', standalone: true, pure: true })
export class CategoryLabelPipe implements PipeTransform {
  transform(value: string): string { return CATEGORY_LABELS[value] ?? value; }
}

@Pipe({ name: 'categoryIcon', standalone: true, pure: true })
export class CategoryIconPipe implements PipeTransform {
  transform(value: string): string { return CATEGORY_ICONS[value] ?? 'help'; }
}

@Pipe({ name: 'complaintDate', standalone: true, pure: true })
export class ComplaintDatePipe implements PipeTransform {
  transform(value: string | null | undefined, includeTime = false): string {
    if (!value) return '—';
    const d = new Date(value);
    const dateStr = d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
    if (!includeTime) return dateStr;
    const timeStr = d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
    return `${dateStr} at ${timeStr}`;
  }
}
