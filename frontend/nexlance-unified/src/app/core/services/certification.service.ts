import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { Observable } from 'rxjs';
import {
  Test, TestRequest, TestPublic, Question, QuestionRequest,
  SubmitTestRequest, UserTestResult, Certification
} from '../../shared/models/certification.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CertificationService {
  private http = inject(HttpClient);
  private readonly API = environment.certificationApiUrl;

  // ── Admin - Tests
  getAllTests(): Observable<Test[]> {
    return this.http.get<Test[]>(`${this.API}/admin/tests`);
  }

  getTestById(id: number): Observable<Test> {
    return this.http.get<Test>(`${this.API}/admin/tests/${id}`);
  }

  createTest(request: TestRequest): Observable<Test> {
    return this.http.post<Test>(`${this.API}/admin/tests`, request);
  }

  getAllCertifications(): Observable<Certification[]> {
  return this.http.get<Certification[]>(`${this.API}/admin/certifications`);
}

  updateTest(id: number, request: TestRequest): Observable<Test> {
    return this.http.put<Test>(`${this.API}/admin/tests/${id}`, request);
  }

  deleteTest(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/admin/tests/${id}`);
  }

  addQuestion(testId: number, request: QuestionRequest): Observable<Question> {
    return this.http.post<Question>(`${this.API}/admin/tests/${testId}/questions`, request);
  }

  updateQuestion(id: number, request: QuestionRequest): Observable<Question> {
    return this.http.put<Question>(`${this.API}/admin/tests/questions/${id}`, request);
  }

  deleteQuestion(id: number): Observable<void> {
  return this.http.delete<void>(`${this.API}/admin/tests/questions/${id}`);
  }

  // ── Freelancer
  getTestBySkill(skillId: number): Observable<TestPublic> {
    return this.http.get<TestPublic>(`${this.API}/freelancer/tests/skill/${skillId}`);
  }

  getMyResults(): Observable<UserTestResult[]> {
    return this.http.get<UserTestResult[]>(`${this.API}/freelancer/tests/results/me`);
  }

  submitTest(request: SubmitTestRequest): Observable<UserTestResult> {
    return this.http.post<UserTestResult>(`${this.API}/freelancer/tests/submit`, request);
  }

  getMyCertifications(): Observable<Certification[]> {
    return this.http.get<Certification[]>(`${this.API}/freelancer/certifications/me`);
  }

  // ── Client
  getFreelancerCertifications(userId: string): Observable<Certification[]> {
    return this.http.get<Certification[]>(`${this.API}/client/certifications/${userId}`);
  }
  checkCooldown(skillId: number): Observable<{onCooldown: boolean, lastAttemptAt?: string}> {
  return this.http.get<{onCooldown: boolean, lastAttemptAt?: string}>(`${this.API}/freelancer/tests/cooldown/${skillId}`);
}

  generateQuestionsWithAI(testId: number, count: number = 5, skillName: string = ''): Observable<Question[]> {
    return this.http.post<Question[]>(
      `${this.API}/admin/tests/${testId}/ai-generate?count=${count}&skillName=${encodeURIComponent(skillName)}`,
      {}
    );
  }
}