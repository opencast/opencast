import { test, expect } from "@playwright/test";

test("overview page shows the welcome heading", async ({ page }) => {
    await page.goto("/ltitools");
    await expect(page.locator("h1")).toHaveText("Welcome to the LTI Module");
});
