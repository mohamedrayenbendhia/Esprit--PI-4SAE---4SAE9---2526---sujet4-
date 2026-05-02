import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Skill, SkillRequest, UserSkill, UserSkillRequest, UserSkillUpdateRequest } from '../../shared/models/competence.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CompetenceService {
  private http = inject(HttpClient);
  private readonly API = environment.competenceApiUrl;

  // ── Admin - Skills
  getAllSkills(): Observable<Skill[]> {
    return this.http.get<Skill[]>(`${this.API}/admin/skills`);
  }

  createSkill(request: SkillRequest): Observable<Skill> {
    return this.http.post<Skill>(`${this.API}/admin/skills`, request);
  }

  updateSkill(id: number, request: SkillRequest): Observable<Skill> {
    return this.http.put<Skill>(`${this.API}/admin/skills/${id}`, request);
  }

  deleteSkill(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/admin/skills/${id}`);
  }

  // ── Freelancer - My Skills
  // Freelancer
getMySkills(): Observable<UserSkill[]> {
  return this.http.get<UserSkill[]>(`${this.API}/freelancer/skills/user-skills/me`);
}

addSkill(request: UserSkillRequest): Observable<UserSkill> {
  return this.http.post<UserSkill>(`${this.API}/freelancer/skills/user-skills`, request);
}

updateMySkill(id: number, request: UserSkillRequest): Observable<UserSkill> {
  return this.http.put<UserSkill>(`${this.API}/freelancer/skills/user-skills/${id}`, request);
}

deleteMySkill(id: number): Observable<void> {
  return this.http.delete<void>(`${this.API}/freelancer/skills/user-skills/${id}`);
}

getAvailableSkills(): Observable<Skill[]> {
  return this.http.get<Skill[]>(`${this.API}/freelancer/skills`);
}

  // ── Client - View freelancer skills
  getFreelancerSkills(userId: string): Observable<UserSkill[]> {
    return this.http.get<UserSkill[]>(`${this.API}/client/skills/user-skills/${userId}`);
  }

  // Client
getAllSkillsClient(): Observable<Skill[]> {
  return this.http.get<Skill[]>(`${this.API}/client/skills`);
}

getFreelancersBySkill(skillId: number): Observable<UserSkill[]> {
  return this.http.get<UserSkill[]>(`${this.API}/client/skills/user-skills/skill/${skillId}`);
}

endorseSkill(freelancerId: string, skillId: number): Observable<{endorsements: number}> {
  return this.http.post<{endorsements: number}>(`${this.API}/client/skills/endorse/${freelancerId}/${skillId}`, {});
}

removeEndorsement(freelancerId: string, skillId: number): Observable<{endorsements: number}> {
  return this.http.delete<{endorsements: number}>(`${this.API}/client/skills/endorse/${freelancerId}/${skillId}`);
}

getEndorsementCount(freelancerId: string, skillId: number): Observable<{endorsements: number}> {
  return this.http.get<{endorsements: number}>(`${this.API}/client/skills/endorse/${freelancerId}/${skillId}/count`);
}

hasEndorsed(freelancerId: string, skillId: number): Observable<{hasEndorsed: boolean}> {
  return this.http.get<{hasEndorsed: boolean}>(`${this.API}/client/skills/endorse/${freelancerId}/${skillId}/has-endorsed`);
}

getSkillStats(): Observable<any[]> {
  return this.http.get<any[]>(`${this.API}/admin/skills/stats`);
}

getGlobalStats(): Observable<any> {
  return this.http.get<any>(`${this.API}/admin/skills/stats/global`);
}

}