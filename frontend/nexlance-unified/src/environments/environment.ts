export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api', // All REST through API Gateway
  userApiUrl: 'http://localhost:8080/api', // User service via gateway
  jobOffersApiUrl: 'http://localhost:8080/api', // Job offers via gateway
  invitationsApiUrl: 'http://localhost:8080/api', // Invitations via gateway
  projectsApiUrl: 'http://localhost:8080/api', // Projects via gateway
  contractApiUrl: 'http://localhost:8080/contrat_backend',
  evaluationApiUrl: 'http://localhost:8080/api',
  portfolioApiUrl: 'http://localhost:8080/api', // Portfolio service (8087) via gateway
  competenceApiUrl: 'http://localhost:8080/api', // Competence service (8088) via gateway
  certificationApiUrl: 'http://localhost:8080/api', // Certification service (8089) via gateway
  complaintsApiUrl: 'http://localhost:8080/api', // Complaints service via gateway
  organizationsApiUrl: 'http://localhost:8080/api', // Organizations service via gateway
  notificationsApiUrl: 'http://localhost:8080/api', // Notification service via gateway (Emmanuel — port 9090 direct)
  recommendationAiApiUrl: 'http://localhost:8080/api', // Recommendation AI service via gateway
  // WebSocket URLs (direct to backend services, not proxied through gateway)
  jobOffersWsUrl: 'http://localhost:8087/ws',
  projectsWsUrl: 'http://localhost:9091/ws',
  notificationsWsUrl: 'http://localhost:9090/ws', // WebSocket STOMP direct — notification-service Emmanuel
  // Algolia - Live search for job offers (free plan: 10k records, 10k searches/month)
  algolia: {
    appId: 'KM9WRMQS2G',
    searchApiKey: 'df7c0ee2645e62f782113a8fbbde715a', // Public search-only key
    indexName: 'nexlance_job_offers'
  },

  // Cloudinary - File upload & versioning (free plan: 25GB storage, 25GB bandwidth)
  cloudinary: {
    cloudName: 'de6pfv187',
    uploadPreset: 'nexlance_unsigned', // Unsigned upload preset
    apiUrl: 'https://api.cloudinary.com/v1_1'
  },

  // PostHog - Analytics (free plan: 1M events/month)
  posthog: {
    apiKey: 'phc_St3Ma6Z1M6YcjUgvfsw2OcdL02Qa9jA0GXIxPq2B49Y',
    apiHost: 'https://us.i.posthog.com'
  },

  // REST Countries - Country data for location fields (free, no key needed)
  restCountriesUrl: 'https://restcountries.com/v3.1',

  // ExchangeRate API - Currency conversion (free: 1500 req/month)
  exchangeRate: {
    apiUrl: 'https://v6.exchangerate-api.com/v6',
    apiKey: 'c9fb6bff9b339a53f277a8b4'
  },

  // Quotable API - Motivational quotes for dashboards (free, no key needed)
  quotableApiUrl: 'https://api.quotable.io'
};
