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
    // Requests to static files are only made after loading the manifest.
    await swReady;
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
let refreshEnabled = params.get('jwtRefresh') === 'true';
if (refreshEnabled && window.self === window.top) {
  // eslint-disable-next-line no-console
  console.warn('jwtRefresh=true given, but not running in iframe');
  refreshEnabled = false;
}
const swUrl = `sw.js?${new URLSearchParams({
  jwt: params.get('jwt'),
  videoId: params.get('id'),
  refresh: refreshEnabled,
})}`;

// Resolves once the SW controls this page (or we gave up on that). Requests
// made before that bypass the SW and thus fail for protected static files.
const swReady = (async () => {
  if (!('serviceWorker' in navigator)) {
    // eslint-disable-next-line no-console
    console.warn('Service workers not supported: cannot authenticate static file requests via JWT');
    return;
  }

  // Listen before registering to not miss the event.
  const controllerChange = new Promise(resolve => {
    navigator.serviceWorker.addEventListener('controllerchange', resolve, { once: true });
  });

  try {
    await navigator.serviceWorker.register(swUrl, { updateViaCache: 'none' });
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error('Failed to register service worker', e);
    return;
  }

  // On the first visit, the SW claims this page right after activating. But
  // e.g. after a hard reload, the page is never controlled, so we don't wait
  // forever.
  if (!navigator.serviceWorker.controller) {
    const timeout = new Promise(resolve => setTimeout(() => resolve('timeout'), 3000));
    if (await Promise.race([controllerChange, timeout]) === 'timeout') {
      // eslint-disable-next-line no-console
      console.warn('Page not controlled by service worker: static file requests won\'t include JWT');
    }
  }
})();



// If JWT refresh is enabled, we need to forward messages between the host of
// this iframe and the SW (as they cannot communicate directly).
if (refreshEnabled && 'serviceWorker' in navigator) {
  // Forward messages coming from the iframe host to the SW.
  window.addEventListener('message', ev => {
    if (ev.source !== window.parent) {
      return;
    }

    const d = ev.data;
    if (typeof d === 'object' && d?.type === 'oc-event-jwt' && typeof d?.jwt === 'string') {
      const { type, jwt } = d;
      navigator.serviceWorker.controller?.postMessage({ type, jwt });
    }
  });

  // Forward messages coming from the SW to the iframe host.
  navigator.serviceWorker.addEventListener('message', ev => {
    const d = ev.data;
    if (typeof d === 'object' && d?.type === 'oc-event-jwt-request' && typeof d?.event === 'string') {
      const { type, event } = d;
      window.parent.postMessage({ type, event }, '*');
    }
  });
}
