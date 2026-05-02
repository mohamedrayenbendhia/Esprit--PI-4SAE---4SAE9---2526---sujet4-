export enum JobCategory {
  DEVELOPMENT = 'DEVELOPMENT',
  DESIGN = 'DESIGN',
  MARKETING = 'MARKETING',
  WRITING = 'WRITING',
  OTHER = 'OTHER'
}

export enum BudgetType {
  FIXED = 'FIXED',
  HOURLY = 'HOURLY'
}

export enum JobOfferStatus {
  DRAFT = 'DRAFT',
  OPEN = 'OPEN',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
  ARCHIVED = 'ARCHIVED'
}

export enum ExperienceLevel {
  BEGINNER = 'BEGINNER',
  INTERMEDIATE = 'INTERMEDIATE',
  EXPERT = 'EXPERT'
}

export interface Attachment {
  id: string;
  fileName: string;
  fileUrl: string;
  fileType: string;
  fileSize: number;
  uploadedAt: Date;
}

export interface JobOffer {
  id: string;
  clientId: string;
  title: string;
  description: string;
  category: JobCategory;
  budget: number;
  budgetType: BudgetType;
  estimatedDuration: number; // en jours
  deadline: Date;
  status: JobOfferStatus;
  requiredSkills: string[];
  experienceLevel: ExperienceLevel;
  location: string;
  isRemote: boolean;
  attachments: string[]; // Array of file URLs (backend stores as List<String>)
  viewCount: number;
  applicantCount: number;
  publishedAt?: Date;
  createdAt: Date;
  updatedAt: Date;
  
  // Relations
  client?: any; // User model
  applications?: any[]; // Application model
}

export interface CreateJobOfferDto {
  clientId: string; // UUID of the client creating the offer
  title: string;
  description: string;
  category: JobCategory;
  budget: number;
  budgetType: BudgetType;
  estimatedDuration: number;
  deadline: Date;
  requiredSkills: string[];
  experienceLevel: ExperienceLevel;
  location: string;
  isRemote: boolean;
  attachments?: string[]; // Array of file URLs returned from upload endpoint
  status: JobOfferStatus; // 'draft' ou 'open'
}

export interface UpdateJobOfferDto {
  title?: string;
  description?: string;
  category?: JobCategory;
  budget?: number;
  budgetType?: BudgetType;
  estimatedDuration?: number;
  deadline?: Date;
  requiredSkills?: string[];
  experienceLevel?: ExperienceLevel;
  location?: string;
  isRemote?: boolean;
  attachments?: Attachment[];
  status?: JobOfferStatus;
}

export interface JobOfferFilters {
  category?: JobCategory;
  minBudget?: number;
  maxBudget?: number;
  budgetType?: BudgetType;
  experienceLevel?: ExperienceLevel;
  location?: string;
  isRemote?: boolean;
  status?: JobOfferStatus;
  search?: string;
  publishedAfter?: Date;
  skillsRequired?: string[];
  sortBy?: 'publishedAt' | 'budget' | 'deadline' | 'applicantCount';
  sortOrder?: 'asc' | 'desc';
  page?: number;
  size?: number;
}

export interface JobOfferStats {
  totalOffers: number;
  totalApplications: number;
  conversionRate: number;
  avgApplicationsPerOffer: number;
  avgResponseTime: number;
}
