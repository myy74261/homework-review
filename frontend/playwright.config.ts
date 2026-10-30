import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./tests",
  timeout: 30_000,
  fullyParallel: false,
  reporter: "line",
  use: {
    baseURL: "http://127.0.0.1:8090",
    channel: "msedge",
    trace: "retain-on-failure",
  },
  webServer: {
    command: '"D:\\Project\\jdk\\bin\\java.exe" -jar "target\\review-desk-0.1.0-SNAPSHOT.jar"',
    cwd: "..",
    url: "http://127.0.0.1:8090/login",
    reuseExistingServer: true,
    timeout: 120_000,
    env: {
      SERVER_PORT: "8090",
      SERVER_ADDRESS: "127.0.0.1",
      DATABASE_URL: "jdbc:h2:mem:review-desk-e2e",
      JPA_DDL_AUTO: "create-drop",
      SHOW_DEMO_CREDENTIALS: "true",
      THYMELEAF_CACHE: "false",
      UPLOAD_ROOT: "target/e2e-uploads",
      SESSION_STORE_TYPE: "none",
      FLYWAY_ENABLED: "false",
    },
  },
});
