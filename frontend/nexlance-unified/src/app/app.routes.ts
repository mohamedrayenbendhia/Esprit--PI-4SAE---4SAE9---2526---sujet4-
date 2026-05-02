 import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard, clientGuard, freelancerGuard, supportAgentGuard } from './core/guards/role.guard';
import { UserRole } from './shared/models/user.model';

// Import des composants
import { LoginComponent } from './shared/modules/user/login/login.component';
import { RegisterComponent } from './shared/modules/user/register/register.component';
import { BackofficeLayoutComponent } from './backoffice/layout/backoffice-layout.component';
import { FrontofficeLayoutComponent } from './frontoffice/layout/frontoffice-layout.component';
import { AdminDashboardComponent } from './backoffice/admin/admin-dashboard/admin-dashboard.component';
import { ClientDashboardComponent } from './frontoffice/client/client-dashboard/client-dashboard.component';
import { FreelancerDashboardComponent } from './frontoffice/freelancer/freelancer-dashboard/freelancer-dashboard.component';
import { LandingComponent } from './frontoffice/pages/landing/landing.component';

export const routes: Routes = [
  { path: '', redirectTo: '/landing', pathMatch: 'full' },
  { path: 'landing', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register/client', component: RegisterComponent, data: { role: UserRole.CLIENT } },
  { path: 'register/freelancer', component: RegisterComponent, data: { role: UserRole.FREELANCER } },
  { path: 'register/admin', component: RegisterComponent, data: { role: UserRole.ADMIN } },
  {
    path: 'forgot-password',
    loadComponent: () => import('./shared/modules/user/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./shared/modules/user/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },

  // ── Organisations — pages publiques (sans auth) ─────────────────────────
  { path: 'organizations', redirectTo: '/frontoffice/organizations', pathMatch: 'full' },
  { path: 'organizations/:id', redirectTo: '/frontoffice/organizations/:id', pathMatch: 'full' },
  // Lien email — répondre à une invitation par token (sans auth)
  {
    path: 'invitations/respond',
    loadComponent: () => import('./frontoffice/organizations/invitation-respond/invitation-respond.component').then(m => m.InvitationRespondComponent)
  },

  // ── Redirections vers les routes avec layout ────────────────────────────
  { path: 'profile',              redirectTo: '/frontoffice/profile',              pathMatch: 'full' },
  { path: 'security',             redirectTo: '/frontoffice/security',             pathMatch: 'full' },
  { path: 'profile/kyc',          redirectTo: '/frontoffice/kyc',                  pathMatch: 'full' },
  { path: 'profile/professional', redirectTo: '/frontoffice/profile/professional', pathMatch: 'full' },

  // ============================================================
  // BACK OFFICE — Admin + Agent
  // ============================================================
  {
    path: 'backoffice',
    component: BackofficeLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'admin/dashboard', pathMatch: 'full' },

      // ── Admin — Dashboard & User Management ──────────────────
      { path: 'admin/dashboard', component: AdminDashboardComponent, canActivate: [adminGuard] },
      {
        path: 'admin/users',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/user-management/user-management.component').then(m => m.UserManagementComponent)
      },
      {
        path: 'admin/kyc',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/kyc-verification/kyc-verification.component').then(m => m.KycVerificationComponent)
      },
      {
        path: 'admin/security',
        canActivate: [adminGuard],
        loadComponent: () => import('./shared/modules/user/security-settings/security-settings.component').then(m => m.SecuritySettingsComponent)
      },
      {
        path: 'admin/profile',
        canActivate: [adminGuard],
        loadComponent: () => import('./shared/modules/user/profile/profile.component').then(m => m.ProfileComponent)
      },

      // ── Admin — Module Jobs ──────────────────────────────────
      {
        path: 'admin/jobs',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/admin-jobs/admin-jobs.component').then(m => m.AdminJobsComponent)
      },
      {
        path: 'admin/jobs/:id',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/admin-job-detail/admin-job-detail.component').then(m => m.AdminJobDetailComponent)
      },
      {
        path: 'admin/jobs/:id/moderate',
        canActivate: [adminGuard],
        loadComponent: () => import('./frontoffice/client/job-detail-client/job-detail-client.component').then(m => m.JobDetailClientComponent)
      },
      {
        path: 'admin/analytics/jobs',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/job-analytics/job-analytics.component').then(m => m.JobAnalyticsComponent)
      },

      // ── Admin — Module Projects ──────────────────────────────
      {
        path: 'admin/projects',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/admin-projects/admin-projects.component').then(m => m.AdminProjectsComponent)
      },
      {
        path: 'admin/projects/:id',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/admin-project-detail/admin-project-detail.component').then(m => m.AdminProjectDetailComponent)
      },
      {
        path: 'admin/projects/:id/milestones/:milestoneId/mediate',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/milestone-mediation/milestone-mediation.component').then(m => m.MilestoneMediationComponent)
      },
      {
        path: 'admin/analytics/milestones',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/milestone-analytics/milestone-analytics.component').then(m => m.MilestoneAnalyticsComponent)
      },

      // ── Admin — Module Recommendations ───────────────────────
      {
        path: 'admin/recommendations',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/admin-recommendations/admin-recommendations.component').then(m => m.AdminRecommendationsComponent)
      },
      {
        path: 'admin/analytics/recommendations',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/recommendation-analytics/recommendation-analytics.component').then(m => m.RecommendationAnalyticsComponent)
      },

      // ── Admin — About Me (Skills, Portfolios, Certifications, Tests) ──
      {
        path: 'admin/skills',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/skills/admin-skills.component').then(m => m.AdminSkillsComponent)
      },
      {
        path: 'admin/portfolios',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/portfolios/admin-portfolios.component').then(m => m.AdminPortfoliosComponent)
      },
      {
        path: 'admin/certifications',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/certifications/admin-certifications.component').then(m => m.AdminCertificationsComponent)
      },
      {
        path: 'admin/tests',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/tests/admin-tests.component').then(m => m.AdminTestsComponent)
      },

      // ── Admin — Module Organisations (Emmanuel) ──────────────
      {
        path: 'admin/organizations',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/organizations/admin-organizations.component').then(m => m.AdminOrganizationsComponent)
      },

      // ── Admin — Module Réclamations (Emmanuel) ───────────────
      {
        path: 'admin/complaints',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/complaints/admin-complaints.component').then(m => m.AdminComplaintsComponent)
      },
      {
        path: 'admin/complaints/stats',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/complaints/admin-complaints-stats.component').then(m => m.AdminComplaintsStatsComponent)
      },
      {
        path: 'admin/complaints/nps',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/complaints/admin-nps-stats.component').then(m => m.AdminNpsStatsComponent)
      },
      {
        path: 'admin/complaints/templates',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/complaints/admin-response-templates.component').then(m => m.AdminResponseTemplatesComponent)
      },
      {
        path: 'admin/complaints/risk',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/complaints/admin-risk-dashboard.component').then(m => m.AdminRiskDashboardComponent)
      },
      {
        path: 'admin/complaints/sla-rules',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/complaints/admin-sla-rules.component').then(m => m.AdminSlaRulesComponent)
      },
      // Alias pour compatibilité avec l'ancienne route /sla
      {
        path: 'admin/complaints/sla',
        redirectTo: 'admin/complaints/sla-rules',
        pathMatch: 'full'
      },
      {
        path: 'admin/complaints/:id',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/complaints/admin-complaint-detail.component').then(m => m.AdminComplaintDetailComponent)
      },

      // ── Admin — Audit Log (Emmanuel) ─────────────────────────
      {
        path: 'admin/audit-log',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/admin/modules/audit-log/audit-log-admin.component').then(m => m.AuditLogAdminComponent)
      },

      // ── Agent — File de réclamations (Emmanuel) ──────────────
      // Nouvelles routes avec supportAgentGuard
      {
        path: 'agent/queue',
        canActivate: [supportAgentGuard],
        loadComponent: () => import('./backoffice/agent/agent-queue.component').then(m => m.AgentQueueComponent)
      },
      {
        path: 'agent/my-assigned',
        canActivate: [supportAgentGuard],
        loadComponent: () => import('./backoffice/agent/agent-my-assigned.component').then(m => m.AgentMyAssignedComponent)
      },
      {
        path: 'agent/complaints/:id',
        canActivate: [supportAgentGuard],
        loadComponent: () => import('./backoffice/agent/agent-complaint-detail.component').then(m => m.AgentComplaintDetailComponent)
      },
      // Aliases admin/agent/* pour compatibilité avec l'ancienne navigation
      {
        path: 'admin/agent/queue',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/agent/agent-queue.component').then(m => m.AgentQueueComponent)
      },
      {
        path: 'admin/agent/assigned',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/agent/agent-my-assigned.component').then(m => m.AgentMyAssignedComponent)
      },
      {
        path: 'admin/agent/complaints/:id',
        canActivate: [adminGuard],
        loadComponent: () => import('./backoffice/agent/agent-complaint-detail.component').then(m => m.AgentComplaintDetailComponent)
      }
    ]
  },

  // ============================================================
  // FRONT OFFICE — Client + Freelancer
  // ============================================================
  {
    path: 'frontoffice',
    component: FrontofficeLayoutComponent,
    canActivate: [authGuard],
    children: [
      // ── Routes communes ──────────────────────────────────────
      {
        path: 'profile',
        loadComponent: () => import('./shared/modules/user/profile/profile.component').then(m => m.ProfileComponent)
      },
      {
        path: 'security',
        loadComponent: () => import('./shared/modules/user/security-settings/security-settings.component').then(m => m.SecuritySettingsComponent)
      },
      {
        path: 'kyc',
        loadComponent: () => import('./shared/modules/user/kyc/user-kyc.component').then(m => m.UserKycComponent)
      },
      {
        path: 'profile/professional',
        canActivate: [freelancerGuard],
        loadComponent: () => import('./frontoffice/freelancer/freelancer-profile/freelancer-profile.component').then(m => m.FreelancerProfileComponent)
      },

      // ── Organisations — profils publics (dans le layout pour la navbar) ──
      {
        path: 'organizations',
        loadComponent: () => import('./frontoffice/organizations/organization-search/organization-search.component').then(m => m.OrganizationSearchComponent)
      },
      {
        path: 'organizations/:id',
        loadComponent: () => import('./frontoffice/organizations/organization-profile/organization-profile.component').then(m => m.OrganizationProfileComponent)
      },
      {
        path: 'organizations/:orgId/collab-offers/new',
        canActivate: [freelancerGuard],
        loadComponent: () => import('./frontoffice/organizations/create-collab-offer/create-collab-offer.component').then(m => m.CreateCollabOfferComponent)
      },

      // ── Organisations — hub centralisé (Emmanuel) ────────────
      {
        path: 'my-organizations',
        loadComponent: () => import('./frontoffice/organizations/my-org-hub/my-org-hub.component').then(m => m.MyOrgHubComponent)
      },
      {
        path: 'my-organizations/create',
        loadComponent: () => import('./frontoffice/organizations/create-organization/create-organization.component').then(m => m.CreateOrganizationComponent)
      },
      {
        path: 'my-organizations/:id/settings',
        loadComponent: () => import('./frontoffice/organizations/organization-settings/organization-settings.component').then(m => m.OrganizationSettingsComponent)
      },
      {
        path: 'my-org-invitations',
        loadComponent: () => import('./frontoffice/organizations/my-org-invitations/my-org-invitations.component').then(m => m.MyOrgInvitationsComponent)
      },
      {
        path: 'my-org-applications',
        loadComponent: () => import('./frontoffice/organizations/my-org-applications/my-org-applications.component').then(m => m.MyOrgApplicationsComponent)
      },
      {
        path: 'my-org-rfqs',
        loadComponent: () => import('./frontoffice/organizations/my-org-rfqs/my-org-rfqs.component').then(m => m.MyOrgRfqsComponent)
      },
      {
        path: 'org-matching',
        loadComponent: () => import('./frontoffice/organizations/org-matching/org-matching.component').then(m => m.OrgMatchingComponent)
      },

      // ── Route neutre pour liens dans les emails (Emmanuel) ───
      {
        path: 'my-complaints/:id',
        loadComponent: () => import('./shared/components/complaint-redirect/complaint-redirect.component').then(m => m.ComplaintRedirectComponent)
      },

      // ── Client ──────────────────────────────────────────────
      {
        path: 'client',
        canActivate: [clientGuard],
        children: [
          { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
          { path: 'dashboard', component: ClientDashboardComponent },

          // Jobs
          {
            path: 'create-job',
            loadComponent: () => import('./frontoffice/client/create-job-offer/create-job-offer.component').then(m => m.CreateJobOfferComponent)
          },
          {
            path: 'edit-job/:id',
            loadComponent: () => import('./frontoffice/client/create-job-offer/create-job-offer.component').then(m => m.CreateJobOfferComponent)
          },
          {
            path: 'my-jobs',
            loadComponent: () => import('./frontoffice/client/my-jobs/my-jobs.component').then(m => m.MyJobsComponent)
          },
          {
            path: 'my-jobs/:id',
            loadComponent: () => import('./frontoffice/client/job-detail-client/job-detail-client.component').then(m => m.JobDetailClientComponent)
          },
          {
            path: 'my-jobs/:jobId/applications/:applicationId',
            loadComponent: () => import('./frontoffice/client/job-detail-client/job-detail-client.component').then(m => m.JobDetailClientComponent)
          },

          // Freelancer search
          {
            path: 'freelancers',
            loadComponent: () => import('./frontoffice/client/search-freelancers/search-freelancers.component').then(m => m.SearchFreelancersComponent)
          },
          {
            path: 'freelancers/:id',
            loadComponent: () => import('./frontoffice/client/freelancer-public-profile/freelancer-public-profile.component').then(m => m.FreelancerPublicProfileComponent)
          },

          // Recommendations
          {
            path: 'my-recommendations',
            loadComponent: () => import('./frontoffice/client/my-recommendations/my-recommendations.component').then(m => m.MyRecommendationsComponent)
          },
          {
            path: 'my-recommendations/:id',
            loadComponent: () => import('./frontoffice/client/recommendation-detail/recommendation-detail.component').then(m => m.RecommendationDetailComponent)
          },

          // Projects
          {
            path: 'projects',
            loadComponent: () => import('./frontoffice/client/modules/project/client-projects/client-projects.component').then(m => m.ClientProjectsComponent)
          },
          {
            path: 'projects/create',
            loadComponent: () => import('./frontoffice/client/modules/project/create-project/create-project.component').then(m => m.CreateProjectComponent)
          },
          {
            path: 'projects/:id/dashboard',
            loadComponent: () => import('./frontoffice/client/modules/project/client-project-dashboard/client-project-dashboard.component').then(m => m.ClientProjectDashboardComponent)
          },
          {
            path: 'projects/:id/milestones/:milestoneId',
            loadComponent: () => import('./frontoffice/client/modules/project/client-milestone-detail/client-milestone-detail.component').then(m => m.ClientMilestoneDetailComponent)
          },

          // Evaluations (autres coéquipiers — ne pas toucher)
          {
            path: 'evaluations',
            loadComponent: () => import('./frontoffice/client/evaluation/freelancers-overview/freelancers-overview').then(m => m.FreelancersOverviewComponent)
          },
          {
            path: 'evaluations/evaluate',
            loadComponent: () => import('./frontoffice/client/evaluation/evaluate-freelancer/evaluate').then(m => m.EvaluateFreelancerComponent)
          },
          {
            path: 'evaluations/given',
            loadComponent: () => import('./frontoffice/client/evaluation/evaluations-given/evaluations-given').then(m => m.EvaluationsGivenComponent)
          },
          {
            path: 'evaluations/received',
            loadComponent: () => import('./frontoffice/client/evaluation/client-evaluations-received/client-evaluations-received').then(m => m.ClientEvaluationsReceivedComponent)
          },
          {
            path: 'evaluations/stats',
            loadComponent: () => import('./frontoffice/client/evaluation/client-evaluation-stats/client-evaluation-stats').then(m => m.ClientEvaluationStatsComponent)
          },
          {
            path: 'evaluations/freelancer/:email',
            loadComponent: () => import('./frontoffice/client/evaluation/freelancer-evaluations-view/freelancer-evaluations').then(m => m.FreelancerEvaluationsViewComponent)
          },
          {
            path: 'evaluations/freelancer-details/:email',
            loadComponent: () => import('./frontoffice/client/evaluation/freelancer-details/freelancer-details').then(m => m.FreelancerDetailsComponent)
          },

          // Contracts (autres coéquipiers — ne pas toucher)
          {
            path: 'contracts',
            loadComponent: () => import('./frontoffice/client/contracts/contract-list/contract').then(m => m.ContractListComponent)
          },
          {
            path: 'contracts/create',
            loadComponent: () => import('./frontoffice/client/contracts/contract-create/contract').then(m => m.ContractCreateComponent)
          },
          {
            path: 'contracts/:id',
            loadComponent: () => import('./frontoffice/client/contracts/contract-detail/contract').then(m => m.ClientContractDetailComponent)
          },

          // Badges (autres coéquipiers — ne pas toucher)
          {
            path: 'badges',
            loadComponent: () => import('./frontoffice/client/badges/badges-list/badges-list').then(m => m.BadgesListComponent)
          },
          {
            path: 'badges/freelancer/:email',
            loadComponent: () => import('./frontoffice/client/badges/freelancer-badges-view/freelancer-badges-view').then(m => m.FreelancerBadgesViewComponent)
          },

          // About Me (autres coéquipiers — ne pas toucher)
          {
            path: 'skills',
            loadComponent: () => import('./frontoffice/client/modules/skills/client-skills.component').then(m => m.ClientSkillsComponent)
          },
          {
            path: 'portfolios',
            loadComponent: () => import('./frontoffice/client/modules/portfolios/client-portfolios.component').then(m => m.ClientPortfoliosComponent)
          },
          {
            path: 'certifications',
            loadComponent: () => import('./frontoffice/client/modules/certifications/client-certifications.component').then(m => m.ClientCertificationsComponent)
          },

          // AI Recommendations (autres coéquipiers — ne pas toucher)
          {
            path: 'ai-recommendations',
            loadComponent: () => import('./frontoffice/client/ai-recommendations/ai-recommendations.component').then(m => m.ClientAiRecommendationsComponent)
          },

          // Réclamations client (Emmanuel) — nouvelle route my-complaints
          {
            path: 'my-complaints',
            loadComponent: () => import('./frontoffice/client/complaints/my-complaints.component').then(m => m.MyComplaintsComponent)
          },
          {
            path: 'my-complaints/new',
            loadComponent: () => import('./frontoffice/client/complaints/create-complaint.component').then(m => m.CreateComplaintComponent)
          },
          {
            path: 'my-complaints/:id',
            loadComponent: () => import('./frontoffice/client/complaints/complaint-detail.component').then(m => m.ComplaintDetailComponent)
          },
          // Aliases compatibilité ancienne route /complaints
          {
            path: 'complaints',
            redirectTo: 'my-complaints',
            pathMatch: 'full'
          },
          {
            path: 'complaints/create',
            redirectTo: 'my-complaints/new',
            pathMatch: 'full'
          },
          {
            path: 'complaints/:id',
            redirectTo: 'my-complaints/:id',
            pathMatch: 'prefix'
          }
        ]
      },

      // ── Freelancer ───────────────────────────────────────────
      {
        path: 'freelancer',
        canActivate: [freelancerGuard],
        children: [
          { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
          { path: 'dashboard', component: FreelancerDashboardComponent },

          // Jobs
          {
            path: 'browse-jobs',
            loadComponent: () => import('./frontoffice/freelancer/browse-jobs/browse-jobs.component').then(m => m.BrowseJobsComponent)
          },
          {
            path: 'jobs/:id',
            loadComponent: () => import('./frontoffice/freelancer/job-detail-freelance/job-detail-freelance.component').then(m => m.JobDetailFreelanceComponent)
          },
          {
            path: 'my-applications',
            loadComponent: () => import('./frontoffice/freelancer/my-applications/my-applications.component').then(m => m.MyApplicationsComponent)
          },

          // Invitations freelancer (Emmanuel)
          {
            path: 'my-invitations',
            loadComponent: () => import('./frontoffice/freelancer/my-invitations/my-invitations.component').then(m => m.MyInvitationsFreelancerComponent)
          },
          {
            path: 'my-invitations/:id',
            loadComponent: () => import('./frontoffice/freelancer/my-invitations/invitation-detail/invitation-detail.component').then(m => m.InvitationDetailFreelancerComponent)
          },

          // Recommendations
          {
            path: 'my-recommendations',
            loadComponent: () => import('./frontoffice/freelancer/my-recommendations/my-recommendations.component').then(m => m.FreelancerMyRecommendationsComponent)
          },

          // Projects
          {
            path: 'my-projects',
            loadComponent: () => import('./frontoffice/freelancer/modules/project/freelancer-projects/freelancer-projects.component').then(m => m.FreelancerProjectsComponent)
          },
          {
            path: 'my-projects/:id',
            loadComponent: () => import('./frontoffice/freelancer/modules/project/freelancer-project-dashboard/freelancer-project-dashboard.component').then(m => m.FreelancerProjectDashboardComponent)
          },
          {
            path: 'my-projects/:id/milestones/:milestoneId',
            loadComponent: () => import('./frontoffice/freelancer/modules/project/freelancer-milestone-detail/freelancer-milestone-detail.component').then(m => m.FreelancerMilestoneDetailComponent)
          },

          // Evaluations (autres coéquipiers — ne pas toucher)
          {
            path: 'evaluations',
            loadComponent: () => import('./frontoffice/freelancer/evaluation/my-evaluations-received/my-evaluations-received').then(m => m.MyEvaluationsReceivedComponent)
          },
          {
            path: 'evaluations/evaluate',
            loadComponent: () => import('./frontoffice/freelancer/evaluation/evaluate-client/evaluate-client').then(m => m.EvaluateClientComponent)
          },
          {
            path: 'evaluations/given',
            loadComponent: () => import('./frontoffice/freelancer/evaluation/freelancer-evaluations-given/freelancer-evaluations-given').then(m => m.FreelancerEvaluationsGivenComponent)
          },
          {
            path: 'evaluations/respond/:id',
            loadComponent: () => import('./frontoffice/freelancer/evaluation/respond-evaluation/respond-evaluation').then(m => m.RespondEvaluationComponent)
          },
          {
            path: 'evaluations/stats',
            loadComponent: () => import('./frontoffice/freelancer/evaluation/evaluation-stats/evaluation-stats').then(m => m.EvaluationStatsComponent)
          },

          // Contracts (autres coéquipiers — ne pas toucher)
          {
            path: 'contracts',
            loadComponent: () => import('./frontoffice/freelancer/contracts/contract-list/contract').then(m => m.ContractListComponent)
          },
          {
            path: 'contracts/:id',
            loadComponent: () => import('./frontoffice/freelancer/contracts/contract-detail/contract').then(m => m.ContractDetailComponent)
          },

          // Badges (autres coéquipiers — ne pas toucher)
          {
            path: 'badges',
            loadComponent: () => import('./frontoffice/freelancer/badges/my-badges/my-badges').then(m => m.MyBadgesComponent)
          },
          {
            path: 'badges/progress',
            loadComponent: () => import('./frontoffice/freelancer/badges/all-badges-progress/all-badges-progress').then(m => m.AllBadgesProgressComponent)
          },

          // About Me (autres coéquipiers — ne pas toucher)
          {
            path: 'skills',
            loadComponent: () => import('./frontoffice/freelancer/modules/skills/freelancer-skills.component').then(m => m.FreelancerSkillsComponent)
          },
          {
            path: 'portfolio',
            loadComponent: () => import('./frontoffice/freelancer/modules/portfolio/freelancer-portfolio.component').then(m => m.FreelancerPortfolioComponent)
          },
          {
            path: 'certifications',
            loadComponent: () => import('./frontoffice/freelancer/modules/certifications/freelancer-certifications.component').then(m => m.FreelancerCertificationsComponent)
          },

          // AI Recommendations (autres coéquipiers — ne pas toucher)
          {
            path: 'ai-recommendations',
            loadComponent: () => import('./frontoffice/freelancer/ai-recommendations/ai-recommendations.component').then(m => m.FreelancerAiRecommendationsComponent)
          },

          // Réclamations freelancer (Emmanuel) — nouvelle route my-complaints
          {
            path: 'my-complaints',
            loadComponent: () => import('./frontoffice/client/complaints/my-complaints.component').then(m => m.MyComplaintsComponent)
          },
          {
            path: 'my-complaints/new',
            loadComponent: () => import('./frontoffice/client/complaints/create-complaint.component').then(m => m.CreateComplaintComponent)
          },
          {
            path: 'my-complaints/:id',
            loadComponent: () => import('./frontoffice/client/complaints/complaint-detail.component').then(m => m.ComplaintDetailComponent)
          },
          // Aliases compatibilité ancienne route /complaints
          {
            path: 'complaints',
            redirectTo: 'my-complaints',
            pathMatch: 'full'
          },
          {
            path: 'complaints/create',
            redirectTo: 'my-complaints/new',
            pathMatch: 'full'
          },
          {
            path: 'complaints/:id',
            redirectTo: 'my-complaints/:id',
            pathMatch: 'prefix'
          }
        ]
      }
    ]
  },

  { path: '**', redirectTo: '/login' }
];
