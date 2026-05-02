export const environment = {
  production: true,
  apiUrl: 'http://localhost:8080/api', // All REST through API Gateway
  userApiUrl: 'http://localhost:8080/api', // User service via gateway
  jobOffersApiUrl: 'http://localhost:8080/api', // Job offers via gateway
  invitationsApiUrl: 'http://localhost:8080/api', // Invitations via gateway
  projectsApiUrl: 'http://localhost:8080/api', // Projects via gateway
  contractApiUrl: 'http://localhost:8080/contrat_backend',
  evaluationApiUrl: 'http://localhost:8080/api',
  complaintsApiUrl: 'http://localhost:8080/api', // Complaints service via gateway
  organizationsApiUrl: 'http://localhost:8080/api', // Organizations service via gateway
  // WebSocket URLs (direct to backend services, not proxied through gateway)
  jobOffersWsUrl: 'http://localhost:8087/ws',
  projectsWsUrl: 'http://localhost:9091/ws',

  // Algolia - Live search for job offers
  algolia: {
    appId: 'KM9WRMQS2G',
    searchApiKey: 'df7c0ee2645e62f782113a8fbbde715a',
    indexName: 'nexlance_job_offers'
  },

  // Cloudinary - File upload & versioning
  cloudinary: {
    cloudName: 'de6pfv187',
    uploadPreset: 'nexlance_unsigned',
    apiUrl: 'https://api.cloudinary.com/v1_1'
  },

  // PostHog - Analytics
  posthog: {
    apiKey: 'phc_St3Ma6Z1M6YcjUgvfsw2OcdL02Qa9jA0GXIxPq2B49Y',
    apiHost: 'https://us.i.posthog.com'
  },

  // REST Countries
  restCountriesUrl: 'https://restcountries.com/v3.1',

  // ExchangeRate API
  exchangeRate: {
    apiUrl: 'https://v6.exchangerate-api.com/v6',
    apiKey: 'c9fb6bff9b339a53f277a8b4'
  },

  // Quotable API
  quotableApiUrl: 'https://api.quotable.io'
};
