export enum ContractStatus {
  DRAFT = 'DRAFT',
  SIGNED_BY_CLIENT = 'SIGNED_BY_CLIENT',
  PENDING_FREELANCER_SIGNATURE = 'PENDING_FREELANCER_SIGNATURE',
  FULLY_SIGNED = 'FULLY_SIGNED',
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export enum ContractType {
  STANDARD = 'STANDARD',
  PROFESSIONAL = 'PROFESSIONAL',
  PREMIUM = 'PREMIUM'
}

export enum Currency {
  EUR = 'EUR',
  USD = 'USD',
  TND = 'TND',
  GBP = 'GBP',
  CAD = 'CAD'
}

export enum PaymentMethod {
  BANK_TRANSFER = 'BANK_TRANSFER',
  CREDIT_CARD = 'CREDIT_CARD',
  PAYPAL = 'PAYPAL',
  CHECK = 'CHECK',
  CASH = 'CASH'
}

export enum ApplicableLaw {
  FRENCH = 'FRENCH',
  TUNISIAN = 'TUNISIAN',
  AMERICAN = 'AMERICAN',
  CANADIAN = 'CANADIAN',
  CHINESE = 'CHINESE',
  UK = 'UK',
  OTHER = 'OTHER'
}

export enum MilestoneStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  AWAITING_VALIDATION = 'AWAITING_VALIDATION',
  VALIDATED = 'VALIDATED',
  REJECTED = 'REJECTED'
}

export enum PaymentStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  OVERDUE = 'OVERDUE'
}

export interface Milestone {
  id?: number;
  sequenceNumber?: number;
  title: string;
  description?: string;
  dueDate: string;
  linkedPaymentAmount?: number;
  status?: MilestoneStatus;
  validatedAt?: string;
  validatedBy?: string;
  validationComment?: string;
  createdAt?: string;
}

export interface PaymentSchedule {
  id?: number;
  sequenceNumber?: number;
  label: string;
  percentage?: number;
  amount: number;
  dueDate: string;
  milestoneCondition?: string;
  status?: PaymentStatus;
  paidAt?: string;
  invoiceNumber?: string;
  createdAt?: string;
}

export interface CustomClause {
  id?: number;
  clauseNumber?: number;
  title: string;
  content: string;
  addedBy?: string;
  addedAt?: string;
  position?: number;
}

export interface Contract {
  id?: number;
  contractNumber?: string;
  contractType: ContractType;

  // Client
  clientId: string;
  clientName?: string;
  clientCompany?: string;
  clientEmail?: string;
  clientPhone?: string;
  clientAddress?: string;
  clientCountry?: string;
  clientSiret?: string;

  // Freelancer
  freelancerId: string;
  freelancerName?: string;
  freelancerEmail?: string;
  freelancerPhone?: string;
  freelancerAddress?: string;
  freelancerCountry?: string;
  freelancerSiret?: string;
  freelancerSpecialty?: string;
  freelancerCin?: string; 


  // Dans l'interface Contract, ajouter :
modificationComment?: string;
modificationRequestedAt?: string;

  // Mission
  missionTitle: string;
  missionDescription?: string;
  deliverables?: string;
  technologies?: string;

  // Dates
  startDate?: string;
  endDate?: string;
  durationMonths?: number;

  // Financier
  totalAmount: number;
  currency?: Currency;
  vatRate?: number;
  amountHT?: number;
  amountTTC?: number;
  paymentMethod?: PaymentMethod;
  iban?: string;
  bic?: string;

  // Juridique
  applicableLaw?: ApplicableLaw;
  competentCourt?: string;
  confidentialityYears?: number;
  ipTransferToClient?: boolean;
  portfolioAllowed?: boolean;
  portfolioDelayMonths?: number;

  // Statut & Signatures
  status?: ContractStatus;
  clientSignedAt?: string;
  clientSignatureHash?: string;
  freelancerSignedAt?: string;
  freelancerSignatureHash?: string;
    clientSignatureImage?: string; 
      freelancerSignatureImage?: string; 

  // Metadata
  createdAt?: string;
  updatedAt?: string;

  // Relations
  paymentSchedules?: PaymentSchedule[];
  milestones?: Milestone[];
  customClauses?: CustomClause[];
}

export interface ContractStatistics {
  totalContracts: number;
  draftContracts: number;
  activeContracts: number;
  completedContracts: number;
  pendingSignature: number;
}

export interface FreelancerEarnings {
  totalEarnings: number;
  pendingEarnings: number;
  completedContracts: number;
}

export interface CurrencyConversion {
  originalAmount: number;
  originalCurrency: Currency;
  convertedAmount: number;
  targetCurrency: Currency;
  exchangeRate: number;
  lastUpdated: string;
}