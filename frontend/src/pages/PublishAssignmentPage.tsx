import { useMutation, useQuery } from "@tanstack/react-query";
import { ArrowLeft, Plus, Trash2 } from "lucide-react";
import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "@/lib/api";
import { useAppStore } from "@/store/app-store";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, Input, Select, Textarea } from "@/components/ui/field";
import { ErrorState, Loading } from "@/components/ui/loading";

interface RubricDraft {
  name: string;
  points: number;
  description: string;
}

const defaults: RubricDraft[] = [
  { name: "研究问题与目标", points: 25, description: "问题、目标与研究边界是否清楚" },
  { name: "证据与引用", points: 25, description: "结论是否有可追溯的数据或文献支持" },
  { name: "方法与数据", points: 30, description: "样本、抽样、回收率与统计过程是否透明" },
  { name: "结构与表达", points: 20, description: "论证顺序、段落衔接与学术表达是否清晰" },
];

function localDateTime(days = 7) {
  const value = new Date(Date.now() + days * 24 * 60 * 60 * 1000);
  value.setSeconds(0, 0);
  const offset = value.getTimezoneOffset() * 60_000;
  return new Date(value.getTime() - offset).toISOString().slice(0, 16);
}

export function PublishAssignmentPage() {
  const navigate = useNavigate();
  const notify = useAppStore((state) => state.notify);
  const classrooms = useQuery({
    queryKey: ["teacher-classrooms"],
    queryFn: api.teacherClassrooms,
  });
  const [title, setTitle] = useState("短视频使用与大学生注意力恢复的关系研究");
  const [courseName, setCourseName] = useState("传播学方法课");
  const [classroomId, setClassroomId] = useState<number | "">("");
  const [instructions, setInstructions] = useState(
    "完成一篇调查报告，说明研究问题、研究方法、数据来源与主要发现，并附参考文献。",
  );
  const [dueAt, setDueAt] = useState(localDateTime());
  const [submissionFormat, setSubmissionFormat] = useState("MIXED");
  const [aiPolicy, setAiPolicy] = useState("ASSISTED_ALLOWED");
  const [aiDeclarationRequired, setAiDeclarationRequired] = useState(true);
  const [sourceDeclarationRequired, setSourceDeclarationRequired] = useState(true);
  const [appealAllowed, setAppealAllowed] = useState(true);
  const [rubrics, setRubrics] = useState<RubricDraft[]>(defaults);

  const selectedClassroom = classroomId || classrooms.data?.[0]?.id || "";
  const total = useMemo(
    () => rubrics.reduce((sum, rubric) => sum + Number(rubric.points || 0), 0),
    [rubrics],
  );

  const publish = useMutation({
    mutationFn: () =>
      api.publishAssignment({
        title,
        courseName,
        classroomId: selectedClassroom,
        instructions,
        dueAt,
        submissionFormat,
        aiPolicy,
        aiDeclarationRequired,
        sourceDeclarationRequired,
        appealAllowed,
        rubrics,
      }),
    onSuccess: (result) => {
      notify("作业已发布，学生端现在可以查看。", "success");
      navigate(result.redirectPath.replace("/app", ""));
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  if (classrooms.isLoading) return <Loading />;
  if (classrooms.isError) {
    return <ErrorState message={classrooms.error.message} retry={() => classrooms.refetch()} />;
  }

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link to="/teacher">
          <ArrowLeft className="size-4" />
          返回课堂
        </Link>
      </Button>

      <section>
        <p className="page-eyebrow">发布作业</p>
        <h1 className="mt-2 font-display text-4xl font-semibold tracking-tight">
          创建新的课程作业
        </h1>
        <p className="mt-3 max-w-2xl text-sm leading-7 text-muted">
          设置提交要求、截止时间和评分标准。发布后学生即可查看并提交。
        </p>
      </section>

      {classrooms.data?.length ? (
        <form
          className="space-y-4"
          onSubmit={(event) => {
            event.preventDefault();
            publish.mutate();
          }}
        >
          <Card>
            <CardHeader>
              <div>
                <p className="page-eyebrow">01 · 作业信息</p>
                <CardTitle className="mt-1">基本信息</CardTitle>
              </div>
            </CardHeader>
            <CardContent className="grid gap-4 md:grid-cols-2">
              <Field label="作业名称" className="md:col-span-2">
                <Input value={title} onChange={(event) => setTitle(event.target.value)} required />
              </Field>
              <Field label="课程名称">
                <Input
                  value={courseName}
                  onChange={(event) => setCourseName(event.target.value)}
                  required
                />
              </Field>
              <Field label="发布课堂">
                <Select
                  value={selectedClassroom}
                  onChange={(event) => setClassroomId(Number(event.target.value))}
                  required
                >
                  {classrooms.data.map((classroom) => (
                    <option key={classroom.id} value={classroom.id}>
                      {classroom.name} · {classroom.courseName}
                    </option>
                  ))}
                </Select>
              </Field>
              <Field label="截止时间">
                <Input
                  type="datetime-local"
                  value={dueAt}
                  onChange={(event) => setDueAt(event.target.value)}
                  required
                />
              </Field>
              <Field label="作业形式">
                <Select
                  value={submissionFormat}
                  onChange={(event) => setSubmissionFormat(event.target.value)}
                >
                  <option value="DOCUMENT">文档</option>
                  <option value="CODE">代码</option>
                  <option value="SCREENSHOT">截图 / 设计</option>
                  <option value="MIXED">混合形式</option>
                </Select>
              </Field>
              <Field label="AI 使用政策">
                <Select value={aiPolicy} onChange={(event) => setAiPolicy(event.target.value)}>
                  <option value="NOT_ALLOWED">不允许使用</option>
                  <option value="ASSISTED_ALLOWED">允许辅助使用</option>
                  <option value="GENERATION_ALLOWED">允许生成内容并声明</option>
                </Select>
              </Field>
              <Field label="作业要求" className="md:col-span-2">
                <Textarea
                  rows={5}
                  value={instructions}
                  onChange={(event) => setInstructions(event.target.value)}
                  required
                />
              </Field>
              <div className="grid gap-2 rounded-xl bg-pine-50 p-4 md:col-span-2 md:grid-cols-3">
                <label className="flex items-center gap-2 text-sm text-stone-700">
                  <input
                    type="checkbox"
                    checked={aiDeclarationRequired}
                    onChange={(event) => setAiDeclarationRequired(event.target.checked)}
                    className="size-4 accent-pine-600"
                  />
                  AI 使用声明
                </label>
                <label className="flex items-center gap-2 text-sm text-stone-700">
                  <input
                    type="checkbox"
                    checked={sourceDeclarationRequired}
                    onChange={(event) => setSourceDeclarationRequired(event.target.checked)}
                    className="size-4 accent-pine-600"
                  />
                  来源与数据声明
                </label>
                <label className="flex items-center gap-2 text-sm text-stone-700">
                  <input
                    type="checkbox"
                    checked={appealAllowed}
                    onChange={(event) => setAppealAllowed(event.target.checked)}
                    className="size-4 accent-pine-600"
                  />
                  允许学生申诉
                </label>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <div>
                <p className="page-eyebrow">02 · 评分量规</p>
                <CardTitle className="mt-1">评分项目</CardTitle>
                <p className="mt-1 text-xs text-muted">支持 1–10 个维度，总分必须为 100。</p>
              </div>
              <span className="rounded-full bg-stone-100 px-3 py-1.5 text-xs">
                总分{" "}
                <strong className={total === 100 ? "text-pine-700" : "text-clay-600"}>
                  {total}
                </strong>{" "}
                / 100
              </span>
            </CardHeader>
            <CardContent className="space-y-3">
              {rubrics.map((rubric, index) => (
                <div
                  key={index}
                  className="grid gap-3 rounded-xl bg-stone-50 p-3 md:grid-cols-[minmax(150px,0.8fr)_90px_minmax(220px,1.3fr)_auto] md:items-end"
                >
                  <Field label={`维度 ${index + 1}`}>
                    <Input
                      value={rubric.name}
                      onChange={(event) =>
                        setRubrics((items) =>
                          items.map((item, itemIndex) =>
                            itemIndex === index ? { ...item, name: event.target.value } : item,
                          ),
                        )
                      }
                      required
                    />
                  </Field>
                  <Field label="分值">
                    <Input
                      type="number"
                      min={1}
                      max={100}
                      value={rubric.points}
                      onChange={(event) =>
                        setRubrics((items) =>
                          items.map((item, itemIndex) =>
                            itemIndex === index
                              ? { ...item, points: Number(event.target.value) }
                              : item,
                          ),
                        )
                      }
                      required
                    />
                  </Field>
                  <Field label="观察说明">
                    <Input
                      value={rubric.description}
                      onChange={(event) =>
                        setRubrics((items) =>
                          items.map((item, itemIndex) =>
                            itemIndex === index
                              ? { ...item, description: event.target.value }
                              : item,
                          ),
                        )
                      }
                      required
                    />
                  </Field>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    disabled={rubrics.length === 1}
                    onClick={() =>
                      setRubrics((items) => items.filter((_, itemIndex) => itemIndex !== index))
                    }
                    aria-label="删除评分维度"
                  >
                    <Trash2 className="size-4 text-muted" />
                  </Button>
                </div>
              ))}
              <Button
                type="button"
                variant="secondary"
                disabled={rubrics.length >= 10}
                onClick={() =>
                  setRubrics((items) => [...items, { name: "", points: 0, description: "" }])
                }
              >
                <Plus className="size-4" />
                增加评分维度
              </Button>
            </CardContent>
          </Card>

          <div className="flex justify-end gap-3">
            <Button asChild variant="secondary">
              <Link to="/teacher">取消</Link>
            </Button>
            <Button type="submit" size="lg" disabled={publish.isPending || total !== 100}>
              {publish.isPending ? "发布中…" : "发布作业并开放提交"}
            </Button>
          </div>
        </form>
      ) : (
        <Card className="p-8 text-center">
          <h2 className="font-display text-2xl font-semibold">先创建一个课堂</h2>
          <p className="mt-2 text-sm text-muted">学生加入课堂后才能看到作业。</p>
          <Button asChild className="mt-5">
            <Link to="/teacher">返回课堂创建</Link>
          </Button>
        </Card>
      )}
    </div>
  );
}
