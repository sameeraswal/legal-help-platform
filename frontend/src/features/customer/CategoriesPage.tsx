import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { listCategories, startCase } from "../../api/petition";
import { Card } from "../../components/Card";
import { Spinner } from "../../components/Spinner";
import { useState } from "react";

export function CategoriesPage() {
  const { data: categories, isLoading } = useQuery({ queryKey: ["categories"], queryFn: listCategories });
  const navigate = useNavigate();
  const [starting, setStarting] = useState<number | null>(null);

  async function handleSelect(categoryId: number) {
    setStarting(categoryId);
    try {
      const created = await startCase(categoryId, {});
      navigate(`/cases/${created.id}/intake`);
    } finally {
      setStarting(null);
    }
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Select a case category</h1>
      {isLoading && <Spinner />}
      <div className="grid gap-3 sm:grid-cols-2">
        {categories?.map((category) => (
          <button key={category.id} onClick={() => handleSelect(category.id)} disabled={starting !== null} className="text-left">
            <Card className="h-full hover:border-brand-300 disabled:opacity-50">
              <p className="text-sm font-medium text-gray-900">{category.name}</p>
              <p className="mt-1 text-xs text-gray-500">{category.description}</p>
              {starting === category.id && <p className="mt-2 text-xs text-brand-600">Starting...</p>}
            </Card>
          </button>
        ))}
      </div>
    </div>
  );
}
