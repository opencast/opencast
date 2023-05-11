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
import { test, expect } from '@playwright/test';
import { playVideo } from './utils';


test('Has the correct title', async ({ page }) => {
  await page.goto('http://127.0.0.1:7070/paella7/ui/watch.html?id=ID-dual-stream-demo');
  await expect(page).toHaveTitle('Dual-Stream Demo - No series | Opencast');
});

test('Video is loaded', async ({ page }) => {
  await page.goto('/paella7/ui/watch.html?id=ID-dual-stream-demo');    
  await playVideo(page);
});
