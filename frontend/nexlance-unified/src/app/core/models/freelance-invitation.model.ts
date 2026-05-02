export interface FreelanceInvitation {
  id: string;
  jobOfferId: string;
  freelanceId: string;
  clientId: string;
  message: string;
  proposedBudget?: number;
  deadlineResponse?: Date;
  status: InvitationStatus;
  createdAt: Date;
  respondedAt?: Date;
  
  // Relations (populated)
  jobOffer?: any;
  freelance?: any;
  client?: any;
}

export enum InvitationStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  DECLINED = 'DECLINED',
  EXPIRED = 'EXPIRED'
}

export interface CreateInvitationDto {
  clientId: string;
  freelanceId: string;
  jobOfferId: string;
  message: string;
  proposedBudget?: number;
  deadlineResponse?: Date;
}

export interface UpdateInvitationDto {
  status: InvitationStatus;
  message?: string;
}

export interface InvitationFilters {
  status?: InvitationStatus;
  jobOfferId?: string;
  freelanceId?: string;
  clientId?: string;
  dateFrom?: Date;
  dateTo?: Date;
}
