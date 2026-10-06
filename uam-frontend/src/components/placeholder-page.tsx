export function PlaceholderPage({ title }: { title: string }) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-7 sm:p-10">
      <h1 className="text-2xl font-semibold tracking-tight text-slate-900">{title}</h1>
      <p className="mt-4 text-slate-600">Chưa triển khai</p>
    </section>
  );
}
