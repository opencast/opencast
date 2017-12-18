# RepublishOaiPmhWorkflowOperation


## Description

The RepublishOaiPmhWorkflowOperation will update your media in your OAI-PMH repositories. In case that the media has
not been published before, this operation will skip.


## Parameter Table

| Configuration Keys | Description                                                                                                     |
|:-------------------|:----------------------------------------------------------------------------------------------------------------|
| source-flavors     | Republish any media package elements with one of these (comma-separated) flavors                                |
| source-tags        | Republish only media package elements that are tagged with one of these (comma-separated) tags                  |
| repository         | The name of the OAI-PMH repository where the media should be updated                                            |
| merge              | Merge with existing published data. Updated media package elements replace existing ones based on their flavor. |


## Operation Example

    <operation
        id="republish-oaipmh"
        fail-on-error="true"
        exception-handler-workflow="error"
        description="Distribute and publish to the OAI-PMH repository">
        <configurations>
            <configuration key="source-flavors">presenter/source, presentation/source</configuration>
            <configuration key="source-tags">oaipmh</configuration>
            <configuration key="repository">default</configuration>
            <configuration key="merge">true</configuration>
        </configurations>
    </operation>
