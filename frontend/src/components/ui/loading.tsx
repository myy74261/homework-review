import { LoaderCircle } from "lucide-react";

export function Loading({ label = "正在加载…" }: { label?: string }) {
  return (
    <div className="grid min-h-[45vh] place-items-center text-sm text-muted">
      <div className="flex items-center gap-3">
        <LoaderCircle className="size-5 animate-spin text-pine-600" />
        {label}
      </div>
    </div>
  );
}

export function ErrorState({ message, retry }: { message: string; retry?: () => void }) {
  return (
    <div className="rounded-2xl border border-clay-500/20 bg-clay-50 p-6 text-sm text-clay-600">
      <p>{message}</p>
      {retry ? (
        <button
          type="button"
          onClick={retry}
          className="mt-3 rounded-lg border border-clay-500/30 px-3 py-1.5 font-medium"
        >
          重试
        </button>
      ) : null}
    </div>
  );
}
