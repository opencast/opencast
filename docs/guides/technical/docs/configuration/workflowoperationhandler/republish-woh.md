# RepublishWorkflowOperation

## Description
The republish workflow operation handler will take a mediapackage from the archive and republish just its metadata. This is achieved by publishing the archived elements as specified in the "source-flavor" option and merging them with what had been published to search before. Alternatively, "merge" can be set to false which will result in the operation replacing what is in search.

This way one is able to use the archive's mediapackage editor to make changes to a recording's metadata and then use the "republish" workflow operation to publish the updated metadata to the search index. Note that you would want to distribute the catalogs to download before publishing because the search service will try to download them.

## Parameter Table

Tags and flavors can be used in combination.

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|source-flavors |"dublincore/episode"| Select all media package elements with any of these (comma separated) flavors. If no source flavor is specified, all archived elements will be republished, including tracks and attachments.|EMPTY|
|source-tags|"engage, publish"|Only select media package elements that are tagged with any of these (comma separated) tags.|EMPTY|
|merge|"true" or "false"|Indicates whether the republished mediapackage elements should be merged with what has been published to search so far. If set to "true", mediapackage elements that are selected (by means of flavor and tags) will replace what is in search. If set to "false", the mediapackage in search will be replaced completely.|true|

## Operation Example

    <operation
      id="republish"
      max-attempts="2"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Republishing metadata">
      <configurations>
        <configuration key="source-flavors">dublincore/*</configuration>    <configuration key="source-tags">engage,atom</configuration>
        <configuration key="merge">true</configuration>
      </configurations>
    </operation>
