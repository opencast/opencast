# Publication to Workspace Workflow Operation

ID: `publication-channel-to-workspace`

## Description

The Publication to Workspace operation can be used to copy content from publication channels to the workspace.
With this workflow one can copy or manipulate published elements without re-encoding.

## Parameter Table

|Configuration Key  |Example           |Description                                           |
|-------------------|------------------|------------------------------------------------------|
|publication-channel|engage-player     |The publication-channel |
|source-flavors     |presenter/delivery|Comma-separated list of flavors identifying elements to copy|
|source-tags        |engage-download   |Comma-separated list of tags identifying elements to copy|
|target-tags        |archive           |Comma-separated list of tags to add to copied elements|


## Operation Example

```xml
<operation id="publication-channel-to-workspace"
           description="Copy publication channel to workspace">
  <configurations>
    <configuration key="source-channel">engage-player</configuration>
    <configuration key="source-flavors">presenter/delivery,presentation/delivery</configuration>
    <configuration key="source-tags">engage-download,engage-streaming</configuration>
    <configuration key="target-tags">archive</configuration>
  </configurations>
</operation>
```
