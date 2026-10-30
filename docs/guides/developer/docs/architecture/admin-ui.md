Admin UI
========

The admin UI is Opencast's primary web interface for day-to-day administration: managing events, series, users,
access control, and more. Unlike most of Opencast, it isn't a Java module at all — `modules/admin` is a git
submodule containing a separate, modern single-page application (React, Redux, built with Vite), developed
and versioned independently of the rest of the codebase.

This page covers how that frontend fits into an otherwise Java-and-OSGi system: how it gets built and served, and
how it talks to the backend.


Building and Serving the Frontend
------------------------------------

Building `modules/admin` still goes through the regular Maven build. The `frontend-maven-plugin` installs a pinned
Node and npm version (`node.version` in the root `pom.xml`), then runs `npm ci` and `npm run build` during Maven's
`generate-resources` phase, producing a static build directory.

From there, nothing Opencast-specific happens in Java at all: the built assets are embedded directly into the
`opencast-admin` OSGi bundle (`Include-Resource = /admin-ui=-build/`) and served through the OSGi HTTP Service —
implemented in Opencast's case by OPS4J Pax Web, running on Jetty. `Http-Alias = /admin-ui` tells the HTTP service
to serve this bundle's resources at `/admin-ui`, and `Http-Welcome = index.html` makes that path serve the SPA's
entry point for any unmatched route, which is what lets client-side routing work. There is no custom Java code
serving the frontend; it is entirely declarative bnd/manifest configuration.

This is not a special case: the same `Http-Alias` pattern serves most of Opencast's other UIs the same way,
including the editor (`/editor-ui`), the Paella player (`/paella8/ui`), the classic engage player (`/engage/ui`),
Opencast Studio (`/studio`), and the REST API documentation (`/rest-docs`).


Talking to the Backend
-------------------------

Because the admin UI is served from the same Jetty instance as everything else, every API call it makes is
same-origin: the frontend's HTTP client has an empty base URL in production (`axios.defaults.baseURL` is only
overridden locally during development, to point at a separately-running backend), so requests simply go to
whatever host and port `/admin-ui` itself was loaded from. This means the same session-cookie-based authentication
Opencast uses everywhere else works for the admin UI without any extra token handling.

Almost everything the frontend calls lives in `modules/admin-service`, consistently namespaced under `/admin-ng/`
— events, series, jobs, access control, users, groups, themes, statistics, and more, for example `/admin-ng/series`
or `/admin-ng/acl`. Calling another service's own endpoint directly is very much the exception rather than the
rule: it happens only for a handful of things that are not admin-UI-specific functionality at all, such as
`/info/me.json` for the current user's identity, `/services/health.json` for service health, and
`/sysinfo/bundles/version` for the running version — general-purpose endpoints that exist independently of the
admin UI and would make little sense duplicated under `/admin-ng/`.
