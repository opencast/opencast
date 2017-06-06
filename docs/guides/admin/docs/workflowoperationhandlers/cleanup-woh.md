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

\* mandatory configuration key

Notes:

* If *delete-external* is set to true, the externally referenced media package elements will be removed from its source where the value of *preserve-flavors* does not match
* If you have an shared working file repository (see [Opencast Configuration on Multiple Servers Setup](../installation/multiple-servers/#orgopencastprojectorganization-mh_default_orgcfg)) set *delete-external* to false to speedup the process

##Operation Example

    <operation
      id="cleanup"
      fail-on-error="false"
      description="Remove temporary processing artifacts">
      <configurations>
        <configuration key="preserve-flavors">security/*</configuration>
        <configuration key="delete-external">true</configuration>
      </configurations>
    </operation>

