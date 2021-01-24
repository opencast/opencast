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


Selenium Tests
--------------

The module comes with a suite of basic Selenium based integration tests.
The tests require Firefox and geckodriver to be installed.
They automatically launch the React app and mock proxy and run against the mocked data.
The tests should usually suffice in determining if e.g. a library update works.

You can run them like this:

```sh
python -m venv venv
. ./venv/bin/activate
pip install -r requirements.txt

./selenium-tests
```

You can also run them with Firefoy in graphical mode:

```sh
./selenium-tests gui
```
