import { useAppStore } from "@/store/app-store";
import type {
  AiReviewRun,
  AiReviewSuggestion,
  Appeal,
  Classroom,
  IntegrityFlag,
  LessonWorkspace,
  Score,
  Session,
  StudentAssignmentDetail,
  StudentDashboard,
  TeacherDashboard,
  TeacherReviewWorkspace,
} from "@/lib/types";

class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

interface RequestOptions extends RequestInit {
  skipCsrf?: boolean;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const session = useAppStore.getState().session;
  const method = (options.method ?? "GET").toUpperCase();
  const headers = new Headers(options.headers);

  if (!(options.body instanceof FormData)) {
    headers.set("Accept", "application/json");
    if (options.body && !headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }
  } else {
    headers.set("Accept", "application/json");
  }

  if (session && method !== "GET" && method !== "HEAD" && !options.skipCsrf) {
    headers.set(session.csrfHeader, session.csrfToken);
  }

  const response = await fetch(path, { ...options, headers, credentials: "same-origin" });
  const contentType = response.headers.get("content-type") ?? "";
  if (response.redirected && contentType.includes("text/html")) {
    window.location.assign("/login");
    throw new ApiError("登录状态已失效", 401);
  }

  if (!response.ok) {
    const problem = contentType.includes("application/json")
      ? await response.json().catch(() => null)
      : null;
    throw new ApiError(problem?.message ?? `请求失败（${response.status}）`, response.status);
  }

  if (response.status === 204) return undefined as T;
  if (!contentType.includes("application/json")) {
    window.location.assign("/login");
    throw new ApiError("登录状态已失效", 401);
  }
  return response.json() as Promise<T>;
}

export async function loadSession(): Promise<Session> {
  const response = await fetch("/api/app/session", {
    credentials: "same-origin",
    headers: { Accept: "application/json" },
  });
  const contentType = response.headers.get("content-type") ?? "";
  if (!response.ok || !contentType.includes("application/json")) {
    window.location.assign("/login");
    throw new ApiError("请先登录", 401);
  }
  const session = (await response.json()) as Session;
  useAppStore.getState().setSession(session);
  return session;
}

export const api = {
  teacherDashboard: () => request<TeacherDashboard>("/api/app/teacher/dashboard"),
  studentDashboard: () => request<StudentDashboard>("/api/app/student/dashboard"),
  teacherClassrooms: () => request<Classroom[]>("/api/app/teacher/classrooms"),
  createClassroom: (payload: { name: string; courseName: string }) =>
    request<Classroom>("/api/app/teacher/classrooms", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  publishAssignment: (payload: unknown) =>
    request<{ id: number; redirectPath: string }>("/api/app/teacher/assignments", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  reviewWorkspace: (submissionId: number) =>
    request<TeacherReviewWorkspace>(`/api/app/teacher/reviews/${submissionId}`),
  saveReview: (
    submissionId: number,
    payload: { title: string; content: string; baseRevision: number },
  ) =>
    request<{ revision: number; versionCount: number; updatedAt: string }>(
      `/api/submissions/${submissionId}/review`,
      { method: "PUT", body: JSON.stringify(payload) },
    ),
  saveVersion: (submissionId: number) =>
    request<{ versionNumber: number }>(`/api/submissions/${submissionId}/review/versions`, {
      method: "POST",
    }),
  completeReview: (submissionId: number) =>
    request<void>(`/api/submissions/${submissionId}/review/complete`, { method: "POST" }),
  saveScores: (submissionId: number, scores: Record<number, number>) =>
    request<Score[]>(`/api/app/teacher/submissions/${submissionId}/scores`, {
      method: "PUT",
      body: JSON.stringify({ scores }),
    }),
  createIntegrityFlag: (
    submissionId: number,
    payload: { category: string; severity: string; evidence: string; reason: string },
  ) =>
    request<IntegrityFlag>(`/api/app/teacher/submissions/${submissionId}/integrity-flags`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  resolveIntegrityFlag: (flagId: number, status: "CONFIRMED" | "CLEARED") =>
    request<IntegrityFlag>(`/api/app/teacher/integrity-flags/${flagId}/resolve`, {
      method: "POST",
      body: JSON.stringify({ status }),
    }),
  respondAppeal: (
    appealId: number,
    payload: { status: "UNDER_REVIEW" | "ACCEPTED" | "REJECTED"; teacherResponse: string },
  ) =>
    request<Appeal>(`/api/app/teacher/appeals/${appealId}/respond`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  generateAiReview: (submissionId: number) =>
    request<AiReviewRun>(`/api/app/teacher/reviews/${submissionId}/ai-review`, { method: "POST" }),
  adoptAiSuggestion: (suggestionId: number, teacherNote?: string) =>
    request<AiReviewSuggestion>(`/api/app/teacher/ai-review/suggestions/${suggestionId}/adopt`, {
      method: "POST",
      body: teacherNote ? JSON.stringify({ teacherNote }) : undefined,
    }),
  dismissAiSuggestion: (suggestionId: number) =>
    request<AiReviewSuggestion>(`/api/app/teacher/ai-review/suggestions/${suggestionId}/dismiss`, {
      method: "POST",
    }),
  lessonWorkspace: (assignmentId: number) =>
    request<LessonWorkspace>(`/api/app/teacher/assignments/${assignmentId}/lesson-plan`),
  saveLessonPlan: (
    assignmentId: number,
    payload: { title: string; content: string; baseRevision: number },
  ) =>
    request<{ revision: number; updatedAt: string }>(
      `/api/assignments/${assignmentId}/lesson-plan`,
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    ),
  regenerateLessonPlan: (assignmentId: number) =>
    request<{ revision: number; updatedAt: string }>(
      `/api/assignments/${assignmentId}/lesson-plan/regenerate`,
      { method: "POST" },
    ),
  studentAssignment: (assignmentId: number) =>
    request<StudentAssignmentDetail>(`/api/app/student/assignments/${assignmentId}`),
  joinClassroom: (joinCode: string) =>
    request<Classroom>("/api/app/student/classrooms/join", {
      method: "POST",
      body: JSON.stringify({ joinCode }),
    }),
  submitAssignment: (assignmentId: number, form: FormData) =>
    request<{ success: boolean; message: string; redirectUrl: string }>(
      `/student/assignments/${assignmentId}/submissions/ajax`,
      { method: "POST", body: form },
    ),
  submitAppeal: (submissionId: number, payload: { reason: string; requestedResolution: string }) =>
    request<Appeal>(`/api/app/student/submissions/${submissionId}/appeals`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  changePassword: (payload: { currentPassword: string; newPassword: string }) =>
    request<void>("/api/app/account/password", {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
  logout: () => request<void>("/logout", { method: "POST" }),
};

export { ApiError };
