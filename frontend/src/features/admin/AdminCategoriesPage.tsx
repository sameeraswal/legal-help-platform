import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createCategory, listAllCategoriesAdmin, updateCategory } from "../../api/petition";
import type { CaseCategory } from "../../api/types/petition";
import { Card } from "../../components/Card";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import { Spinner } from "../../components/Spinner";

const EMPTY = { slug: "", name: "", description: "", templateKey: "", active: true };

export function AdminCategoriesPage() {
  const queryClient = useQueryClient();
  const { data: categories, isLoading } = useQuery({ queryKey: ["admin-categories"], queryFn: listAllCategoriesAdmin });
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState<number | null>(null);

  const saveMutation = useMutation({
    mutationFn: () => (editingId ? updateCategory(editingId, form) : createCategory(form)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-categories"] });
      setForm(EMPTY);
      setEditingId(null);
    },
  });

  function startEdit(category: CaseCategory) {
    setEditingId(category.id);
    setForm({
      slug: category.slug,
      name: category.name,
      description: category.description ?? "",
      templateKey: category.templateKey,
      active: category.active,
    });
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Case Categories</h1>

      <Card title={editingId ? "Edit category" : "New category"} className="mb-6">
        <div className="grid gap-3 sm:grid-cols-2">
          <Input placeholder="Slug" value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} />
          <Input placeholder="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <Input
            placeholder="Prompt template key"
            value={form.templateKey}
            onChange={(e) => setForm({ ...form, templateKey: e.target.value })}
          />
          <Input
            placeholder="Description"
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
        </div>
        <Button className="mt-3" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
          {editingId ? "Save changes" : "Create category"}
        </Button>
      </Card>

      {isLoading && <Spinner />}
      <div className="space-y-2">
        {categories?.map((category) => (
          <button key={category.id} onClick={() => startEdit(category)} className="w-full text-left">
            <Card className="hover:border-brand-300">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-900">{category.name}</p>
                  <p className="text-xs text-gray-500">{category.slug}</p>
                </div>
                <span className={`text-xs ${category.active ? "text-green-600" : "text-gray-400"}`}>
                  {category.active ? "Active" : "Inactive"}
                </span>
              </div>
            </Card>
          </button>
        ))}
      </div>
    </div>
  );
}
