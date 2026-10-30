import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Download, RefreshCw, Save } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import { useAppStore } from "@/store/app-store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input, Textarea } from "@/components/ui/field";
import { ErrorState, Loading } from "@/components/ui/loading";

export function LessonPlanPage() {
  const { assignmentId } = useParams();
  const id = Number(assignmentId);
  const queryClient = useQueryClient();
  const notify = useAppStore((state) => state.notify);
  const workspace = useQuery({
    queryKey: ["lesson-plan", id],
    queryFn: () => api.lessonWorkspace(id),
    enabled: Number.isFinite(id),
  });
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [revision, setRevision] = useState(0);
  const [dirty, setDirty] = useState(false);

  useEffect(() => {
    if (!workspace.data || dirty) return;
    setTitle(workspace.data.title);
    setContent(workspace.data.content);
    setRevision(workspace.data.revision);
  }, [dirty, workspace.data]);

  const save = useMutation({
    mutationFn: () => api.saveLessonPlan(id, { title, content, baseRevision: revision }),
    onSuccess: async () => {
      setRevision((value) => value + 1);
      setDirty(false);
      notify("备课稿已保存。", "success");
      await queryClient.invalidateQueries({ queryKey: ["lesson-plan", id] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const regenerate = useMutation({
    mutationFn: () => api.regenerateLessonPlan(id),
    onSuccess: async () => {
      setDirty(false);
      notify("已根据最新共性问题重新生成。", "success");
      await queryClient.invalidateQueries({ queryKey: ["lesson-plan", id] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  if (workspace.isLoading) return <Loading label="正在整理备课素材…" />;
  if (workspace.isError) {
    return <ErrorState message={workspace.error.message} retry={() => workspace.refetch()} />;
  }

  const data = workspace.data!;
  return (
    <div className="space-y-4">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link to="/teacher">
          <ArrowLeft className="size-4" />
          返回课堂
        </Link>
      </Button>

      <section className="flex flex-col justify-between gap-4 rounded-[26px] border border-line bg-white/85 p-6 shadow-card lg:flex-row lg:items-end">
        <div>
          <p className="page-eyebrow">备课建议</p>
          <h1 className="mt-2 font-display text-3xl font-semibold">{data.assignment.title}</h1>
          <p className="mt-2 text-sm text-muted">
            {data.evidenceCount} 条教师确认证据 · 最后更新 {formatDate(data.updatedAt)} ·{" "}
            {data.owner
              ? data.aiConfigured
                ? "AI 增强已启用"
                : "规则生成模式"
              : "协作者只读，备课稿由课堂所有者维护"}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {data.owner ? (
            <>
              <Button
                variant="secondary"
                disabled={regenerate.isPending}
                onClick={() => regenerate.mutate()}
              >
                <RefreshCw className="size-4" />
                {data.aiConfigured ? "AI 重新生成" : "重新生成"}
              </Button>
              <Button disabled={!dirty || save.isPending} onClick={() => save.mutate()}>
                <Save className="size-4" />
                {save.isPending ? "保存中…" : "保存备课稿"}
              </Button>
            </>
          ) : null}
          <Button asChild variant="outline">
            <a href={`/teacher/assignments/${id}/lesson-plan/export.md`} download>
              <Download className="size-4" />
              Markdown
            </a>
          </Button>
        </div>
      </section>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_330px]">
        <Card className="min-h-[640px]">
          <CardHeader>
            <div className="flex-1">
              <p className="page-eyebrow">备课稿</p>
              <Input
                value={title}
                disabled={!data.owner}
                onChange={(event) => {
                  setTitle(event.target.value);
                  setDirty(true);
                }}
                className="mt-2 h-auto border-0 px-0 font-display text-2xl font-semibold shadow-none focus:ring-0"
              />
            </div>
          </CardHeader>
          <CardContent>
            <Textarea
              value={content}
              disabled={!data.owner}
              onChange={(event) => {
                setContent(event.target.value);
                setDirty(true);
              }}
              className="min-h-[560px] border-0 bg-transparent px-0 font-display text-[15px] leading-8 shadow-none focus:ring-0"
            />
          </CardContent>
        </Card>

        <aside className="space-y-4">
          <Card>
            <CardHeader>
              <div>
                <p className="page-eyebrow">问题汇总</p>
                <CardTitle className="mt-1">共性问题</CardTitle>
              </div>
              <Badge>{data.issueGroups.length} 类</Badge>
            </CardHeader>
            <CardContent className="space-y-3">
              {data.issueGroups.map((group) => (
                <div key={group.rubricName} className="rounded-xl border border-line p-3">
                  <div className="flex items-center justify-between">
                    <strong className="text-sm">{group.rubricName}</strong>
                    <span className="text-xs text-clay-600">{group.count} 处</span>
                  </div>
                  <ul className="mt-2 space-y-1 text-xs leading-5 text-muted">
                    {group.teacherNotes.slice(0, 3).map((note) => (
                      <li key={note} className="line-clamp-2">
                        · {note}
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
              {data.issueGroups.length === 0 ? (
                <p className="rounded-xl border border-dashed border-line p-4 text-sm leading-6 text-muted">
                  当前还没有已确认的共性问题，请先在审核工作台确认。
                </p>
              ) : null}
            </CardContent>
          </Card>

          <Card className="bg-pine-50/70">
            <CardContent className="pt-5 text-xs leading-6 text-pine-700">
              {data.aiConfigured
                ? "AI 根据教师确认的问题整理备课稿，最终内容由教师修改后使用。"
                : "当前使用规则整理教师确认的问题，最终内容由教师修改后使用。"}
            </CardContent>
          </Card>
        </aside>
      </div>
    </div>
  );
}
