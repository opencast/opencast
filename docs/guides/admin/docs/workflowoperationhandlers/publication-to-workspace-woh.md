# PublicationToWorkspaceWorkflowOperationHandler

## Description
The CopyWorkflowOperationHandler can be used to copy the content form publication channels to workspace.
Wit this Workflow one can copy or maipulate published elements without reencoding.
## Parameter Table

|Configuration Key  |Example           |Description                                           |
|-------------------|------------------|------------------------------------------------------|
|publication-channel|engage-player     |the publication-channel interal,engage-player,oaipm   |
|source-flavor      |presenter/delivery|the "," seperatated source flavor list to move        |
|source-tag         |engage-download   |the "," seperated source tag list to move             | 
|target-tags        |archive           |the "," seperated of tags to add to the moved elements| 


\* mandatory configuration key

Notes:

This is working since opencast 11. When publication channel data is also stored in the elastic search index.


## Operation Example

    <operation id="publication-channel-to-workspace"
             description="Copy publication channel to workspace"
             fail-on-error="true"
             exception-handler-workflow="partial-error">
    <configurations>
      <configuration key="source-channel">engage-player</configuration>
      <configuration key="source-flavor">presenter-delivery,presentation-delivery</configuration>
      <configuration key="source-tag">engage-download,engage-streaming</configuration>
      <configuration key="target-tags">archive</configuration>
    </configurations>
  </operation>

