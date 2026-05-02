import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@frontoffice/client/client-dashboard/badges/badges-list/badges-list')
        .then(m => m.BadgesListComponent)
  },
  {
    path: 'freelancer/:freelancerId',
    loadComponent: () =>
      import('@frontoffice/client/client-dashboard/badges/freelancer-badges-view/freelancer-badges-view')
        .then(m => m.FreelancerBadgesViewComponent)
  }
];

@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild(routes)
  ]
})
export class ClientBadgesModule {}