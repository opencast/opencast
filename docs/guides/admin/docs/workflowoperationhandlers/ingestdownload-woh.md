# IngestDownloadWorkflowOperationHandler

## Description

With the IngestDownloadWorkflowOperationHandler it's possible to initially download external URI's from mediapackage
elements and store them to the working file repository. The external element URI's are then rewritten to the stored
working file repository URI.

In case of having external element URI's showing to a different Opencast working file repository, it's also possible to
delete them after downloading it by activating the "delete-external" option.
Additionally the "source-flavors" and "source-tags" option can be used to specify exactly, which external URI's should
be downloaded.

This operation is originally implemented to get rid of remaining files on ingest working file repositories.

## Parameter Table

| configuration keys | example              | description                                                                         | default value   |
|--------------------|----------------------|-------------------------------------------------------------------------------------|-----------------|
| delete-external    | "true"               | Whether to try to delete external working file repository URIs.                     | FALSE           |
| source-flavors     | "dublincore/episode" | List of flavors (separated by comma), elements matching a flavor will be downloaded | "\*/\*"         |
| source-tags        | "archive"            | List of tags (separated by comma), elements matching a tag will be downloaded       | "" (empty list) |
| tags-and-flavors   | "true"               | Whether both, a tag and a flavor, must match or if one is sufficient                | FALSE           |

## Operation Example

    <operation
      id="ingest-download"
      fail-on-error="false"
      description="Downloads external artifacts to the working file repository">
      <configurations>
        <configuration key="delete-external">true</configuration>
        <configuration key="source-flavors">dublincore/episode</configuration>
        <configuration key="source-tags">archive</configuration>
        <configuration key="tags-and-flavors">true</configuration>
      </configurations>
    </operation>
