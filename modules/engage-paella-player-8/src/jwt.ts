// Support for embedding the player with a JWT (`?jwt=...`), e.g. by an LMS,
// without an Opencast session. API requests get the JWT as URL parameter (see
// `withJwt`). Requests for protected static files (videos, images, ...) need an
// `Authorization` header, which only a service worker can add: see `sw.ts`.
// This file contains the page side of that.

import { setUpClient, type SetUpClientResult } from '@opencast/jwtify';
import { getUrlParameter, type OpencastPaellaConfig } from '@asicupv/paella-opencast-core';

// JWT given to this page by an embedding application (e.g. an LMS), if any.
export const jwt = getUrlParameter('jwt');
const videoId = getUrlParameter('id');

// Adds the JWT given to this page (if any) to the URL of an Opencast API
// request, so that Opencast authorizes the request with it.
export function withJwt(url: string): string {
    if (!jwt) {
        return url;
    }
    return `${url}${url.includes('?') ? '&' : '?'}jwt=${encodeURIComponent(jwt)}`;
}

// Our settings in `config.json` (section `opencast.jwt`), which the core's
// config type doesn't know about.
type JwtConfig = {
    authStaticFiles?: boolean;
    trustedOrigins?: string[];
};

let refreshEnabled = getUrlParameter('jwtRefresh') === 'true';
if (refreshEnabled && window.self === window.top) {
    // eslint-disable-next-line no-console
    console.warn('jwtRefresh=true given, but not running in iframe');
    refreshEnabled = false;
}

if ('serviceWorker' in navigator) {
    // Send our JWT to the SW, so that it can be used for the first requests
    // already. A new SW or one that got reset (after a hard reload) has an empty
    // cache, so we also do that whenever the controller changes. This listener
    // is added before `setUpClient` so that it runs before its promise resolves.
    const sendJwt = () => {
        if (jwt && videoId) {
            navigator.serviceWorker.controller?.postMessage({ type: 'oc-event-jwt', event: videoId, jwt });
        }
    };
    navigator.serviceWorker.addEventListener('controllerchange', sendJwt);
    sendJwt();
}

// Sets up the service worker for static file auth, or unregisters it if it's
// disabled in the config, so that it doesn't linger. Resolves once the SW
// controls this page (or we gave up on that, or static file auth is disabled).
// Requests made before that bypass the SW and thus fail for protected static
// files. Never rejects, and logs a warning itself if something went wrong.
export function setUpStaticFileAuth(config: OpencastPaellaConfig | null): Promise<SetUpClientResult> {
    const jwtConfig = (config?.opencast as { jwt?: JwtConfig } | undefined)?.jwt;
    const enabled = jwtConfig?.authStaticFiles === true;

    // The SW only attaches JWTs to requests to these origins. They are passed
    // via the SW URL, as the SW needs them right when it starts. Changing them
    // thus reinstalls the SW.
    const trustedOrigins = new Set([window.location.origin]);
    for (const origin of jwtConfig?.trustedOrigins ?? []) {
        try {
            // Normalize, e.g. remove trailing slash, as the SW compares exactly.
            trustedOrigins.add(new URL(origin).origin);
        } catch {
            // eslint-disable-next-line no-console
            console.warn(`Ignoring invalid entry in 'opencast.jwt.trustedOrigins': ${origin}`);
        }
    }

    const swUrl = `sw.js?${new URLSearchParams({ trustedOrigins: [...trustedOrigins].join(',') })}`;
    return setUpClient(swUrl, { enabled });
}

// Answer JWT requests from the SW. If JWT refresh is enabled, we forward them
// to the host of this iframe (as it and the SW cannot communicate directly).
if ('serviceWorker' in navigator) {
    // SW requests waiting for an answer from the iframe host: request ID -> SW.
    // We reply to the SW that asked, which can differ from `controller` during
    // an SW update.
    const pendingRequests = new Map<string, ServiceWorker>();

    navigator.serviceWorker.addEventListener('message', ev => {
        const d = ev.data;
        if (typeof d === 'object' && d?.type === 'oc-event-jwt-request'
            && typeof d?.event === 'string' && typeof d?.reqId === 'string'
            && ev.source instanceof ServiceWorker) {
            const sw = ev.source;
            const answer = (token: string | null) => sw.postMessage({
                type: 'oc-event-jwt',
                event: d.event,
                jwt: token,
                reqId: d.reqId,
            });

            if (d.event !== videoId) {
                // We only have JWTs for our own event.
                answer(null);
            } else if (refreshEnabled) {
                pendingRequests.set(d.reqId, sw);
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
