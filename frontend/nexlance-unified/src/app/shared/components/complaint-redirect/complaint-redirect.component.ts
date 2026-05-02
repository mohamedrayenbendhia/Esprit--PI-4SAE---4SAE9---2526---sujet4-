import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserRole } from '../../models/user.model';

@Component({
  standalone: true,
  template: ''
})
export class ComplaintRedirectComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    const id   = this.route.snapshot.paramMap.get('id');
    const user = this.auth.getCurrentUser();

    switch (user?.role) {
      case UserRole.ADMIN:
        this.router.navigate([`/backoffice/admin/complaints/${id}`]);
        break;
      case UserRole.SUPPORT_AGENT:
        this.router.navigate([`/backoffice/agent/complaints/${id}`]);
        break;
      case UserRole.FREELANCER:
        this.router.navigate([`/frontoffice/freelancer/my-complaints/${id}`]);
        break;
      default:
        this.router.navigate([`/frontoffice/client/my-complaints/${id}`]);
    }
  }
}
