/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
import { expect } from '@playwright/test';
import { test, clickToStartVideo, pauseVideo, playerInstanceStr } from './utils';


test.describe('Player URL query parameters', () => {

  test('Without additional query parameters', async ({ page }) => {
    await page.goto('/paella7/ui/watch.html?id=ID-strong-river-flowing-down-the-green-forest');
    await clickToStartVideo(page);
    await pauseVideo(page);
    await page.waitForTimeout(1000);

    const currentTime = await page.evaluate(`${playerInstanceStr}.videoContainer.currentTime()`);
    const captionsVisible = await page.evaluate(`${playerInstanceStr}.captionsCanvas.isVisible`);
    await expect(currentTime).toBeCloseTo(0, 0);
    await expect(captionsVisible).toBeFalsy();
  });

  test('Check time param in URL and seek: ?time=10s', async ({ page }) => {
    await page.goto('/paella7/ui/watch.html?id=ID-strong-river-flowing-down-the-green-forest&time=10s');
    await clickToStartVideo(page);
    await pauseVideo(page);
    await page.waitForTimeout(5000);
    const currentTime = await page.evaluate(`${playerInstanceStr}.videoContainer.currentTime()`);
    await expect(currentTime).toBeCloseTo(10, 0);
  });

  test('Check trimming param in URL: ?trimming=5s;20s', async ({ page }) => {
    await page.goto('/paella7/ui/watch.html?id=ID-strong-river-flowing-down-the-green-forest&trimming=5s;20s');
    await clickToStartVideo(page);
    await pauseVideo(page);
    await page.waitForTimeout(5000);
    const trimState = await page.evaluate(`${playerInstanceStr}.videoContainer.isTrimEnabled`);
    const trimStart = await page.evaluate(`${playerInstanceStr}.videoContainer.trimStart`);
    const trimEnd = await page.evaluate(`${playerInstanceStr}.videoContainer.trimEnd`);
    const duration = await page.evaluate(`${playerInstanceStr}.videoContainer.duration()`);
    await expect(trimState).toBeTruthy;
    await expect(trimStart).toBe(5);
    await expect(trimEnd).toBe(20);
    await expect(duration).toBe(15);
  });

  test('Check time param in URL and seek: &start=5&end=20', async ({ page }) => {
    await page.goto('/paella7/ui/watch.html?id=ID-strong-river-flowing-down-the-green-forest&start=5&end=20');
    await clickToStartVideo(page);
    await pauseVideo(page);
    await page.waitForTimeout(5000);
    // Test video trim attributes
    const trimState = await page.evaluate(`${playerInstanceStr}.videoContainer.isTrimEnabled`);
    const trimStart = await page.evaluate(`${playerInstanceStr}.videoContainer.trimStart`);
    const trimEnd = await page.evaluate(`${playerInstanceStr}.videoContainer.trimEnd`);
    const duration = await page.evaluate(`${playerInstanceStr}.videoContainer.duration()`);
    await expect(trimState).toBeTruthy;
    await expect(trimStart).toBe(5);
    await expect(trimEnd).toBe(20);
    await expect(duration).toBe(15);
  });

  // test('Check captions param in URL: ?captions=<lang>', async ({ page }) => {
  //   await page.goto('/paella7/ui/watch.html?id=ID-strong-river-flowing-down-the-green-forest&captions=en');
  //   await clickToStartVideo(page);
  //   await pauseVideo(page);
  //   await page.waitForTimeout(5000);
  //   const captionsVisible = await page.evaluate(`${playerInstanceStr}.captionsCanvas.isVisible`);
  //   await expect(captionsVisible).toBeTruthy();
  // });
});
