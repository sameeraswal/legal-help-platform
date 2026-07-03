export interface CaseCategory {
  id: number;
  slug: string;
  name: string;
  description: string | null;
  templateKey: string;
  active: boolean;
}

export interface CaseCategoryUpsertRequest {
  slug: string;
  name: string;
  description: string;
  templateKey: string;
  active: boolean;
}

export type CaseStatus = "DRAFT" | "SUBMITTED" | "PETITION_GENERATED";

export interface CaseRecord {
  id: number;
  categoryId: number;
  details: Record<string, unknown>;
  status: CaseStatus;
  createdAt: string;
  updatedAt: string;
}

export interface Petition {
  id: number;
  caseId: number;
  pdfUrl: string;
  docxUrl: string;
  version: number;
  disclaimerVersion: string;
  generatedAt: string;
}
