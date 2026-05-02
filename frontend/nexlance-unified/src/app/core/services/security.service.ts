import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface Setup2FAResponse {
  qrCode: string;
  secret: string;
  currentCode: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class SecurityService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}`;

  // ==================== PASSWORD MANAGEMENT ====================
  
  /**
   * Change password for current authenticated user
   */
  changePassword(request: ChangePasswordRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.API_URL}/users/me/password`, request);
  }

  /**
   * Request password reset email
   */
  forgotPassword(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.API_URL}/auth/forgot-password`, { email });
  }

  /**
   * Reset password with token
   */
  resetPassword(email: string, token: string, newPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.API_URL}/auth/reset-password`, {
      email,
      token,
      newPassword
    });
  }

  // ==================== 2FA MANAGEMENT ====================
  
  /**
   * Setup 2FA - Get QR code and secret
   */
  setup2FA(): Observable<Setup2FAResponse> {
    return this.http.post<Setup2FAResponse>(`${this.API_URL}/auth/2fa/setup`, {});
  }

  /**
   * Enable 2FA after verification
   */
  enable2FA(verificationCode: string): Observable<{ message: string; backupCodes: string[] }> {
    return this.http.post<{ message: string; backupCodes: string[] }>(`${this.API_URL}/auth/2fa/enable`, { verificationCode });
  }

  /**
   * Disable 2FA
   */
  disable2FA(password: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.API_URL}/auth/2fa/disable`, { password });
  }

  /**
   * Verify 2FA code
   */
  verify2FA(code: string): Observable<{ valid: boolean; message: string }> {
    return this.http.post<{ valid: boolean; message: string }>(`${this.API_URL}/auth/2fa/verify`, { code });
  }

  /**
   * Regenerate backup codes
   */
  regenerateBackupCodes(password: string): Observable<{ message: string; backupCodes: string[] }> {
    return this.http.post<{ message: string; backupCodes: string[] }>(`${this.API_URL}/auth/2fa/regenerate-backup-codes`, { password });
  }

  /**
   * Get 2FA status for current user
   */
  get2FAStatus(): Observable<{ enabled: boolean }> {
    return this.http.get<{ enabled: boolean }>(`${this.API_URL}/auth/2fa/status`);
  }
}
