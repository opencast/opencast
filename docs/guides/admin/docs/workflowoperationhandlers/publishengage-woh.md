PublishEngageWorkflowOperation
==============================


Description
-----------

The PublishEngageWorkflowOperation will bring your media to the engage distribution channels (streaming, progressive
download, …)


Parameter Table
---------------

|configuration keys         |description                                                                   |
|---------------------------|------------------------------------------------------------------------------|
|check-availability         |Check if the media if reachable                                                |
|download-source-flavors    |Specifies which media should be published for download                        |
|download-source-tags       |Specifies which media should be published for download                        |
|download-target-subflavors |Subflavor to use for distributed material                                     |
|download-target-tags       |Modify tags of published media                                                |
|strategy                   |If there is no key, published media would be retracted before publishing      |
|                           |`<configuration key="strategy">merge</configuration>`                         |
|                           |merges new publication with existing publication                              |
|streaming-source-flavors   |Specifies which media should be published to the streaming server             |
|streaming-source-tags      |Specifies which media should be published to the streaming server             |
|streaming-target-tags      |Modify tags of published media                                                |
|streaming-target-subflavors|Subflavor to use for distributed material                                     |


Operation Example
-----------------

    <operation
        id="publish-engage"
        max-attempts="2"
        fail-on-error="true"
        exception-handler-workflow="error"
        description="Distribute and publish to engage player">
        <configurations>
            <configuration key="download-source-tags">engage,atom,rss</configuration>
            <configuration key="streaming-source-tags">engage</configuration>
            <configuration key="check-availability">true</configuration>
            <configuration key="strategy">merge</configuration>
        </configurations>
    </operation>
