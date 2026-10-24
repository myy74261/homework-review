import { useQuery } from "@tanstack/react-query";
import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "@/components/AppShell";
import { ErrorState, Loading } from "@/components/ui/loading";
import { loadSession } from "@/lib/api";
import { useAppStore } from "@/store/app-store";
import { TeacherDashboardPage } from "@/pages/TeacherDashboardPage";
import { PublishAssignmentPage } from "@/pages/PublishAssignmentPage";
import { ReviewWorkspacePage } from "@/pages/ReviewWorkspacePage";
import { LessonPlanPage } from "@/pages/LessonPlanPage";
import { StudentHomePage } from "@/pages/StudentHomePage";
import { StudentAssignmentPage } from "@/pages/StudentAssignmentPage";

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
          path="teacher"
          element={
            <TeacherOnly>
              <TeacherDashboardPage />
            </TeacherOnly>
          }
        />
        <Route
          path="teacher/assignments/new"
          element={
            <TeacherOnly>
              <PublishAssignmentPage />
            </TeacherOnly>
          }
        />
        <Route
          path="teacher/assignments/:assignmentId/lesson-plan"
          element={
            <TeacherOnly>
              <LessonPlanPage />
            </TeacherOnly>
          }
        />
        <Route
          path="reviews/:submissionId"
          element={
            <TeacherOnly>
              <ReviewWorkspacePage />
            </TeacherOnly>
          }
        />
        <Route
          path="student"
          element={
            <StudentOnly>
              <StudentHomePage />
            </StudentOnly>
          }
        />
        <Route
          path="student/assignments/:assignmentId"
          element={
            <StudentOnly>
              <StudentAssignmentPage />
            </StudentOnly>
          }
        />
        <Route path="*" element={<HomeRedirect />} />
      </Route>
    </Routes>
  );
}
