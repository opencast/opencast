# ArchiveWorkflowHandler

## Description
The ArchiveWorkflowHandler will archive the current state of the mediapackage.

## Parameter Table

|configuration keys|example|description|
|------------------|-------|-----------|
|source-tags|text| Specifies which media should be archived.|
|source-flavors|presenter/source	|Flavors that should be archived, separated by ","|


## Operation Example

    <operation
          if="${archiveOp}"
          id="archive"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Archiving">
          <configurations>
                <configuration key="source-tags">archive</configuration>
          </configurations>
    </operation>

