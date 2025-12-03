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
import { applyOpencastTheme } from './default-theme.js';
import { PaellaOpencast, getUrlFromOpencastConfig } from './js/PaellaOpencast.js';

window.onload = async () => {
  let paella = new PaellaOpencast('player-container');

  try {
    await applyOpencastTheme(paella);
    await paella.loadManifest();

    //check audio-only config
    const config = await fetch(getUrlFromOpencastConfig('config.json'));
    const configJson = await config.json();
    const htmlPlayer = configJson?.opencast?.audioOnlyHTMLPlayer;

    // The player manifest access functions will only be available after the manifest has been loaded.
    if (paella.streams.isNativelyPlayable &&
      paella.streams.isAudioOnly &&
      htmlPlayer &&
      (!paella.captions || paella.captions.length === 0))
    {
      const nativePlayer = paella.streams.nativePlayer;
      nativePlayer.setAttribute('controls','');
      await paella.unload();
      const playerContainer = document.getElementById('player-container');
      playerContainer.innerHTML = '';
      playerContainer.appendChild(nativePlayer);
    }
    paella.log.info('Paella player load done');
  }
  catch(error){
    paella.log.error(error);
  }
};


// Setup service worker to enable static file auth via JWT. We pass JWT and
// video ID via query parameters in order to have them available in the SW
// immediately. I know that typically, you want an unchanging URL for your SWs,
// but that only applies to service workers that want to have a persistent state
// as far as I can tell. This query parameter passing method is known and used.
// Also, the SW is super tiny, so the additional fetches don't really matter.
const params = new URLSearchParams(window.location.search);
const swUrl = `sw.js?${new URLSearchParams({
  jwt: params.get('jwt'),
  videoId: params.get('id'),
})}`;

navigator.serviceWorker
  .register(swUrl, { updateViaCache: 'none' })
  // eslint-disable-next-line no-console
  .catch(e => console.error('Failed to register service worker', e));
