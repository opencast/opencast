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

// Requests to pages waiting for an answer: request ID -> resolve function.
const pendingRequests = new Map();

const fetchJwts =  async (eventIds, { clientId }) => {
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

  // We ask the page that made the request. The client ID is empty for
  // navigation requests (e.g. a static file opened directly in a tab), and only
  // windows can answer.
  const client = clientId ? await self.clients.get(clientId) : undefined;
  if (client?.type !== 'window') {
    return new Map();
  }

  // Get fresh JWT by asking the host frame (by first asking the client).
  const jwt = await new Promise(resolve => {
    const reqId = crypto.randomUUID();
    pendingRequests.set(reqId, resolve);
    client.postMessage({
      type: 'oc-event-jwt-request',
      event: eventId,
      reqId,
    });

    // After some timeout we give up, as there might not be anyone answering.
    setTimeout(() => {
      pendingRequests.delete(reqId);
      resolve(null);
    }, 3000);
  });

  return new Map(jwt ? [[eventId, jwt]] : []);
};

const handle = setUpServiceWorker({
  getJwts: fetchJwts,
  trustedOcOrigins: ['https://stable.opencast.org', 'http://localhost:8080', 'http://localhost:8081'], // TODO
  debugLog: true, // TODO: remove
});

// Receive JWTs from clients. They are sent unsolicited when the page loads and
// as answer to our `oc-event-jwt-request` (with `reqId`, and `jwt: null` if
// the page has none). We cache all of them, so that answers arriving after the
// timeout are still useful.
self.addEventListener('message', e => {
  const d = e.data;
  if (typeof d === 'object' && d?.type === 'oc-event-jwt' && typeof d?.event === 'string') {
    const jwt = typeof d.jwt === 'string' ? d.jwt : null;
    if (jwt) {
      handle.cache.add(d.event, jwt);
    }

    const resolve = pendingRequests.get(d.reqId);
    if (resolve) {
      pendingRequests.delete(d.reqId);
      resolve(jwt);
    }
  }
});
