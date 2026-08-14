import { test, expect } from "@playwright/test";

// mock-server-api-data/search/episode.json reports total=106, limit=5, offset=0.
const EXPECTED_HEADING = /^Results 1-5 of 106/;

for (const [edit, deletion] of [
    [false, false],
    [true, true],
] as const) {
    test(`series tool lists results with the right action buttons (edit=${edit}, deletion=${deletion})`, async ({ page }) => {
        const expectedButtonCount = Number(edit) + Number(deletion);

        await page.goto(`/ltitools/index.html?subtool=series&edit=${edit}&deletion=${deletion}`);

        const header = page.locator("header");
        await expect(header).toHaveText(EXPECTED_HEADING);

        const firstResult = page.locator(".list-group-item").first();
        await expect(firstResult.locator("button")).toHaveCount(expectedButtonCount);

        await firstResult.click();
        await expect(page).toHaveURL(/\/play\/[^/]+$/);
    });
}
