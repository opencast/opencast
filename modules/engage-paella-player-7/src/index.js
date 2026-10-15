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
import { setUpClient } from '@opencast/jwtify';
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


// Setup service worker to enable static file auth via JWT.
const params = new URLSearchParams(window.location.search);
const jwt = params.get('jwt');
const videoId = params.get('id');
let refreshEnabled = params.get('jwtRefresh') === 'true';
if (refreshEnabled && window.self === window.top) {
  // eslint-disable-next-line no-console
  console.warn('jwtRefresh=true given, but not running in iframe');
  refreshEnabled = false;
}
const swUrl = 'sw.js';

if ('serviceWorker' in navigator) {
  // Send our JWT to the SW, so that it can be used for the first requests
  // already. A new SW or one that got reset (after a hard reload) has an empty
  // cache, so we also do that whenever the controller changes. This listener
  // is added before `setUpClient` so that it runs before `swReady` resolves.
  const sendJwt = () => {
    if (jwt && videoId) {
      navigator.serviceWorker.controller?.postMessage({ type: 'oc-event-jwt', event: videoId, jwt });
    }
  };
  navigator.serviceWorker.addEventListener('controllerchange', sendJwt);
  sendJwt();
}

// Resolves once the SW controls this page (or we gave up on that). Requests
// made before that bypass the SW and thus fail for protected static files.
// Never rejects, and logs a warning itself if something went wrong.
const swReady = setUpClient(swUrl);



// Answer JWT requests from the SW. If JWT refresh is enabled, we forward them
// to the host of this iframe (as it and the SW cannot communicate directly).
if ('serviceWorker' in navigator) {
  // SW requests waiting for an answer from the iframe host: request ID -> SW.
  // We reply to the SW that asked, which can differ from `controller` during
  // an SW update.
  const pendingRequests = new Map();

  navigator.serviceWorker.addEventListener('message', ev => {
    const d = ev.data;
    if (typeof d === 'object' && d?.type === 'oc-event-jwt-request'
      && typeof d?.event === 'string' && typeof d?.reqId === 'string') {
      const answer = token => ev.source.postMessage({
        type: 'oc-event-jwt',
        event: d.event,
        jwt: token,
        reqId: d.reqId,
      });

      if (d.event !== videoId) {
        // We only have JWTs for our own event.
        answer(null);
      } else if (refreshEnabled) {
        pendingRequests.set(d.reqId, ev.source);
        window.parent.postMessage({ type: d.type, event: d.event }, '*');
      } else {
        // Our initial JWT (if any). The SW asks again e.g. after it was stopped
        // and lost its cache. If the JWT is expired, requests fail either way.
        answer(jwt);
      }
    }
  });

  // Forward JWTs coming from the iframe host to the SW.
  if (refreshEnabled) {
    window.addEventListener('message', ev => {
      if (ev.source !== window.parent) {
        return;
      }

      const d = ev.data;
      if (typeof d === 'object' && d?.type === 'oc-event-jwt'
        && d?.event === videoId && typeof d?.jwt === 'string') {
        const msg = { type: d.type, event: d.event, jwt: d.jwt };
        if (pendingRequests.size === 0) {
          // The host sent it without us asking: just put it into the SW's cache.
          navigator.serviceWorker.controller?.postMessage(msg);
        }
        for (const [reqId, sw] of pendingRequests) {
          sw.postMessage({ ...msg, reqId });
        }
        pendingRequests.clear();
      }
    });
  }
}
