# Admin Job Detail Component

## 📋 Description

Interface administrative complète pour visualiser les détails d'une offre d'emploi spécifique. Ce component permet aux administrateurs de consulter toutes les informations relatives à une offre d'emploi, gérer son statut, et visualiser les candidatures associées.

## 📁 Fichiers Créés

### 1. **admin-job-detail.component.ts**
- **Localisation**: `src/app/backoffice/admin/modules/admin-job-detail/admin-job-detail.component.ts`
- **Type**: Component TypeScript standalone
- **Fonctionnalités**:
  - Récupération des détails d'une offre d'emploi par ID
  - Affichage des informations du client créateur
  - Liste des candidatures reçues
  - Actions administrateur: Réouvrir, Archiver, Annuler, Supprimer
  - Gestion des statuts avec notifications
  - Navigation vers les profils utilisateurs et candidatures

### 2. **admin-job-detail.component.html**
- **Localisation**: `src/app/backoffice/admin/modules/admin-job-detail/admin-job-detail.component.html`
- **Structure**:
  - Header avec titre, statut et menu d'actions
  - Cartes statistiques (Vues, Candidatures, Budget, Durée)
  - Colonne gauche: Description, Détails, Compétences, Fichiers joints
  - Colonne droite: Informations client, Liste des candidatures

### 3. **admin-job-detail.component.scss**
- **Localisation**: `src/app/backoffice/admin/modules/admin-job-detail/admin-job-detail.component.scss`
- **Design**:
  - Layout responsive avec grille CSS
  - Cartes avec effets hover et ombres
  - Gradient de couleurs pour les statuts
  - Animation fadeIn au chargement
  - Scrollbar personnalisée

## 🛣️ Routing

### Route ajoutée dans `app.routes.ts`:
```typescript
{
  path: 'admin/jobs/:id',
  loadComponent: () => import('./backoffice/admin/modules/admin-job-detail/admin-job-detail.component')
    .then(m => m.AdminJobDetailComponent)
}
```

### Navigation depuis AdminJobsComponent:
- Clic sur une ligne de tableau → `/backoffice/admin/jobs/:id`
- Méthode `viewJobDetails()` modifiée pour pointer vers la nouvelle route

## 🎨 Fonctionnalités

### ✅ Informations Affichées
- **Informations générales**: Titre, description, catégorie, statut
- **Budget**: Montant et type (fixe/horaire)
- **Timing**: Durée estimée, deadline, dates de publication/mise à jour
- **Compétences**: Liste des compétences requises avec chips
- **Localisation**: Adresse + badge Remote si applicable
- **Niveau d'expérience**: Débutant, Intermédiaire, Expert
- **Statistiques**: Nombre de vues et de candidatures
- **Fichiers**: Liste des documents joints téléchargeables

### ⚙️ Actions Administrateur
- **Réouvrir** (Reopen): Remettre une offre au statut OPEN
- **Archiver** (Archive): Passer au statut ARCHIVED
- **Annuler** (Cancel): Annuler une offre ouverte → CANCELLED
- **Supprimer** (Delete): Suppression définitive avec confirmation

### 👤 Section Client
- Avatar du client
- Nom complet
- Email et téléphone
- Bouton "View Profile" pour accéder au profil complet

### 📝 Section Candidatures
- Liste de toutes les candidatures avec:
  - ID du freelancer
  - Date de candidature
  - Statut (En attente, Acceptée, Rejetée, Retirée)
  - Chips colorés selon le statut
- Clic sur une candidature → Navigation vers détails

## 🎨 Codes Couleur des Statuts

### Statuts d'Offres
- **DRAFT** (Brouillon): Jaune/Orange `#ffeaa7 → #fdcb6e`
- **OPEN** (Ouverte): Bleu `#74b9ff → #0984e3`
- **IN_PROGRESS** (En cours): Violet `#a29bfe → #6c5ce7`
- **COMPLETED** (Terminée): Vert `#55efc4 → #00b894`
- **CANCELLED** (Annulée): Rouge `#ff7675 → #d63031`
- **ARCHIVED** (Archivée): Gris `#b2bec3 → #636e72`

### Statuts de Candidatures
- **PENDING** (En attente): Jaune `#ffeaa7`
- **ACCEPTED** (Acceptée): Vert `#00b894`
- **REJECTED** (Rejetée): Rouge `#d63031`
- **WITHDRAWN** (Retirée): Gris `#636e72`

## 🔌 Services Utilisés

- **JobOfferService**: Récupération et gestion des offres
  - `getJobOfferById(id)`: Détails de l'offre
  - `changeStatus(id, status)`: Modification de statut
  - `deleteJobOffer(id)`: Suppression

- **ApplicationService**: Gestion des candidatures
  - `getApplicationsByJobOffer(jobOfferId)`: Liste des candidatures

- **UserService**: Informations utilisateurs
  - `getUserById(id)`: Détails du client

## 📱 Responsive Design

- **Desktop** (>968px): Layout 2 colonnes (2fr | 1fr)
- **Tablette/Mobile** (<968px): Layout 1 colonne empilée
- Cartes statistiques adaptatives avec `repeat(auto-fit, minmax(240px, 1fr))`

## 🚀 Utilisation

### Pour accéder à la page:
1. Se connecter en tant qu'administrateur
2. Naviguer vers "Admin → Jobs"
3. Cliquer sur une ligne de tableau
4. OU accéder directement à `/backoffice/admin/jobs/{JOB_ID}`

### Navigation retour:
- Bouton flèche ← en haut à gauche
- Méthode `goBack()` → `/admin/jobs`

## 📊 Exemple de Données Affichées

```typescript
{
  id: "uuid-123",
  title: "Full Stack Developer Needed",
  description: "We are looking for...",
  category: "DEVELOPMENT",
  budget: 5000,
  budgetType: "FIXED",
  estimatedDuration: 30,
  deadline: "2026-05-15",
  status: "OPEN",
  requiredSkills: ["Angular", "Spring Boot", "MySQL"],
  experienceLevel: "INTERMEDIATE",
  location: "Tunis, Tunisia",
  isRemote: true,
  viewCount: 142,
  applicantCount: 8
}
```

## ⚠️ Notes Importantes

1. **Route Priority**: La route `admin/jobs/:id` doit être placée AVANT `admin/jobs/:id/moderate` dans le fichier de routes pour éviter les conflits
2. **Confirmation Suppression**: Un popup de confirmation s'affiche avant suppression définitive
3. **État Loading**: Spinner affiché pendant le chargement des données
4. **Gestion d'Erreurs**: Messages d'erreur avec MatSnackBar en cas de problème

## 🔒 Sécurité

- Route protégée par `authGuard` et `adminGuard`
- Accessible uniquement aux utilisateurs avec le rôle ADMIN
- Vérification de l'existence du job ID dans `ngOnInit`

## 🎯 Améliorations Futures Possibles

- [ ] Export PDF des détails de l'offre
- [ ] Historique des modifications de statut
- [ ] Commentaires administrateur internes
- [ ] Filtrage/tri des candidatures
- [ ] Graphiques de statistiques avancées
- [ ] Envoi d'emails depuis l'interface

---

**Créé le**: 11 avril 2026  
**Auteur**: GitHub Copilot  
**Version**: 1.0.0
