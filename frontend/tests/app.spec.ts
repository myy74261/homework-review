import { expect, test } from "@playwright/test";

async function login(page: import("@playwright/test").Page, username: string, password: string) {
  await page.goto("/login");
  await page.getByLabel("账号").fill(username);
  await page.getByLabel("密码").fill(password);
  await page.getByRole("button", { name: "登录", exact: true }).click();
}

test("teacher React workspace renders dashboard, review and lesson pages", async ({ page }) => {
  await login(page, "chen", "Teach1234");
  await expect(page).toHaveURL(/\/app\/teacher$/);
  await expect(page.getByRole("heading", { name: "作业审核与课堂反馈" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "我的课堂", exact: true })).toBeVisible();
  await expect(page.getByRole("tab", { name: /概览/ })).toHaveAttribute("aria-selected", "true");
  await page.screenshot({ path: "test-results/teacher-dashboard.png", fullPage: true });

  await page.getByRole("tab", { name: /作业管理/ }).click();
  await expect(page.getByRole("heading", { name: "已发布作业" })).toBeVisible();
  await page.getByRole("tab", { name: /AI 预审/ }).click();
  await expect(page.getByRole("heading", { name: "批量预审进度" })).toBeVisible();
  await expect(page.getByText(/作业截止后自动开始|尚未接入模型/)).toBeVisible();

  await page.goto("/app/reviews/1");
  await expect(page.getByRole("heading", { name: "预审建议" })).toBeVisible();
  await expect(page.getByText(/当前未配置 AI 接口/)).toBeVisible();
  await expect(page.getByRole("heading", { name: "评分建议" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "诚信核验" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "提交内容" })).toBeVisible();
  const editor = page.getByLabel("教师审核意见正文");
  await editor.fill(`${await editor.inputValue()}\n\nReact 审核台自动保存验证。`);
  await expect(page.getByText(/已保存 ·/)).toBeVisible();

  await page.goto("/app/teacher/assignments/1/lesson-plan");
  await expect(page.getByText("备课建议")).toBeVisible();
  await expect(page.getByText("规则生成模式").or(page.getByText("AI 增强已启用"))).toBeVisible();
});

test("teacher can withdraw, edit and republish an assignment", async ({ page }) => {
  await login(page, "chen", "Teach1234");
  await page.getByRole("tab", { name: /作业管理/ }).click();

  const assignmentCard = page.getByTestId("assignment-card-1");
  await expect(assignmentCard.getByText("已发布")).toBeVisible();
  await assignmentCard.getByRole("button", { name: "撤回并修改" }).click();
  await expect(page.getByRole("heading", { name: "撤回作业并修改" })).toBeVisible();
  await page.getByRole("button", { name: "确认撤回" }).click();

  await expect(page).toHaveURL(/\/app\/teacher\/assignments\/1\/edit$/);
  await expect(page.getByRole("heading", { name: "修改后重新发布" })).toBeVisible();
  await page.screenshot({ path: "test-results/teacher-assignment-edit.png", fullPage: true });
  await page.getByLabel("作业名称").fill("短视频使用与注意力恢复（修订版）");
  await page.getByLabel("截止时间").fill("2099-06-30T23:59");
  await page.getByRole("button", { name: "保存修改并重新发布" }).click();

  await expect(page).toHaveURL(/\/app\/teacher$/);
  await page.getByRole("tab", { name: /作业管理/ }).click();
  await expect(page.getByTestId("assignment-card-1").getByText("修订版")).toBeVisible();
  await expect(page.getByTestId("assignment-card-1").getByText("已发布")).toBeVisible();
});

test("student React workspace renders assignment and submission details", async ({ page }) => {
  await login(page, "lin", "Student1234");
  await expect(page).toHaveURL(/\/app\/student$/);
  await expect(page.getByRole("heading", { name: "我的作业" })).toBeVisible();

  await page.goto("/app/student/assignments/1");
  await expect(page.getByText("评分量规")).toBeVisible();
  await expect(page.getByText("我的提交内容")).toBeVisible();
  await expect(page.getByText("学术诚信与 AI 使用声明")).toBeVisible();
  const submissionForm = await page.getByRole("heading", { name: "提交修订版本" }).boundingBox();
  const submissionHistory = await page.getByRole("heading", { name: /次提交全文/ }).boundingBox();
  expect(submissionForm?.x ?? -1).toBeLessThan(submissionHistory?.x ?? Number.POSITIVE_INFINITY);
  await page.screenshot({ path: "test-results/student-assignment.png", fullPage: true });
});

test("student workspace remains usable on a phone viewport", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page, "lin", "Student1234");
  await expect(page.getByRole("heading", { name: "我的作业" })).toBeVisible();
  await page.goto("/app/student/assignments/1");
  await expect(page.getByRole("heading", { name: "评分标准" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "提交修订版本" })).toBeVisible();
  await page.screenshot({ path: "test-results/student-mobile.png", fullPage: true });
});

test("student can submit through the React async form", async ({ page }) => {
  await login(page, "lin", "Student1234");
  await page.goto("/app/student/assignments/1");
  await page
    .getByPlaceholder("粘贴或输入完整作业内容……")
    .fill("这是一份 React 端到端提交验证内容，用于确认异步上传和 CSRF 请求正常。");
  await page.getByPlaceholder("列出主要文献、数据来源和获取方式").fill("课程材料与个人整理");
  await page.locator('input[type="file"]').setInputFiles({
    name: "react-upload.md",
    mimeType: "text/markdown",
    buffer: Buffer.from("# React 文件上传验证\n\n这是一份用于端到端测试的附件。"),
  });
  await page.getByRole("checkbox").check();
  await page.getByRole("button", { name: "提交修订版本" }).click();
  await expect(page.getByText("作业已提交，等待教师审核。")).toBeVisible();
  await expect(page.getByText("react-upload.md")).toBeVisible();
});

test("authenticated user can change their password from account settings", async ({ page }) => {
  await login(page, "chen", "Teach1234");
  await page.goto("/app/account");
  await expect(page.getByRole("heading", { name: "AI 接口设置" })).toBeVisible();
  await page.getByLabel("当前密码").fill("Teach1234");
  await page.getByLabel("新密码", { exact: true }).fill("ChangedPass-2026!");
  await page.getByLabel("确认新密码").fill("ChangedPass-2026!");
  await page.getByRole("button", { name: "更新密码" }).click();
  await expect(page.getByText("密码已更新，下次登录时请使用新密码。")).toBeVisible();
});

test("student account settings never expose AI provider configuration", async ({ page }) => {
  await login(page, "lin", "Student1234");
  await page.goto("/app/account");
  await expect(page.getByRole("heading", { name: "个人中心" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "AI 接口设置" })).toHaveCount(0);
});
