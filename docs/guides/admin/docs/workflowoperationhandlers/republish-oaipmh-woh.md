# RepublishOaiPmhWorkflowOperation


## Description

The Republish OAI-PMH workflow operation will update metadata in your OAI-PMH repositories. In case that the media has
not been published before, this operation will skip. Otherwise all elements matching the flavors and tags will be replaced.
In case of missing elements in the media package, the published elements will be also removed.


## Parameter Table

| Configuration Keys | Description                                                                                                     |
|:-------------------|:----------------------------------------------------------------------------------------------------------------|
| source-flavors     | Republish any media package elements with one of these (comma-separated) flavors                                |
| source-tags        | Republish only media package elements that are tagged with one of these (comma-separated) tags                  |
| repository         | The name of the OAI-PMH repository where the media should be updated                                            |


## Operation Example

    <operation
      id="republish-oaipmh"
      description="Update recording metadata in default OAI-PMH repository">
      <configurations>
        <configuration key="source-flavors">dublincore/*,security/*</configuration>
        <configuration key="repository">default</configuration>
      </configurations>
    </operation>
