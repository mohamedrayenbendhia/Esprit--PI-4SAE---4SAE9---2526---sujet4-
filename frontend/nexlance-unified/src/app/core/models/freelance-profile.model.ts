export enum Availability {
  AVAILABLE = 'AVAILABLE',
  BUSY = 'BUSY',
  UNAVAILABLE = 'UNAVAILABLE'
}

export interface FreelanceProfile {
  id: string;
  userId: string;
  title: string;
  bio: string;
  hourlyRate: number;
  availability: Availability;
  experienceYears: number;
  portfolioUrl?: string;
  linkedInUrl?: string;
  githubUrl?: string;
  location: string;
  languages: string[];
  timezone: string;
  completionRate: number;
  responseTime: number; // en heures
  skills?: string[];
  certifications?: string[];
  // User info from joined User entity
  firstName?: string;
  lastName?: string;
  email?: string;
  avatar?: string;
}

export interface CreateFreelanceProfileRequest {
  title: string;
  bio: string;
  hourlyRate: number;
  availability: Availability;
  experienceYears: number;
  portfolioUrl?: string;
  linkedInUrl?: string;
  githubUrl?: string;
  location: string;
  languages: string[];
  timezone: string;
  skills?: string[];
}
