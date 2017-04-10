# ArchiveWorkflowHandler

## Description
The ArchiveWorkflowHandler will archive the specified assets of the mediapackage.

## Parameter Table

|configuration keys|example          |description                                      |
|------------------|-----------------|-------------------------------------------------|
|source-tags       |text             |Specifies the media to be archived.              |
|source-flavors    |presenter/source |Flavors that should be archived, separated by ","|

## Required Assets

All archive operations require a dublincore/episode catalog to be archived along with the additional assets. Not adding
one will cause the operation to fail.

## Operation Example

    <operation
      id="archive"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Archiving">
      <configurations>
        <configuration key="source-tags">archive</configuration>
      </configurations>
    </operation>
