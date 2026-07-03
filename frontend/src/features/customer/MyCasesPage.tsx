import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { listMyCases } from "../../api/petition";
import { Card } from "../../components/Card";
import { Spinner } from "../../components/Spinner";
import { Button } from "../../components/Button";
import { formatIst } from "../../utils/format";

export function MyCasesPage() {
  const { data: cases, isLoading } = useQuery({ queryKey: ["cases"], queryFn: listMyCases });

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-900">My Cases</h1>
        <Link to="/categories">
          <Button>Start a new case</Button>
        </Link>
      </div>

      {isLoading && <Spinner />}

      {cases && cases.length === 0 && (
        <Card>
          <p className="text-sm text-gray-600">You haven't started a case yet.</p>
        </Card>
      )}

      <div className="space-y-3">
        {cases?.map((c) => (
          <Link key={c.id} to={`/cases/${c.id}`}>
            <Card className="hover:border-brand-300">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-900">Case #{c.id}</p>
                  <p className="text-xs text-gray-500">Updated {formatIst(c.updatedAt)}</p>
                </div>
                <span className="rounded-full bg-brand-50 px-3 py-1 text-xs font-medium text-brand-700">{c.status}</span>
              </div>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
