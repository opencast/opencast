/**
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
import { expect, test } from '@playwright/test';
import { clickToStartVideo, pauseVideo, playerInstanceStr } from './utils';


test.describe('Player URL query parameters', () => {

  test('Without query aditional query parameters', async ({ page }) => {
    await page.goto('/paella7/ui/watch.html?id=ID-dual-stream-demo');
    await clickToStartVideo(page);
    await pauseVideo(page);
    await page.waitForTimeout(1000);

    const currentTime = await page.evaluate(`${playerInstanceStr}.videoContainer.currentTime()`);
    const captionsVisible = await page.evaluate(`${playerInstanceStr}.captionsCanvas.isVisible`);
    await expect(currentTime).toBeCloseTo(0, 0);
    await expect(captionsVisible).toBeFalsy();
  });

  test('Check time param in URL and seek: ?time=1m2s', async ({ page }) => {
    await page.goto('/paella7/ui/watch.html?id=ID-dual-stream-demo&time=20s');
    await clickToStartVideo(page);
    await pauseVideo(page);
    await page.waitForTimeout(5000);

    const currentTime = await page.evaluate(`${playerInstanceStr}.videoContainer.currentTime()`);
    await expect(currentTime).toBeCloseTo(20, 0);
  });

  // test('Check captions param in URL: ?captions=<lang>', async ({ page }) => {
  //   await page.goto('/paella7/ui/watch.html?id=ID-dual-stream-demo&captions=en');
  //   await clickToStartVideo(page);
  //   await pauseVideo(page);
  //   await page.waitForTimeout(5000);
  //   const captionsVisible = await page.evaluate(`${playerInstanceStr}.captionsCanvas.isVisible`);
  //   await expect(captionsVisible).toBeTruthy();
  // });

});
