CleanupWorkflowOperationHandler
===============================

Description
-----------

This operation removes all files from the workspace and the working file repository which belong to media package
elements of the running workflow unless their flavor is matched by the value configured in `preserve-flavors`.  It is
usually used as last workflow operation in a workflow to ensure that temporary processing artefacts are removed.


Parameter Table
---------------

|Configuration Key|Example      |Description                                                        |Default|
|-----------------|-------------|-------------------------------------------------------------------|-------|
|preserve-flavors |`security/*` |Comma-separated list of flavors to be preserved.                   |       |
|delete-external  |`true`       |If files from external working file repositories should be deleted |`false`|
|delay            |`5`          |Seconds to wait before removing files                              |`1`    |

### Notes

- If `delete-external` is set to `true`, the externally referenced media package elements will be removed from its
  source where the value of `preserve-flavors` does not match
- If you have an shared working file repository setting `delete-external` to `false` will speed up the cleanup process
  while still removing all files.

Operation Example
-----------------

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
