# NexLance Unified - Architecture du Projet

## Structure du Projet

Le projet **NexLance Unified** est organisé en une architecture modulaire avec séparation des responsabilités :

```
nexlance-unified/
├── src/
│   ├── app/
│   │   ├── backoffice/              # MODULE BACK OFFICE
│   │   │   ├── layout/              # Layout pour l'administration
│   │   │   └── admin/
│   │   │       └── admin-dashboard/ # Dashboard administrateur
│   │   │
│   │   ├── frontoffice/             # MODULE FRONT OFFICE
│   │   │   ├── layout/              # Layout pour client/freelancer
│   │   │   ├── client/
│   │   │   │   └── client-dashboard/   # Dashboard client
│   │   │   └── freelancer/
│   │   │       └── freelancer-dashboard/ # Dashboard freelancer
│   │   │
│   │   ├── shared/                  # MODULES PARTAGÉS
│   │   │   ├── modules/
│   │   │   │   └── user/            # MODULE USER
│   │   │   │       ├── login/       # Login unique pour tous les rôles
│   │   │   │       └── register/    # Register par rôle
│   │   │   └── models/
│   │   │       └── user.model.ts    # Modèles utilisateur
│   │   │
│   │   ├── core/                    # SERVICES & GUARDS
│   │   │   ├── services/
│   │   │   │   └── auth.service.ts  # Service d'authentification
│   │   │   ├── guards/
│   │   │   │   ├── auth.guard.ts    # Guard d'authentification
│   │   │   │   └── role.guard.ts    # Guards par rôle
│   │   │   └── interceptors/
│   │   │       └── auth.interceptor.ts # Intercepteur HTTP
│   │   │
│   │   ├── app.routes.ts            # Configuration des routes
│   │   ├── app.config.ts            # Configuration de l'app
│   │   └── app.component.ts         # Composant racine
│   │
│   ├── assets/                      # Assets statiques
│   ├── styles.scss                  # Styles globaux
│   ├── index.html                   # HTML principal
│   └── main.ts                      # Point d'entrée
│
├── angular.json                     # Configuration Angular
├── package.json                     # Dépendances
└── tsconfig.json                    # Configuration TypeScript
```

## Organisation des Modules

### 1. **Back Office** (`/backoffice`)
Dédié à l'administration de la plateforme :
- Gestion des utilisateurs (clients, freelancers, admins)
- Supervision des projets
- Statistiques et analytics
- **Accès réservé au rôle ADMIN**

### 2. **Front Office** (`/frontoffice`)
Interface pour les clients et freelancers :
- **Client** : Recherche de freelancers, publication de projets
- **Freelancer** : Navigation des projets, soumission de propositions
- **Accès selon le rôle de l'utilisateur**

### 3. **Module User** (`/shared/modules/user`)
Gestion de l'authentification par rôle :
- **Login unique** : Un seul formulaire pour tous les rôles
- **Register par rôle** :
  - `/register/client` - Inscription client
  - `/register/freelancer` - Inscription freelancer
  - `/register/admin` - Inscription administrateur

## Routes de l'Application

### Routes Publiques
- `/` → Redirection vers `/login`
- `/login` → Page de connexion unique
- `/register/client` → Inscription client
- `/register/freelancer` → Inscription freelancer
- `/register/admin` → Inscription administrateur

### Routes Back Office (Protégées - Admin uniquement)
- `/backoffice/admin/dashboard` → Dashboard administrateur

### Routes Front Office (Protégées - Par rôle)
- `/frontoffice/client/dashboard` → Dashboard client (CLIENT ou ADMIN)
- `/frontoffice/freelancer/dashboard` → Dashboard freelancer (FREELANCER ou ADMIN)

## Sécurité et Guards

### AuthGuard
Vérifie si l'utilisateur est authentifié. Si non, redirige vers `/login`.

### RoleGuard
Vérifie si l'utilisateur a le bon rôle pour accéder à une route :
- **adminGuard** : Réservé aux ADMIN
- **clientGuard** : CLIENT ou ADMIN
- **freelancerGuard** : FREELANCER ou ADMIN

## Authentification

### Service AuthService
Gère toute l'authentification :
- `login()` : Connexion unique
- `registerClient()` : Inscription client
- `registerFreelancer()` : Inscription freelancer
- `registerAdmin()` : Inscription admin
- `logout()` : Déconnexion
- `isAuthenticated()` : Vérification d'authentification
- `hasRole()` : Vérification de rôle

### Redirection Automatique
Après login/register, redirection automatique selon le rôle :
- **CLIENT** → `/frontoffice/client/dashboard`
- **FREELANCER** → `/frontoffice/freelancer/dashboard`
- **ADMIN** → `/backoffice/admin/dashboard`

## Backend API

Le projet se connecte au backend sur :
- **URL** : `http://localhost:8080/api/auth`
- **Endpoints** :
  - `POST /api/auth/register` - Inscription
  - `POST /api/auth/login` - Connexion

## Configuration du Port

Le projet est configuré pour démarrer sur le port **4200** :
```bash
npm start
```

Cette commande lance :
```bash
ng serve --port 4200
```

## Technologies Utilisées

- **Angular 21+** : Framework front-end
- **Angular Material** : Bibliothèque UI
- **RxJS** : Programmation réactive
- **TypeScript** : Langage
- **SCSS** : Préprocesseur CSS

## Commandes Disponibles

```bash
# Installation des dépendances
npm install

# Démarrer le serveur de développement (port 4200)
npm start

# Build de production
npm run build

# Tests
npm test
```

## Prochaines Étapes

1. Ajouter plus de pages dans chaque module
2. Implémenter la gestion complète des utilisateurs (admin)
3. Créer les pages de projets (client/freelancer)
4. Ajouter les fonctionnalités de recherche
5. Intégrer les notifications en temps réel
6. Améliorer le design et l'UX

---

**Note** : Ce projet utilise une architecture standalone components (sans NgModules) conformément aux meilleures pratiques Angular modernes.
