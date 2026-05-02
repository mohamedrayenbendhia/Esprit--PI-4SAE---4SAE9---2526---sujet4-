// Backend uses UserType internally
export enum UserType {
  FREELANCE = 'FREELANCE',
  CLIENT = 'CLIENT',
  ADMIN = 'ADMIN',
  SUPPORT_AGENT = 'SUPPORT_AGENT'
}

// Frontend convenience enum (maps to UserType)
export enum UserRole {
  CLIENT = 'CLIENT',
  FREELANCER = 'FREELANCE',  // Maps to UserType.FREELANCE on backend
  ADMIN = 'ADMIN',
  SUPPORT_AGENT = 'SUPPORT_AGENT'  // Agent de support — module réclamations (Emmanuel)
}

export enum UserStatus {
  ACTIVE = 'ACTIVE',
  SUSPENDED = 'SUSPENDED',
  PENDING_VERIFICATION = 'PENDING_VERIFICATION',
  DELETED = 'DELETED'
}

export enum SubscriptionType {
  FREE = 'FREE',
  PREMIUM = 'PREMIUM',
  ENTERPRISE = 'ENTERPRISE'
}

export interface User {
  id: string;
  email: string;
  password?: string;
  type?: UserType;
  role: UserRole;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  avatar?: string;
  status: UserStatus;
  subscriptionType?: SubscriptionType;
  emailVerified: boolean;
  createdAt?: Date;
  updatedAt?: Date;
  lastLoginAt?: Date;
  lastLogin?: Date;  // Alias for compatibility
}

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterRequest {
  email: string;
  password: string;
  confirmPassword: string;
  type?: UserType;
  role: UserRole;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  acceptTerms?: boolean;
}

export interface LoginResponse {
  token: string;
  refreshToken?: string;
  user: User;
}

export interface AuthResponse {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role?: UserRole;  // Backend peut retourner role ou type
  type?: string;    // Certains endpoints retournent type au lieu de role
  status: string;
  emailVerified: boolean;
  token: string;
  avatar?: string;
}
