Error Resolution Workflow Operation
===================================

```Id: error-resolution```

Description
-----------

The Error Resolution operation is an **internal** operation inserted in the workflow by the Workflow Service
when an operation that has retry-strategy="hold" fails. This operations pauses the workflow so that
the user can retry or abort processing using the Admin UI.

See [Retry Strategies](retry-strategies.md) for more details.
