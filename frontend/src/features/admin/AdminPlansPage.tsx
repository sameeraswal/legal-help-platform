import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createPlan, listAllPlansAdmin, updatePlan } from "../../api/billing";
import type { Plan } from "../../api/types/billing";
import { Card } from "../../components/Card";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import { Spinner } from "../../components/Spinner";
import { formatDuration, formatInr } from "../../utils/format";

const EMPTY = { name: "", priceMinorUnits: 0, seconds: 0, active: true };

export function AdminPlansPage() {
  const queryClient = useQueryClient();
  const { data: plans, isLoading } = useQuery({ queryKey: ["admin-plans"], queryFn: listAllPlansAdmin });
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState<number | null>(null);

  const saveMutation = useMutation({
    mutationFn: () => (editingId ? updatePlan(editingId, form) : createPlan(form)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-plans"] });
      setForm(EMPTY);
      setEditingId(null);
    },
  });

  function startEdit(plan: Plan) {
    setEditingId(plan.id);
    setForm({ name: plan.name, priceMinorUnits: plan.priceMinorUnits, seconds: plan.seconds, active: plan.active });
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Plans</h1>

      <Card title={editingId ? "Edit plan" : "New plan"} className="mb-6">
        <div className="grid gap-3 sm:grid-cols-3">
          <Input placeholder="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <Input
            type="number"
            placeholder="Price (paise)"
            value={form.priceMinorUnits}
            onChange={(e) => setForm({ ...form, priceMinorUnits: Number(e.target.value) })}
          />
          <Input
            type="number"
            placeholder="Seconds"
            value={form.seconds}
            onChange={(e) => setForm({ ...form, seconds: Number(e.target.value) })}
          />
        </div>
        <Button className="mt-3" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
          {editingId ? "Save changes" : "Create plan"}
        </Button>
      </Card>

      {isLoading && <Spinner />}
      <div className="space-y-2">
        {plans?.map((plan) => (
          <button key={plan.id} onClick={() => startEdit(plan)} className="w-full text-left">
            <Card className="hover:border-brand-300">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-900">{plan.name}</p>
                  <p className="text-xs text-gray-500">
                    {formatInr(plan.priceMinorUnits)} · {formatDuration(plan.seconds)}
                  </p>
                </div>
                <span className={`text-xs ${plan.active ? "text-green-600" : "text-gray-400"}`}>
                  {plan.active ? "Active" : "Inactive"}
                </span>
              </div>
            </Card>
          </button>
        ))}
      </div>
    </div>
  );
}
