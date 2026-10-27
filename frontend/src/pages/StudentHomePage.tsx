import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, BookOpen, CheckCircle2, Clock3, KeyRound } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import { useAppStore } from "@/store/app-store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, Input } from "@/components/ui/field";
import { ErrorState, Loading } from "@/components/ui/loading";

export function StudentHomePage() {
  const queryClient = useQueryClient();
  const notify = useAppStore((state) => state.notify);
  const [joinCode, setJoinCode] = useState("");
  const dashboard = useQuery({
    queryKey: ["student-dashboard"],
    queryFn: api.studentDashboard,
  });

  const join = useMutation({
    mutationFn: () => api.joinClassroom(joinCode),
    onSuccess: async (classroom) => {
      setJoinCode("");
      notify(`已加入课堂：${classroom.name}`, "success");
      await queryClient.invalidateQueries({ queryKey: ["student-dashboard"] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  if (dashboard.isLoading) return <Loading />;
  if (dashboard.isError) {
    return <ErrorState message={dashboard.error.message} retry={() => dashboard.refetch()} />;
  }

  const data = dashboard.data!;
  const pending = data.assignments.filter((item) => !item.latestSubmission).length;
  const feedback = data.assignments.filter((item) => item.statusText === "已反馈").length;

  return (
    <div className="space-y-5">
      <section className="grid gap-5 rounded-[28px] border border-line bg-white/85 p-6 shadow-card lg:grid-cols-[minmax(0,1fr)_380px] lg:p-8">
        <div>
          <p className="page-eyebrow">你好，{data.user.displayName}</p>
          <h1 className="mt-2 max-w-2xl font-display text-4xl font-semibold leading-tight tracking-tight sm:text-5xl">
            作业与反馈
          </h1>
          <p className="mt-4 max-w-xl text-sm leading-7 text-muted">
            查看作业要求、提交记录和教师反馈。
          </p>
          <div className="mt-5 flex gap-3">
            <Badge>{data.assignments.length} 项作业</Badge>
            <Badge tone="warning">{pending} 项待提交</Badge>
            <Badge>{feedback} 项已反馈</Badge>
          </div>
        </div>

        <Card className="bg-stone-50/80 shadow-none">
          <CardHeader>
            <div>
              <p className="page-eyebrow">加入课堂</p>
              <CardTitle className="mt-1">输入教师提供的课堂码</CardTitle>
            </div>
            <KeyRound className="size-5 text-pine-600" />
          </CardHeader>
          <CardContent>
            <form
              className="space-y-3"
              onSubmit={(event) => {
                event.preventDefault();
                join.mutate();
              }}
            >
              <Field label="课堂码">
                <Input
                  value={joinCode}
                  onChange={(event) => setJoinCode(event.target.value.toUpperCase())}
                  maxLength={12}
                  placeholder="例如 A2B3C4D5"
                  required
                />
              </Field>
              <Button className="w-full" disabled={join.isPending || !joinCode.trim()}>
                {join.isPending ? "加入中…" : "加入课堂"}
              </Button>
            </form>
            {data.classrooms.length ? (
              <div className="mt-4 flex flex-wrap gap-2">
                {data.classrooms.map((classroom) => (
                  <span
                    key={classroom.id}
                    className="rounded-lg bg-white px-2.5 py-1.5 text-xs text-muted"
                  >
                    {classroom.name}
                  </span>
                ))}
              </div>
            ) : null}
          </CardContent>
        </Card>
      </section>

      <section>
        <div className="mb-3 flex items-end justify-between">
          <div>
            <p className="page-eyebrow">学习任务</p>
            <h2 className="section-title mt-1">我的作业</h2>
          </div>
          <span className="text-xs text-muted">作业与反馈实时同步</span>
        </div>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {data.assignments.map((item) => (
            <Card key={item.assignment.id} className="group flex flex-col p-5">
              <div className="flex items-center justify-between gap-3">
                <Badge
                  tone={
                    item.statusText === "已反馈"
                      ? "default"
                      : item.statusText === "待提交"
                        ? "warning"
                        : "neutral"
                  }
                >
                  {item.statusText}
                </Badge>
                <span className="text-xs text-muted">{item.assignment.courseName}</span>
              </div>
              <h3 className="mt-3 font-display text-xl font-semibold leading-7">
                {item.assignment.title}
              </h3>
              <p className="mt-2 line-clamp-3 flex-1 text-sm leading-6 text-muted">
                {item.assignment.instructions}
              </p>
              <div className="mt-4 grid gap-2 border-t border-line pt-3 text-xs text-muted">
                <span className="flex items-center gap-2">
                  <Clock3 className="size-3.5" />
                  截止 {formatDate(item.assignment.dueAt)}
                </span>
                <span className="flex items-center gap-2">
                  {item.latestSubmission ? (
                    <CheckCircle2 className="size-3.5 text-pine-600" />
                  ) : (
                    <BookOpen className="size-3.5" />
                  )}
                  {item.latestSubmission
                    ? `第 ${item.latestSubmission.attemptNumber} 次提交`
                    : "尚未提交"}
                </span>
              </div>
              <Button asChild className="mt-4 w-full" variant="secondary">
                <Link to={`/student/assignments/${item.assignment.id}`}>
                  {item.latestSubmission ? "查看提交与反馈" : "查看要求并提交"}
                  <ArrowRight className="size-4 transition group-hover:translate-x-0.5" />
                </Link>
              </Button>
            </Card>
          ))}
        </div>
        {data.assignments.length === 0 ? (
          <Card className="p-10 text-center">
            <h2 className="font-display text-2xl font-semibold">还没有可查看的作业</h2>
            <p className="mt-2 text-sm text-muted">请使用教师提供的课堂码加入课堂。</p>
          </Card>
        ) : null}
      </section>
    </div>
  );
}
