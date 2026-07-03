import { useState } from "react";
import { useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { downloadPetitionDocx, downloadPetitionPdf, generatePetition, getCase, listPetitionVersions } from "../../api/petition";
import { Card } from "../../components/Card";
import { Button } from "../../components/Button";
import { Spinner } from "../../components/Spinner";
import { formatIst } from "../../utils/format";

function triggerDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export function CaseDetailPage() {
  const { caseId } = useParams<{ caseId: string }>();
  const id = Number(caseId);
  const queryClient = useQueryClient();
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { data: caseRecord, isLoading } = useQuery({ queryKey: ["case", id], queryFn: () => getCase(id) });
  const { data: petitions } = useQuery({ queryKey: ["petitions", id], queryFn: () => listPetitionVersions(id) });

  const generateMutation = useMutation({
    mutationFn: () => generatePetition(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["petitions", id] });
      queryClient.invalidateQueries({ queryKey: ["case", id] });
    },
  });

  async function handleGenerate() {
    setError(null);
    setGenerating(true);
    try {
      await generateMutation.mutateAsync();
    } catch {
      setError("Petition generation failed. Please try again in a moment.");
    } finally {
      setGenerating(false);
    }
  }

  if (isLoading || !caseRecord) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-8">
        <Spinner />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-1 text-xl font-semibold text-gray-900">Case #{caseRecord.id}</h1>
      <p className="mb-6 text-sm text-gray-500">Status: {caseRecord.status}</p>

      <Card title="Case details" className="mb-4">
        <dl className="grid grid-cols-2 gap-2 text-sm">
          {Object.entries(caseRecord.details).map(([key, value]) => (
            <div key={key} className="contents">
              <dt className="text-gray-500">{key}</dt>
              <dd className="text-gray-900">{String(value) || "—"}</dd>
            </div>
          ))}
        </dl>
      </Card>

      {caseRecord.status !== "DRAFT" && (
        <Card title="Petition" className="mb-4">
          <Button onClick={handleGenerate} disabled={generating}>
            {generating ? "Generating..." : petitions && petitions.length > 0 ? "Regenerate petition" : "Generate petition"}
          </Button>
          {error && <p className="mt-2 text-sm text-red-600">{error}</p>}

          <div className="mt-4 space-y-2">
            {petitions?.map((p) => (
              <div key={p.id} className="flex items-center justify-between rounded-md border border-gray-100 px-3 py-2">
                <div>
                  <p className="text-sm font-medium">Version {p.version}</p>
                  <p className="text-xs text-gray-500">Generated {formatIst(p.generatedAt)}</p>
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="secondary"
                    onClick={async () => triggerDownload(await downloadPetitionPdf(id, p.id), `petition-v${p.version}.pdf`)}
                  >
                    PDF
                  </Button>
                  <Button
                    variant="secondary"
                    onClick={async () => triggerDownload(await downloadPetitionDocx(id, p.id), `petition-v${p.version}.docx`)}
                  >
                    DOCX
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
