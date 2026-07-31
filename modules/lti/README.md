LTI Tools
=========

This module contains several LTI tools.


You can start a local version of the tools, available at [127.0.0.1:3000](http://127.0.0.1:3000) by running:

```sh
npm ci
npm run start
```


End-to-End Tests
----------------

The module comes with a suite of Playwright based integration tests under `tests/`.
They start the React app themselves, serve the LTI REST endpoints from the JSON
fixtures in `mock-server-api-data/` via a small mock/proxy server (`tests/mock-server.ts`),
and run against that mocked data. The tests should usually suffice in determining
if e.g. a library update works.

Install the Playwright browser binaries once:

```sh
npx playwright install firefox
```

Then run the tests:

```sh
npm run test:e2e
```

You can also run them interactively via Playwright's UI mode:

```sh
npm run test:e2e:ui
```
