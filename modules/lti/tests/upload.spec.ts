import { test, expect } from "@playwright/test";

test("upload tool requires a file before allowing submission", async ({ page }) => {
    await page.goto("/ltitools/index.html?subtool=upload&series=");

    await expect(page.locator("form")).toBeVisible();
    await expect(page.locator("h2").first()).toHaveText("Upload new event");

    // Clicking submit with no fields filled out must not trigger an upload.
    await page.locator(".btn-primary").click();
    await expect(page.locator(".alert-success")).not.toBeVisible({ timeout: 1000 });
});

test("edit flow loads metadata for an existing event", async ({ page }) => {
    // Exercises mock-server-api-data/lti-service-gui/existing-event/metadata,
    // a fixture that previously existed but was never used by any test.
    await page.goto("/ltitools/index.html?subtool=upload&episode_id=existing-event&series=some-series");

    await expect(page.locator("h2").first()).toHaveText("Edit event");
    await expect(page.locator("#title")).toHaveValue("1");
});
