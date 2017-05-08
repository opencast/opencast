# AssetManagerSnapshotWorkflowOperationHandler

## Description
The snapshot handler allows you to take a new, versioned snapshot of a media package.

## Parameter Table

|configuration keys|example|description|
|------------------|-------|-----------|
|source-tags|text|Comma separated list of tags. Specifies which media should be the source of a snapshop.|
|source-flavors|presenter/source|Comma separated list of flavors. Specifies which media should be the source of a snapshot.|


## Operation Example

    <operation
      if="${snapshotOp}"
      id="snapshot"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Archiving">
      <configurations>
        <configuration key="source-tags">archive</configuration>
      </configurations>
    </operation>
