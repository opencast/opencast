# PublicationToWorkspaceWorkflowOperationHandler

## Description
The PublicationToWorkspaceWorkflowOperationHandler can be used to copy the content from publication channels to workspace.
With this workflow one can copy or manipulate published elements without re-encoding.
## Parameter Table

|Configuration Key  |Example           |Description                                           |
|-------------------|------------------|------------------------------------------------------|
|publication-channel|engage-player, internal, oaipmh    |the publication-channel |
|source-flavors     |presenter/delivery|the "," separated source flavor list to move        |
|source-tags        |engage-download   |the "," separated source tag list to move             | 
|target-tags        |archive           |the "," separated list of tags to add to the moved elements| 



## Operation Example
```
  <operation id="publication-channel-to-workspace"
      description="Copy publication channel to workspace"
      fail-on-error="true"
      exception-handler-workflow="partial-error">
    <configurations>
      <configuration key="source-channel">engage-player</configuration>
      <configuration key="source-flavors">presenter-delivery,presentation-delivery</configuration>
      <configuration key="source-tags">engage-download,engage-streaming</configuration>
      <configuration key="target-tags">archive</configuration>
    </configurations>
  </operation>
```