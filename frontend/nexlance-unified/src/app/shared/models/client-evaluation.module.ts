import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  {
    path: 'evaluations-given',
    loadComponent: () =>
      import('@frontoffice/client/client-dashboard/evaluation/evaluations-given/evaluations-given')
        .then(m => m.EvaluationsGivenComponent)
  },
  {
    path: 'evaluate/:freelancerId/:projectId',
    loadComponent: () =>
      import('@frontoffice/client/client-dashboard/evaluation/evaluate-freelancer/evaluate')
        .then(m => m.EvaluateFreelancerComponent)
  },
  {
    path: 'freelancer/:freelancerId/evaluations',
    loadComponent: () =>
      import('@frontoffice/client/client-dashboard/evaluation/freelancer-evaluations-view/freelancer-evaluations')
        .then(m => m.FreelancerEvaluationsViewComponent)
  }
];

@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild(routes)
  ]
})
export class ClientEvaluationModule {}