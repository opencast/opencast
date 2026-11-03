/// <reference lib="webworker" />

// Service worker that attaches JWTs to requests for protected static files
// (videos, images, ...). Only a service worker can add headers to requests made
// by e.g. `<video>`. Built separately via `vite.config.sw.ts`. The page side
// lives in `jwt.ts`.

import { setUpServiceWorker, type EventId, type GetJwtsInfo, type Jwt } from '@opencast/jwtify';

declare let self: ServiceWorkerGlobalScope;

// Origins we attach JWTs to requests for, passed by the page (from its config).
const trustedOcOrigins = new URLSearchParams(self.location.search).get('trustedOrigins')?.split(',') ?? [];

// Requests to pages waiting for an answer: request ID -> resolve function.
const pendingRequests = new Map<string, (jwt: Jwt | null) => void>();

const fetchJwts = async (eventIds: Set<EventId>, { clientId }: GetJwtsInfo): Promise<Map<EventId, Jwt>> => {
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

    // The page answers with its own JWT or, if JWT refresh is enabled, gets a
    // fresh one from the iframe host.
    const jwt = await new Promise<Jwt | null>(resolve => {
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
    trustedOcOrigins,
    // debugLog: true,
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
