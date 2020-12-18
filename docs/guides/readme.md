Opencast Documentation Sources
==============================

The documentation uses `mkdocs` and the `markdown_inline_graphviz` extension as static site generator. The documentation
on [docs.opencast.org](https://docs.opencast.org) is updated once a day from these sources. For details about the
automated build, check out:

- <https://github.com/opencast/docs.opencast.org>


Quick-build
-----------

A quick example on how to build/serve the docs locally.
Requires python virtual environment.

```sh
% cd guides
% virtualenv venv
% . ./venv/bin/activate
% pip install mkdocs mkdocs-windmill
% pip install markdown_inline_graphviz_extension
% pip install markdown-inline-graphviz-extension-png
% cd developer
% python -m mkdocs serve
```


Tests
-----

The guides come with a few automated style checks. They are executed on pull requests automatically but you can also run
them locally:

```sh
% cd guides
% npm install
% npm test
```
