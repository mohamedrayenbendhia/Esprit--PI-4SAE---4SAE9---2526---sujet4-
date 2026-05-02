export interface QuestionRequest {
  questionText: string;
  correctAnswer: string;
}

export interface Question {
  id: number;
  questionText: string;
  correctAnswer: string;
}

// ── Test
export interface Test {
  id: number;
  title: string;
  description?: string;
  skillId: number;
  skillName?: string;
  passingScore: number;
  questions: Question[];
}

export interface TestRequest {
  title: string;
  skillId: number;
  passingScore: number;
}

// ── TestPublic (sans correctAnswer)
export interface TestPublic {
  id: number;
  title: string;
  description?: string;
  passingScore: number;
  questions: Question[];
}

// ── Submit Test
export interface SubmitTestRequest {
  userId?: string;
  userSkillId: number;
  answers: string[];
}

// ── UserAnswer
export interface UserAnswer {
  id: number;
  questionText: string;
  yourAnswer: string;
  isCorrect: boolean;
}

// ── UserTestResult
export interface UserTestResult {
  id: number;
  userId: string;
  testId: number;
  testTitle: string;
  userSkillId: number;
  score: number;
  passingScore: number;
  isPassed: boolean;
  passedAt: string;
  cooldownMessage?: string;
  onCooldown?: boolean;
  answers: UserAnswer[];
}

// ── Certification
export interface Certification {
  id: number;
  userId: string;
  userSkillId: number;
  testTitle: string;
  score: number;
  date: string;
  certificateUrl?: string;
  isExpired?: boolean;
  expiresAt?: string;
}