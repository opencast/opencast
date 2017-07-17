# CleanupWorkflowOperationHandler

## Description
This opertaion removes all files in the working file repository for mediapackage elements that don't match one of the
*preserve-flavors* configuration value.
It is used as last workflow operation in a workflow to ensure that temporary processing artefacts are removed from the working file repository.

## Parameter Table

|Configuration Key|Example                |Description                                       |
|-----------------|-----------------------|--------------------------------------------------|
|preserve-flavors  |security/\*,\*/source |Comma-separated list of flavors to be preserved.  |
|delete-external   |true                  |Whether to try to delete external working file repository URIs using HTTP delete. Default is false.|
|delay             |5                     |Time to wait in seconds before removing files. Default is 1s.|

\* mandatory configuration key

Notes:

* If *delete-external* is set to true, all externally referenced media package elements will be removed from its source independent of the value of *preserve-flavors*

##Operation Example

    <operation
      id="cleanup"
      fail-on-error="false"
      description="Remove temporary processing artifacts">
      <configurations>
        <configuration key="preserve-flavors">security/*</configuration>
        <configuration key="delete-external">true</configuration>
        <configuration key="delay">5</configuration>
      </configurations>
    </operation>

