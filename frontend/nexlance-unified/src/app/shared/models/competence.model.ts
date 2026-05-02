// ── Enums
export enum SkillLevel {
  BEGINNER = 'BEGINNER',
  INTERMEDIATE = 'INTERMEDIATE',
  EXPERT = 'EXPERT'
}

// ── Skill (Admin)
export interface Skill {
  id: number;
  name: string;
  description?: string;
  category: string;
  iconUrl?: string;
}

export interface SkillRequest {
  name: string;
  description?: string;
  category: string;
  iconUrl?: string;
}

export interface EndorsementStatus {
  endorsements: number;
  hasEndorsed: boolean;
}

// ── UserSkill (Freelancer)
export interface UserSkill {
  id: number;
  userId: string;
  skill: {
    id: number;
    name: string;
    category?: string;
  };
  level: SkillLevel;
  yearsOfExperience: number;
}

export interface UserSkillRequest {
  skillId: number;
  level: SkillLevel;
  yearsOfExperience: number;
  description?: string;
}

export interface UserSkillUpdateRequest {
  level: SkillLevel;
  yearsOfExperience: number;
  description?: string;
}