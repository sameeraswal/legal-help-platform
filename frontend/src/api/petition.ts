import { apiFetch, apiFetchBlob } from "./client";
import type { CaseCategory, CaseCategoryUpsertRequest, CaseRecord, Petition } from "./types/petition";

export function listCategories(): Promise<CaseCategory[]> {
  return apiFetch<CaseCategory[]>("/api/categories");
}

export function listAllCategoriesAdmin(): Promise<CaseCategory[]> {
  return apiFetch<CaseCategory[]>("/api/admin/categories");
}

export function createCategory(request: CaseCategoryUpsertRequest): Promise<CaseCategory> {
  return apiFetch<CaseCategory>("/api/admin/categories", { method: "POST", body: JSON.stringify(request) });
}

export function updateCategory(id: number, request: CaseCategoryUpsertRequest): Promise<CaseCategory> {
  return apiFetch<CaseCategory>(`/api/admin/categories/${id}`, { method: "PUT", body: JSON.stringify(request) });
}

export function listMyCases(): Promise<CaseRecord[]> {
  return apiFetch<CaseRecord[]>("/api/cases");
}

export function getCase(caseId: number): Promise<CaseRecord> {
  return apiFetch<CaseRecord>(`/api/cases/${caseId}`);
}

export function startCase(categoryId: number, details: Record<string, unknown>): Promise<CaseRecord> {
  return apiFetch<CaseRecord>("/api/cases", { method: "POST", body: JSON.stringify({ categoryId, details }) });
}

export function updateCaseDraft(caseId: number, details: Record<string, unknown>): Promise<CaseRecord> {
  return apiFetch<CaseRecord>(`/api/cases/${caseId}`, { method: "PATCH", body: JSON.stringify({ details }) });
}

export function submitCase(caseId: number): Promise<CaseRecord> {
  return apiFetch<CaseRecord>(`/api/cases/${caseId}/submit`, { method: "POST" });
}

export function generatePetition(caseId: number): Promise<Petition> {
  return apiFetch<Petition>(`/api/cases/${caseId}/petitions/generate`, { method: "POST" });
}

export function listPetitionVersions(caseId: number): Promise<Petition[]> {
  return apiFetch<Petition[]>(`/api/cases/${caseId}/petitions`);
}

export function downloadPetitionPdf(caseId: number, petitionId: number): Promise<Blob> {
  return apiFetchBlob(`/api/cases/${caseId}/petitions/${petitionId}/pdf`);
}

export function downloadPetitionDocx(caseId: number, petitionId: number): Promise<Blob> {
  return apiFetchBlob(`/api/cases/${caseId}/petitions/${petitionId}/docx`);
}
