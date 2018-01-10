# PublishAWSS3WorkflowOperation


## Description

The PublishAWSS3WorkflowOperation will bring your media to the engage distribution channels (streaming, progressive download, …)


## Parameter Table

|configuration keys         |description                                                                   |
|---------------------------|------------------------------------------------------------------------------|
|check-availability         |Check if the media if rechable                                                |
|download-source-flavors    |Specifies which media should be published for download                        |
|download-source-tags       |Specifies which media should be published for download                        |
|download-target-subflavors |Subflavor to use for distributed material                                     |
|download-target-tags       |Modify tags of published media                                                |
|strategy                   |If there is no key, published media would be retracted before publishing      |
|                           | <configuration key="strategy">merge</configuration>                          |
|                           |merges new publication with existing publication                              |
|streaming-source-flavors   |Specifies which media should be published to the streaming server             |
|streaming-source-tags      |Specifies which media should be published to the streaming server             |
|streaming-tagret-tags      |Modify tags of published media                                                |
|streaming-target-subflavors|Subflavor to use for distributed material                                     |


## Operation Example

    <operation
      id="publish-aws"
      max-attempts="2"
      exception-handler-workflow="partial-error"
      description="Publishing to Amazon Web Services">
      <configurations>
        <configuration key="download-source-flavors">dublincore/*,security/*</configuration>
        <configuration key="download-source-tags">engage-download,atom,rss,mobile</configuration>
        <configuration key="streaming-source-tags">engage-streaming</configuration>
        <configuration key="strategy">merge</configuration>
        <configuration key="check-availability">true</configuration>
      </configurations>
    </operation>
