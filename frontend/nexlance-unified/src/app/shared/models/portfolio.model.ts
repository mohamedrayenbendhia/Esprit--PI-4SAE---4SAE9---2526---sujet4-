export interface Project {
  id: number;
  title: string;
  description?: string;
  techStack?: string;
  startDate?: string;
  endDate?: string;
  githubUrl?: string;
  demoUrl?: string;
  images?: string;
}

export interface ProjectRequest {
  title: string;
  description?: string;
  techStack?: string;
  startDate?: string;
  endDate?: string;
  githubUrl?: string;
  demoUrl?: string;
  images?: string;
}

// ── Portfolio
export interface Portfolio {
  id: number;
  userId: string;
  headline: string;
  linkedinUrl?: string;
  githubUrl?: string;
  location?: string;
  isPublic: boolean;
  viewsCount: number;
  projects: Project[];
}

export interface PortfolioRequest {
  headline: string;
  linkedinUrl?: string;
  githubUrl?: string;
  location?: string;
  projects?: ProjectRequest[];
}

export interface PortfolioUpdateRequest {
  headline: string;
  linkedinUrl?: string;
  githubUrl?: string;
  location?: string;
  isPublic?: boolean;  // ← ajoute
  projects?: ProjectRequest[];
}