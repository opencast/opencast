LTI Tools
=========

This module contains several LTI tools.


You can start a local version of the tools, available at [127.0.0.1:3000](http://127.0.0.1:3000) by running:

```sh
npm ci
npm run start
```

To provide the necessary REST endpoints for testing while proxying requests to the tool itself, a mock server listening
on [127.0.0.1:7878](http://127.0.0.1:7878) can be started. For this runn in addition to the command above:

```sh
npm run mock-proxy
```
