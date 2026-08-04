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

import { setUpServiceWorker } from '@opencast/jwtify';

// Get initial parameters
const params = new URLSearchParams(self.location.search);
const jwt = params.get('jwt');
const videoId = params.get('videoId');
const refreshEnabled = params.get('refresh') === 'true';

// Only of those are set do we actually register anything.
if (jwt && videoId) {
  let lastClientId = null;
  let resolveRefresh = () => {};
  if (refreshEnabled) {
    // Remember the last client that sent a request. We will always ask it for
    // fresh JWTs.
    self.addEventListener('fetch', event => {
      lastClientId = event.clientId;
    });

    // Receive messages from clients, which will send JWTs back.
    self.addEventListener('message', e => {
      const d = e.data;
      if (typeof d === 'object' && d?.type === 'oc-event-jwt' && typeof d?.jwt === 'string') {
        resolveRefresh(d.jwt);
      }
    });
  }

  const fetchJwts =  async (eventIds) => {
    if (eventIds.size !== 1 || !eventIds.has(videoId)) {
      // eslint-disable-next-line no-console
      console.warn('event IDs requested by jwtify do not match video ID');
      return new Map();
    }

    if (!refreshEnabled) {
      // eslint-disable-next-line no-console
      console.warn('JWT refresh is not enabled, but jwtify asked for a new JWT, meaning the old one is expired');
      return new Map();
    }

    const client = await self.clients.get(lastClientId) ?? self.clients.matchAll()[0];
    if (!client) {
      // eslint-disable-next-line no-console
      console.warn('No client connected to SW, but a JWT is needed for a request... weird');
      return new Map();
    }

    // Get fresh JWT by asking the host frame (by first asking the client).
    const jwt = await new Promise(resolve => {
      resolveRefresh = resolve;
      client.postMessage({
        type: 'oc-event-jwt-request',
        event: videoId,
      });

      // After some timeout we give up, as there might not be anyone answering.
      setTimeout(() => resolve(null), 3000);
    });

    return new Map(jwt ? [[videoId, jwt]] : []);
  };

  const handle = setUpServiceWorker({
    getJwts: fetchJwts,
    trustedOcOrigins: ['https://stable.opencast.org', 'http://localhost:8080', 'http://localhost:8081'], // TODO
    debugLog: true, // TODO: remove
  });

  // Add our initial JWT to the cache so that it can be used for immediate requests.
  handle.cache.add(videoId, jwt);
}
