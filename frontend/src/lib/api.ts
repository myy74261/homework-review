import { useAppStore } from "@/store/app-store";
import type {
  AiReviewRun,
  AiReviewSuggestion,
  AiSettings,
  AiConnectionResult,
  Appeal,
  Assignment,
  Classroom,
  EditableAssignment,
  IntegrityFlag,
  LessonWorkspace,
  Score,
  Session,
  StudentAssignmentDetail,
  StudentDashboard,
  TeacherDashboard,
  TeacherReviewWorkspace,
  User,
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
  createClassroom: (payload: {
    name: string;
    courseName: string;
    startAt: string;
    endAt: string;
  }) =>
    request<Classroom>("/api/app/teacher/classrooms", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  updateClassroomSchedule: (classroomId: number, payload: { startAt: string; endAt: string }) =>
    request<Classroom>(`/api/app/teacher/classrooms/${classroomId}/schedule`, {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
  dissolveClassroom: (classroomId: number) =>
    request<Classroom>(`/api/app/teacher/classrooms/${classroomId}/dissolve`, {
      method: "POST",
    }),
  importClassroomRoster: (
    classroomId: number,
    students: { studentNumber: string; studentName: string }[],
  ) =>
    request<{ rosterCount: number; linkedCount: number }>(
      `/api/app/teacher/classrooms/${classroomId}/roster`,
      {
        method: "POST",
        body: JSON.stringify({ students }),
      },
    ),
  publishAssignment: (payload: unknown) =>
    request<{ id: number; status: string; redirectPath: string }>("/api/app/teacher/assignments", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  editableAssignment: (assignmentId: number) =>
    request<EditableAssignment>(`/api/app/teacher/assignments/${assignmentId}/edit`),
  withdrawAssignment: (assignmentId: number) =>
    request<{ id: number; status: string; redirectPath: string }>(
      `/api/app/teacher/assignments/${assignmentId}/withdraw`,
      {
        method: "POST",
      },
    ),
  updateAssignmentDueDate: (assignmentId: number, dueAt: string) =>
    request<Assignment>(`/api/app/teacher/assignments/${assignmentId}/due-date`, {
      method: "PUT",
      body: JSON.stringify({ dueAt }),
    }),
  updateAssignment: (assignmentId: number, payload: unknown) =>
    request<{ id: number; status: string; redirectPath: string }>(
      `/api/app/teacher/assignments/${assignmentId}`,
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    ),
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
  getAiSettings: () => request<AiSettings>("/api/app/teacher/ai-settings"),
  saveAiSettings: (payload: { provider: string; model: string; apiKey: string }) =>
    request<AiSettings>("/api/app/teacher/ai-settings", {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
  testAiSettings: (payload: { provider: string; model: string; apiKey: string }) =>
    request<AiConnectionResult>("/api/app/teacher/ai-settings/test", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
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
  updateProfile: (payload: {
    displayName: string;
    email: string;
    department: string;
    professionalTitle: string;
  }) =>
    request<User>("/api/app/account/profile", {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
  deactivateAccount: (payload: { currentPassword: string }) =>
    request<void>("/api/app/account/deactivate", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  logout: () => request<void>("/api/app/account/logout", { method: "POST" }),
};

export { ApiError };
