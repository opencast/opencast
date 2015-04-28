# IngestDownloadWorkflowOperationHandler

## Description
With the IngestDownloadWorkflowOperationHandler it's possible to initially download external URI's from mediapackage elements and store them to the working file repository. The external element URI's are then rewritten to the stored working file repository URI.

In case of having external element URI's showing to a different Matterhorn working file repository, it's also possible to delete them after downloading it by activating the "delete-external" option.

This operation is originally implemented to get rid of remaining files on ingest working file repositories.

## Parameter Table

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|delete-external|"true"|Whether to try to delete external working file repository URIs.|FALSE|

## Operation Example

    <operation
      id="ingest-download"
      fail-on-error="false"
      description="Downloads external artifacts to the working file repository">
      <configurations>
        <configuration key="delete-external">true</configuration>
      </configurations>
    </operation>
