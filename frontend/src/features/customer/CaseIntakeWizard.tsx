import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getCase, submitCase, updateCaseDraft } from "../../api/petition";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import { Card } from "../../components/Card";
import { Spinner } from "../../components/Spinner";

interface IntakeFields {
  partyName: string;
  opposingParty: string;
  incidentDate: string;
  amountInvolved: string;
  description: string;
}

const EMPTY_FIELDS: IntakeFields = { partyName: "", opposingParty: "", incidentDate: "", amountInvolved: "", description: "" };

export function CaseIntakeWizard() {
  const { caseId } = useParams<{ caseId: string }>();
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [fields, setFields] = useState<IntakeFields>(EMPTY_FIELDS);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!caseId) return;
    getCase(Number(caseId)).then((c) => {
      setFields({ ...EMPTY_FIELDS, ...(c.details as Partial<IntakeFields>) });
      setLoading(false);
    });
  }, [caseId]);

  async function saveDraft() {
    if (!caseId) return;
    setSaving(true);
    try {
      await updateCaseDraft(Number(caseId), fields as unknown as Record<string, unknown>);
    } finally {
      setSaving(false);
    }
  }

  async function handleSubmitCase() {
    if (!caseId) return;
    await saveDraft();
    await submitCase(Number(caseId));
    navigate(`/cases/${caseId}`);
  }

  function update<K extends keyof IntakeFields>(key: K, value: string) {
    setFields((prev) => ({ ...prev, [key]: value }));
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-8">
        <Spinner />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="mb-1 text-xl font-semibold text-gray-900">Case details</h1>
      <p className="mb-6 text-sm text-gray-500">Step {step} of 2</p>

      <Card>
        {step === 1 && (
          <div className="space-y-4">
            <Input placeholder="Your full name" value={fields.partyName} onChange={(e) => update("partyName", e.target.value)} />
            <Input
              placeholder="Other party's name"
              value={fields.opposingParty}
              onChange={(e) => update("opposingParty", e.target.value)}
            />
            <Input
              type="date"
              placeholder="Date of incident"
              value={fields.incidentDate}
              onChange={(e) => update("incidentDate", e.target.value)}
            />
            <Input
              placeholder="Amount involved (₹, if applicable)"
              value={fields.amountInvolved}
              onChange={(e) => update("amountInvolved", e.target.value)}
            />
          </div>
        )}

        {step === 2 && (
          <div className="space-y-4">
            <textarea
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
              rows={8}
              placeholder="Describe what happened in your own words..."
              value={fields.description}
              onChange={(e) => update("description", e.target.value)}
            />
          </div>
        )}

        <div className="mt-6 flex justify-between">
          <Button variant="secondary" onClick={() => (step === 1 ? navigate("/categories") : setStep(1))}>
            Back
          </Button>
          <div className="flex gap-2">
            <Button variant="secondary" onClick={saveDraft} disabled={saving}>
              {saving ? "Saving..." : "Save draft"}
            </Button>
            {step === 1 ? (
              <Button onClick={() => setStep(2)}>Next</Button>
            ) : (
              <Button onClick={handleSubmitCase}>Submit case</Button>
            )}
          </div>
        </div>
      </Card>
    </div>
  );
}
