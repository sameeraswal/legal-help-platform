import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getGlobalRateHistory, setLawyerRate } from "../../api/billing";
import { Card } from "../../components/Card";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import { Spinner } from "../../components/Spinner";
import { formatInr, formatIst } from "../../utils/format";

export function AdminLawyerRatesPage() {
  const { data: history, isLoading, refetch } = useQuery({ queryKey: ["global-rate-history"], queryFn: getGlobalRateHistory });
  const [globalRate, setGlobalRate] = useState(0);
  const [lawyerId, setLawyerId] = useState("");
  const [lawyerRate, setLawyerRateValue] = useState(0);

  async function saveGlobal() {
    await setLawyerRate(null, globalRate);
    refetch();
  }

  async function saveLawyer() {
    if (!lawyerId) return;
    await setLawyerRate(Number(lawyerId), lawyerRate);
    setLawyerId("");
    setLawyerRateValue(0);
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Lawyer Rates</h1>

      <Card title="Global default rate (₹ paise / minute)" className="mb-6">
        <div className="flex gap-2">
          <Input type="number" value={globalRate} onChange={(e) => setGlobalRate(Number(e.target.value))} />
          <Button onClick={saveGlobal}>Set</Button>
        </div>

        {isLoading ? (
          <Spinner />
        ) : (
          <div className="mt-4 space-y-1">
            {history?.map((r) => (
              <div key={r.id} className="flex justify-between text-sm text-gray-600">
                <span>{formatInr(r.perMinuteRateMinorUnits)} / min</span>
                <span>{formatIst(r.effectiveFrom)}</span>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Card title="Per-lawyer override">
        <div className="grid grid-cols-2 gap-2">
          <Input placeholder="Lawyer ID" value={lawyerId} onChange={(e) => setLawyerId(e.target.value)} />
          <Input type="number" placeholder="Rate (paise/min)" value={lawyerRate} onChange={(e) => setLawyerRateValue(Number(e.target.value))} />
        </div>
        <Button className="mt-3" onClick={saveLawyer}>
          Set override
        </Button>
      </Card>
    </div>
  );
}
