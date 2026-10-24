import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

const tones = {
  default: "bg-pine-50 text-pine-700",
  warning: "bg-amber-50 text-amber-700",
  danger: "bg-clay-50 text-clay-600",
  neutral: "bg-slate-100 text-slate-600",
};

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: keyof typeof tones;
}

export function Badge({ className, tone = "default", ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium",
        tones[tone],
        className,
      )}
      {...props}
    />
  );
}
