# Retry Strategies

An operation can have a retry-strategy specified to define what will happen if the operation **fails**:

|Strategy | Description|
|---------|------------|
|**none**| This is the default. No action taken. If the operation fails, the behavior will depend on the fail-on-error parameter. If fail-on-error="true", the workflow will fail. If fail-on-error="false", the next operation will be executed.|
|**retry**| If the operation fails, it will be re-tried until the number of attempts reaches max-attempts, which defaults to 2.|
|**hold**|If the operation fails, the workflow will be paused, until the user takes an action. The user can choose to *Retry* the operation or *Abort* it. The user can retry the operation many times, until the number of attempts reaches max-attempts. If the user aborts the operation, the behavior will depend on the fail-on-error parameter as described above.|

**Example 1**: No retry

If the operation1 fails, the workflow will fail because fail-on-error="true".

```
<operation
  id="operation1"
  retry-strategy="none"
  fail-on-error="true"
  exception-handler-workflow="error"
  description="Operation One">
</operation>
```

**Example 2**: Automatic retry

If operation2 fails, it will be retried until it succeeds or until it has failed 5 times.

```
<operation
  id="operation2"
  retry-strategy="retry"
  max-attempts="5"
  fail-on-error="true"
  exception-handler-workflow="error"
  description="Operation Two">
</operation>
```

**Example 3**: Manual retry

If operation3 fails, the user can choose between Retry or Abort. The user can manually retry the operation 4 times.

```
<operation
  id="operation3"
  retry-strategy="hold"
  max-attempts="5"
  fail-on-error="true"
  exception-handler-workflow="error"
  description="Operation Three">
</operation>
```



