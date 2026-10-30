import { useQuery } from "@tanstack/react-query";
import { lazy, Suspense, type ReactNode } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "@/components/AppShell";
import { ErrorState, Loading } from "@/components/ui/loading";
import { loadSession } from "@/lib/api";
import { useAppStore } from "@/store/app-store";

const TeacherDashboardPage = lazy(() =>
  import("@/pages/TeacherDashboardPage").then((module) => ({
    default: module.TeacherDashboardPage,
  })),
);
const PublishAssignmentPage = lazy(() =>
  import("@/pages/PublishAssignmentPage").then((module) => ({
    default: module.PublishAssignmentPage,
  })),
);
const ReviewWorkspacePage = lazy(() =>
  import("@/pages/ReviewWorkspacePage").then((module) => ({
    default: module.ReviewWorkspacePage,
  })),
);
const LessonPlanPage = lazy(() =>
  import("@/pages/LessonPlanPage").then((module) => ({
    default: module.LessonPlanPage,
  })),
);
const StudentHomePage = lazy(() =>
  import("@/pages/StudentHomePage").then((module) => ({
    default: module.StudentHomePage,
  })),
);
const StudentAssignmentPage = lazy(() =>
  import("@/pages/StudentAssignmentPage").then((module) => ({
    default: module.StudentAssignmentPage,
  })),
);
const AccountPage = lazy(() =>
  import("@/pages/AccountPage").then((module) => ({
    default: module.AccountPage,
  })),
);
const AiSettingsPage = lazy(() =>
  import("@/pages/AiSettingsPage").then((module) => ({
    default: module.AiSettingsPage,
  })),
);

function HomeRedirect() {
  const session = useAppStore((state) => state.session);
  return <Navigate to={session?.homePath.replace("/app", "") ?? "/"} replace />;
}

function TeacherOnly({ children }: { children: React.ReactNode }) {
  const session = useAppStore((state) => state.session);
  return session?.user.role === "TEACHER" ? children : <Navigate to="/student" replace />;
}

function StudentOnly({ children }: { children: React.ReactNode }) {
  const session = useAppStore((state) => state.session);
  return session?.user.role === "STUDENT" ? children : <Navigate to="/teacher" replace />;
}

function LazyPage({ children }: { children: ReactNode }) {
  return <Suspense fallback={<Loading label="正在加载页面…" />}>{children}</Suspense>;
}

function TeacherPage({ children }: { children: ReactNode }) {
  return (
    <TeacherOnly>
      <LazyPage>{children}</LazyPage>
    </TeacherOnly>
  );
}

function StudentPage({ children }: { children: ReactNode }) {
  return (
    <StudentOnly>
      <LazyPage>{children}</LazyPage>
    </StudentOnly>
  );
}

export default function App() {
  const setSession = useAppStore((state) => state.setSession);
  const sessionQuery = useQuery({
    queryKey: ["session"],
    queryFn: async () => {
      const session = await loadSession();
      setSession(session);
      return session;
    },
    retry: false,
    staleTime: 5 * 60 * 1000,
  });

  if (sessionQuery.isLoading) return <Loading label="正在进入明鉴工作台…" />;
  if (sessionQuery.isError || !sessionQuery.data) {
    return (
      <div className="mx-auto max-w-xl p-8">
        <ErrorState
          message="登录状态无法确认，请返回登录页重试。"
          retry={() => window.location.assign("/login")}
        />
      </div>
    );
  }

  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<HomeRedirect />} />
        <Route
          path="account"
          element={
            <LazyPage>
              <AccountPage />
            </LazyPage>
          }
        />
        <Route
          path="teacher/ai-settings"
          element={
            <TeacherPage>
              <AiSettingsPage />
            </TeacherPage>
          }
        />
        <Route
          path="teacher"
          element={
            <TeacherPage>
              <TeacherDashboardPage />
            </TeacherPage>
          }
        />
        <Route
          path="teacher/assignments/new"
          element={
            <TeacherPage>
              <PublishAssignmentPage />
            </TeacherPage>
          }
        />
        <Route
          path="teacher/assignments/:assignmentId/edit"
          element={
            <TeacherPage>
              <PublishAssignmentPage />
            </TeacherPage>
          }
        />
        <Route
          path="teacher/assignments/:assignmentId/lesson-plan"
          element={
            <TeacherPage>
              <LessonPlanPage />
            </TeacherPage>
          }
        />
        <Route
          path="reviews/:submissionId"
          element={
            <TeacherPage>
              <ReviewWorkspacePage />
            </TeacherPage>
          }
        />
        <Route
          path="student"
          element={
            <StudentPage>
              <StudentHomePage />
            </StudentPage>
          }
        />
        <Route
          path="student/assignments/:assignmentId"
          element={
            <StudentPage>
              <StudentAssignmentPage />
            </StudentPage>
          }
        />
        <Route path="*" element={<HomeRedirect />} />
      </Route>
    </Routes>
  );
}
