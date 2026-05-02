import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageSwitcherComponent } from '../../../shared/components/language-switcher/language-switcher.component';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    TranslateModule,
    LanguageSwitcherComponent
  ],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss']
})
export class LandingComponent {
  
  features = [
    {
      key: 'security',
      icon: 'verified_user'
    },
    {
      key: 'matching',
      icon: 'trending_up'
    },
    {
      key: 'payment',
      icon: 'payment'
    },
    {
      key: 'support',
      icon: 'support_agent'
    }
  ];

  stats = [
    { value: '10K+', key: 'freelancers' },
    { value: '25K+', key: 'projects' },
    { value: '98%', key: 'satisfaction' },
    { value: '150+', key: 'categories' }
  ];

  categories = [
    { key: 'webDev', icon: 'code', color: '#667eea' },
    { key: 'design', icon: 'palette', color: '#f093fb' },
    { key: 'marketing', icon: 'campaign', color: '#4facfe' },
    { key: 'writing', icon: 'edit_note', color: '#43e97b' },
    { key: 'translation', icon: 'translate', color: '#fa709a' },
    { key: 'video', icon: 'videocam', color: '#fee140' }
  ];

  trustedBy = [
    'Google', 'Microsoft', 'Amazon', 'PayPal', 'L\'Oréal', 'Orange'
  ];

  constructor(private router: Router) {}

  navigateToRegister(role: string): void {
    this.router.navigate(['/register', role]);
  }

  navigateToLogin(): void {
    this.router.navigate(['/login']);
  }
}
