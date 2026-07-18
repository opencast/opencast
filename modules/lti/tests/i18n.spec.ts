import { test, expect } from "@playwright/test";

test("German locale translates the series results heading", async ({ page }) => {
    await page.goto("/ltitools/index.html?subtool=series&lng=de");

    // The old selenium test only asserted the heading did NOT start with the
    // English "Results" string, which would also pass for a broken
    // translation. Assert the actual expected German string instead, derived
    // from lang-de_DE.json's RESULT_HEADING template interpolated against
    // mock-server-api-data/search/episode.json (total=106, limit=5, offset=0).
    await expect(page.locator("header")).toHaveText(/^Ergebnisse 1-5 von 106/);
});
