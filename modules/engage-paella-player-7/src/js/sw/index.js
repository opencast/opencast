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
const refreshEnabled = params.get('refresh') === 'true';

let lastClientId = null;
// The refresh request currently waiting for an answer, if any.
let pendingRefresh = null;
if (refreshEnabled) {
  // Remember the last client that sent a request. We will always ask it for
  // fresh JWTs.
  self.addEventListener('fetch', event => {
    lastClientId = event.clientId;
  });
}

const fetchJwts =  async (eventIds) => {
  if (!refreshEnabled) {
    // eslint-disable-next-line no-console
    console.warn('jwtify asked for a JWT (none cached or expired), but JWT refresh is not enabled');
    return new Map();
  }

  if (eventIds.size !== 1) {
    // eslint-disable-next-line no-console
    console.warn('jwtify asked for JWTs of multiple events at once, which is not supported');
    return new Map();
  }
  const [eventId] = eventIds;

  const client = await self.clients.get(lastClientId) ?? self.clients.matchAll()[0];
  if (!client) {
    // eslint-disable-next-line no-console
    console.warn('No client connected to SW, but a JWT is needed for a request... weird');
    return new Map();
  }

  // Get fresh JWT by asking the host frame (by first asking the client).
  const jwt = await new Promise(resolve => {
    pendingRefresh = { eventId, resolve };
    client.postMessage({
      type: 'oc-event-jwt-request',
      event: eventId,
    });

    // After some timeout we give up, as there might not be anyone answering.
    setTimeout(() => resolve(null), 3000);
  });

  return new Map(jwt ? [[eventId, jwt]] : []);
};

const handle = setUpServiceWorker({
  getJwts: fetchJwts,
  trustedOcOrigins: ['https://stable.opencast.org', 'http://localhost:8080', 'http://localhost:8081'], // TODO
  debugLog: true, // TODO: remove
});

// Receive JWTs from clients. They are sent unsolicited when the page loads and
// as answer to our `oc-event-jwt-request`.
self.addEventListener('message', e => {
  const d = e.data;
  if (typeof d === 'object' && d?.type === 'oc-event-jwt'
    && typeof d?.event === 'string' && typeof d?.jwt === 'string') {
    handle.cache.add(d.event, d.jwt);
    if (pendingRefresh?.eventId === d.event) {
      pendingRefresh.resolve(d.jwt);
    }
  }
});
