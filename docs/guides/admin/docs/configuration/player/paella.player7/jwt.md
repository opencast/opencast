Paella Player 7 - Embedding with JWT
====================================

External applications, usually an LMS, can embed Paella 7 in an iframe and pass a
[JWT](../../security.jwt.md) that grants access to the event. This way, users can watch 
protected videos without an Opencast session.


Prerequisites
-------------

Opencast has to accept JWTs from the embedding application, see the [JWT configuration](../../security.jwt.md).
The JWT needs to grant read access to the event, e.g.:

```json
{
    "exp": 1499863217,
    "oc": { "e:d622b861-4264-4947-8db1-c754c5956433": ["read"] }
}
```


Embedding the player
--------------------

Pass the JWT via the `jwt` URL parameter:

```html
<iframe src="https://opencast.example.com/paella7/ui/watch.html?id=EVENT-ID&jwt=JWT" allowfullscreen></iframe>
```

The player attaches the JWT to the requests it sends to the Opencast API, e.g. to load the event. 
This mostly happens when loading the player, so the JWT has to be valid for only that time.
For protected static files, see the next section.


Protected static files
----------------------

If [static file authorization](../../serving-static-files.md) is enabled (which is the default), requests for videos,
images and other static files need the JWT as well. Opencast uses a service worker that attaches the JWT 
as `Authorization` header to these requests.

This is disabled by default. You can enable it in `.../etc/ui-config/<tenant>/paella7/config.json`:

```json
{
    ...
    "opencast": {
        "jwt": {
            "authStaticFiles": true,
            "trustedOrigins": []
        }
        ...
    }
    ...
}
```

- `authStaticFiles`: Whether to use the service worker. You only need it if static files are protected. When
  disabled, a previously installed service worker is removed the next time the player is loaded.
- `trustedOrigins`: Origins the service worker sends JWTs to, in addition to the origin the player is loaded from.
  You need this if static files are served from a different host, e.g. if `org.opencastproject.download.url` points
  to a different server, when using S3 distribution, or if tenants use different host names. Example:
  `["https://media.example.com"]`.
    - Only list hosts you trust: these receive the JWTs.
    - These hosts have to allow CORS requests from the origin of the player with the `Authorization` and `Range`
      headers.

Service workers are only available on pages served via HTTPS (or from `localhost`). If they are not available, the
player still loads, but cannot load protected static files.


Refreshing JWTs
---------------

JWTs should be short-lived, but users might watch a video for longer than that. If the iframe URL contains
`jwtRefresh=true`, the player asks the embedding page for a new JWT whenever it needs one for static files.
Without it, the player keeps using the JWT from the URL, and requests for static files fail once it is expired.

The player and the embedding page communicate via
[`postMessage`](https://developer.mozilla.org/en-US/docs/Web/API/Window/postMessage):

- The player sends `{ type: "oc-event-jwt-request", event: "EVENT-ID" }` to its parent window.
- The embedding page answers by sending `{ type: "oc-event-jwt", event: "EVENT-ID", jwt: "NEW-JWT" }` to the iframe.
  It has to answer within 3 seconds, otherwise the request is sent without JWT. 

The player only requests JWTs for the event it was loaded with. The embedding page should only answer messages from
its own iframe. Example:

```js
const iframe = document.getElementById("player");
const opencastOrigin = "https://opencast.example.com";

window.addEventListener("message", async event => {
    if (event.source !== iframe.contentWindow || event.origin !== opencastOrigin) {
        return;
    }
    if (event.data?.type === "oc-event-jwt-request") {
        // Get a fresh JWT from your backend, which checks whether the user may watch this video.
        const jwt = await fetchJwtForEvent(event.data.event);
        iframe.contentWindow.postMessage({ type: "oc-event-jwt", event: event.data.event, jwt }, opencastOrigin);
    }
});
```
