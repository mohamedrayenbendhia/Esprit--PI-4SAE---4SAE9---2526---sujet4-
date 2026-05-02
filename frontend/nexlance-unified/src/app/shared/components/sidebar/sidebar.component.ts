import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../models/user.model';
import { LanguageSwitcherComponent } from '../language-switcher/language-switcher.component';

const ROUTE_GROUP_MAP: Record<string, string> = {
  'users': 'users',
  'kyc': 'users',
  'jobs': 'jobs',
  'projects': 'projects',
  'recommendations': 'recommendations',
  'complaints': 'complaints',
};

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatTooltipModule,
    TranslateModule,
    LanguageSwitcherComponent
  ],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  private authService = inject(AuthService);
  private router      = inject(Router);
  private destroyRef  = inject(DestroyRef);

  currentUser = signal<User | null>(null);
  isCollapsed = signal(false);

  expandedGroups = signal<Record<string, boolean>>({
    users:           false,
    jobs:            false,
    projects:        false,
    recommendations: false,
    aboutme:         false,
    complaints:      false,
  });

  constructor() {
    this.authService.currentUser$.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(user => {
      this.currentUser.set(user);
    });

    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((e: any) => {
      this.expandGroupForUrl(e.urlAfterRedirects ?? e.url);
    });

    this.expandGroupForUrl(this.router.url);
  }

  private expandGroupForUrl(url: string): void {
    const segments = url.split('/').filter(Boolean);
    for (const seg of segments) {
      const group = ROUTE_GROUP_MAP[seg];
      if (group) {
        this.expandedGroups.update(g => ({ ...g, [group]: true }));
        break;
      }
    }
  }

  toggleGroup(group: string): void {
    if (this.isCollapsed()) return;
    this.expandedGroups.update(groups => ({
      ...groups,
      [group]: !groups[group]
    }));
  }

  isGroupExpanded(group: string): boolean {
    return this.expandedGroups()[group] ?? false;
  }

  isAdmin(): boolean {
    const t = this.currentUser()?.type ?? (this.currentUser() as any)?.role;
    return t === 'ADMIN';
  }

  isAgent(): boolean {
    const t = this.currentUser()?.type ?? (this.currentUser() as any)?.role;
    return t === 'SUPPORT_AGENT';
  }

  get userRoleLabelKey(): string {
    const t = this.currentUser()?.type ?? (this.currentUser() as any)?.role;
    switch (t) {
      case 'ADMIN':         return 'nav.administrator';
      case 'SUPPORT_AGENT': return 'nav.supportAgent';
      case 'CLIENT':        return 'nav.client';
      case 'FREELANCE':     return 'nav.freelance';
      default:              return 'nav.administrator';
    }
  }

  get userInitials(): string {
    const user = this.currentUser();
    if (!user) return '';
    const firstInitial = user.firstName?.charAt(0) || '';
    const lastInitial = user.lastName?.charAt(0) || '';
    return `${firstInitial}${lastInitial}`.toUpperCase();
  }

  get userFullName(): string {
    const user = this.currentUser();
    if (!user) return '';
    return `${user.firstName} ${user.lastName}`;
  }

  toggleSidebar(): void {
    this.isCollapsed.update(value => !value);
  }

  logout(): void {
    this.authService.logout();
  }
}
