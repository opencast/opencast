import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
    testDir: "./tests",
    fullyParallel: true,
    reporter: "list",
    globalSetup: "./tests/global-setup",

    use: {
        baseURL: "http://127.0.0.1:7878",
        trace: "on-first-retry",
    },

    projects: [
        { name: "firefox", use: { ...devices["Desktop Firefox"] } },
    ],

    // The app itself is served by Vite; tests navigate through the mock/proxy
    // server started in global-setup.ts, which forwards non-mocked requests here.
    webServer: {
        command: "npm run start",
        url: "http://localhost:3000",
        reuseExistingServer: !process.env.CI,
        env: { BROWSER: "none" },
    },
});
