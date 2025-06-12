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

```yaml
  - id: publication-channel-to-workspace
    description: Copy publication channel to workspace
    configurations:
      - source-channel: engage-player
      - source-flavors: presenter/delivery,presentation/delivery
      - source-tags: engage-download,engage-streaming
      - target-tags: archive
```
