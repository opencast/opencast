# ZipWorkflowOperation

## Description
The ZipWorkflowOperationHandler creates a zip archive including all elements from the current mediapackage that are specified in the operation configuration. It then adds the archive to the as an attachment with the given flavor and tags to the mediapackage.

## Parameter Table

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|zip-collection|zips| A comma separated list of flavors to preserve from deleting.|zip|
|include-flavors|"*/source,dublincore/*"|Which elements to include in the archive. This configuration parameter accepts exact flavors like "presenter/source" as well as wildcard flavor definitions like "*/source".|(all)|
|target-flavor	|"archive/zip"	|The flavor of the created attachment.	|archive/zip|
|target-tags	|"archive"	|The tags to apply to the attachment.	|-|
|compression	|"true"	|Whether to compress the archive content. Usually, for media content this doesn't reduce size of the archive by a lot but adds significant processing time by the compression.	|FALSE|

## Operation Example

    <operation
        id="zip"
        description="Creating zipped recording archive"
        fail-on-error="true"
        exception-handler-workflow="cleanup">
        <configurations>
          <configuration key="zip-collection">failed.zips</configuration>
          <configuration key="include-flavors">*/source,dublincore/*</configuration>
          <configuration key="target-flavor">all/zip</configuration>
          <configuration key="compression">false</configuration>
        </configurations>
    </operation>
