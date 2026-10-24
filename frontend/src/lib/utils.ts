import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDate(value?: string | null) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export const formatLabels: Record<string, string> = {
  DOCUMENT: "文档",
  CODE: "代码",
  SCREENSHOT: "截图 / 设计",
  MIXED: "混合形式",
};

export const aiPolicyLabels: Record<string, string> = {
  NOT_ALLOWED: "不允许使用 AI",
  ASSISTED_ALLOWED: "允许 AI 辅助并声明",
  GENERATION_ALLOWED: "允许生成内容并声明",
};

export const aiUsageLabels: Record<string, string> = {
  NONE: "未使用 AI",
  ASSISTED: "辅助修改或思路启发",
  PARTIAL_GENERATION: "部分内容由 AI 生成",
  MAIN_GENERATION: "主要内容由 AI 生成",
};

export const appealStatusLabels: Record<string, string> = {
  SUBMITTED: "待受理",
  UNDER_REVIEW: "复核中",
  ACCEPTED: "已采纳",
  REJECTED: "未采纳",
};

export const integrityStatusLabels: Record<string, string> = {
  SUSPECTED: "待核实",
  CONFIRMED: "已确认",
  CLEARED: "已排除",
};
