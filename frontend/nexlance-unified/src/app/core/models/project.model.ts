export enum ProjectStatus {
  ACTIVE = 'ACTIVE',
  ON_HOLD = 'ON_HOLD',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export enum MilestoneStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  SUBMITTED = 'SUBMITTED',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED'
}

export interface ProjectMilestone {
  id?: string;
  projectId?: string;
  title: string;
  description: string;
  orderIndex: number;
  dueDate: Date | string;
  status: MilestoneStatus;
  deliverables?: string; // JSON Array stored as String
  acceptanceCriteria?: string;
  submittedAt?: Date | string;
  approvedAt?: Date | string;
  rejectionReason?: string;
  attachments?: string; // JSON Array stored as String
  requiresDocuments?: boolean;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

export interface Project {
  id?: string;
  jobOfferId?: string;
  title?: string;
  freelanceId?: string;
  clientId?: string;
  startDate: Date | string;
  endDate: Date | string;
  status: ProjectStatus;
  progress?: number; // 0-100
  milestones?: ProjectMilestone[];
  requirements?: string;
  deliverables?: string; // JSON Array stored as String
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

export interface CreateProjectRequest {
  jobOfferId?: string;
  title?: string;
  freelanceId?: string;
  clientId?: string;
  freelanceEmail?: string;
  clientEmail?: string;
  startDate: Date | string;
  endDate: Date | string;
  requirements?: string;
  deliverables?: string;
  milestones?: Omit<ProjectMilestone, 'id' | 'projectId' | 'createdAt' | 'updatedAt'>[];
}

export interface MilestoneSubmission {
  attachments: string; // JSON Array
  comment?: string;
}

export interface MilestoneReview {
  approved: boolean;
  rejectionReason?: string;
}

export interface ProjectStats {
  totalProjects: number;
  activeProjects: number;
  completedProjects: number;
  totalMilestones: number;
  approvedMilestones: number;
  pendingMilestones: number;
  overdueProjects: number;
}

export interface MilestoneStats {
  totalMilestones: number;
  approvalRateFirstAttempt: number;
  averageValidationTime: number;
  averageRevisionsPerMilestone: number;
}
