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

import { getUrlFromOpencastConfig, getUrlFromOpencastPaella } from './js/PaellaOpencast';


export async function applyOpencastTheme(paella) {
  //// Load custom theme
  const config = await fetch(getUrlFromOpencastConfig('config.json'));
  const configJson = await config.json();

  const u = new URL(window.location.href);
  const ocTheme = u.searchParams.get('oc.theme')
    ?? configJson?.opencast?.theme
    ?? 'default_theme'  ;
  try {
    paella.log.info(`Applying opencast theme '${ocTheme}'.`);
    await paella.skin.loadSkin(getUrlFromOpencastConfig(`${ocTheme}/theme.json`));
  }
  catch (err) {
    paella.log.info(`Error applying opencast theme '${ocTheme}'. Using default theme!`);
    await paella.skin.loadSkin(getUrlFromOpencastPaella('default_theme/theme.json'));
  }
}
