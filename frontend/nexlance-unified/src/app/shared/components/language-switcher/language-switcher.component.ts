import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { TranslateService, TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatMenuModule,
    MatIconModule,
    TranslateModule
  ],
  template: `
    <button mat-button [matMenuTriggerFor]="languageMenu" class="language-btn">
      <span class="flag-icon">{{ currentLang === 'fr' ? '🇫🇷' : '🇬🇧' }}</span>
      <span class="lang-code">{{ currentLang === 'fr' ? 'FR' : 'EN' }}</span>
      <mat-icon class="dropdown-icon">arrow_drop_down</mat-icon>
    </button>
    
    <mat-menu #languageMenu="matMenu" class="language-menu">
      <button mat-menu-item (click)="changeLanguage('fr')" [class.active]="currentLang === 'fr'">
        <span class="flag">🇫🇷</span>
        <span class="lang-text">Français</span>
        <mat-icon *ngIf="currentLang === 'fr'" class="check-icon">check</mat-icon>
      </button>
      <button mat-menu-item (click)="changeLanguage('en')" [class.active]="currentLang === 'en'">
        <span class="flag">🇬🇧</span>
        <span class="lang-text">English</span>
        <mat-icon *ngIf="currentLang === 'en'" class="check-icon">check</mat-icon>
      </button>
    </mat-menu>
  `,
  styles: [`
    .language-btn {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      min-width: auto;
      height: 36px;
      border-radius: 18px;
      background-color: rgba(255, 255, 255, 0.1);
      transition: all 0.3s ease;
      color: white;
      
      &:hover {
        background-color: rgba(255, 255, 255, 0.2);
        transform: translateY(-1px);
        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
      }
      
      .flag-icon {
        font-size: 18px;
        line-height: 1;
      }
      
      .lang-code {
        font-size: 13px;
        font-weight: 600;
        letter-spacing: 0.5px;
      }
      
      .dropdown-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
        margin-left: -2px;
      }
    }

    ::ng-deep .language-menu {
      .mat-mdc-menu-content {
        padding: 4px 0;
        min-width: 180px;
      }

      button[mat-menu-item] {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px 16px;
        min-height: 44px;
        
        &.active {
          background-color: rgba(63, 81, 181, 0.08);
          
          .lang-text {
            color: #3f51b5;
            font-weight: 600;
          }
        }
        
        &:hover {
          background-color: rgba(0, 0, 0, 0.04);
        }
        
        .flag {
          font-size: 20px;
          line-height: 1;
        }
        
        .lang-text {
          flex: 1;
          font-size: 14px;
        }
        
        .check-icon {
          color: #3f51b5;
          font-size: 20px;
          width: 20px;
          height: 20px;
          margin-left: auto;
        }
      }
    }
  `]
})
export class LanguageSwitcherComponent {
  private translate = inject(TranslateService);
  
  currentLang: string;

  constructor() {
    this.currentLang = this.translate.currentLang || this.translate.defaultLang || 'en';
    
    // Subscribe to language changes to update the button
    this.translate.onLangChange.subscribe((event) => {
      this.currentLang = event.lang;
    });
  }

  changeLanguage(lang: string): void {
    this.translate.use(lang);
    this.currentLang = lang;
    localStorage.setItem('language', lang);
  }
}
