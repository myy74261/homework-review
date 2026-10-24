import { chromium } from "@playwright/test";

const baseUrl = process.env.SMOKE_BASE_URL;
const username = process.env.SMOKE_USERNAME;
const password = process.env.SMOKE_PASSWORD;

if (!baseUrl || !username || !password) {
  throw new Error("SMOKE_BASE_URL、SMOKE_USERNAME 和 SMOKE_PASSWORD 必须提供");
}

const browser = await chromium.launch({ channel: "msedge", headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  await page.goto(`${baseUrl}/login`);
  await page.getByLabel("账号").fill(username);
  await page.getByLabel("密码").fill(password);
  await page.getByRole("button", { name: "登录并进入课堂" }).click();
  await page.waitForURL(/\/app\/(teacher|student)$/);
  const url = page.url();
  if (url.endsWith("/app/teacher")) {
    await page.getByRole("heading", { name: "从提交证据到备课素材" }).waitFor();
  } else {
    await page.getByRole("heading", { name: "我的作业" }).waitFor();
  }
  console.log(`Production React smoke test passed: ${new URL(url).pathname}`);
} finally {
  await browser.close();
}
