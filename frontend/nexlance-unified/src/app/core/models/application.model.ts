export enum ApplicationStatus {
  PENDING = 'PENDING',
  SHORTLISTED = 'SHORTLISTED',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED',
  WITHDRAWN = 'WITHDRAWN'
}

export interface PortfolioItem {
  id: string;
  title: string;
  description: string;
  imageUrl?: string;
  projectUrl?: string;
  technologies?: string[];
}

export interface Application {
  id: string;
  jobOfferId: string;
  freelanceId: string;
  coverLetter: string;
  proposedRate: number;
  estimatedDelivery: Date;
  status: ApplicationStatus;
  portfolioItems: PortfolioItem[];
  availableFrom: Date;
  isRead: boolean;
  submittedAt: Date;
  respondedAt?: Date;
  createdAt: Date;
  
  // Relations
  jobOffer?: any; // JobOffer model
  freelance?: any; // User model
}

export interface CreateApplicationDto {
  jobOfferId: string;
  freelanceId: string;
  coverLetter: string;
  proposedRate: number;
  estimatedDelivery: Date;
  portfolioItems: PortfolioItem[];
  availableFrom: Date;
}

export interface UpdateApplicationDto {
  coverLetter?: string;
  proposedRate?: number;
  estimatedDelivery?: Date;
  portfolioItems?: PortfolioItem[];
  availableFrom?: Date;
  status?: ApplicationStatus;
}

export interface ApplicationFilters {
  status?: ApplicationStatus;
  jobOfferId?: string;
  freelanceId?: string;
  isRead?: boolean;
  submittedAfter?: Date;
  submittedBefore?: Date;
  sortBy?: 'submittedAt' | 'proposedRate';
  sortOrder?: 'asc' | 'desc';
  page?: number;
  size?: number;
}
