Publish Engage Workflow Operation
=================================

ID: `publish-engage`


Description
-----------

The publish-engage operation will bring your media to the engage distribution channels (streaming, progressive
download, …)


Parameter Table
---------------

|configuration keys         |description                                                                                  |
|---------------------------|---------------------------------------------------------------------------------------------|
|check-availability         |Check if the media is reachable                                                              |
|download-source-flavors    |Distribute any mediapackage elements with one of these (comma separated) flavors to download |
|download-source-tags       |Distribute any mediapackage elements with one of these (comma separated) tags to download    |
|download-target-subflavors |Subflavor to use for distributed material                                                    |
|download-target-tags       |Add tags (comma separated) to published media                                                |
|strategy                   |If there is no key, published media would be retracted before publishing                     |
|                           |`<configuration key="strategy">merge</configuration>`                                        |
|                           |merges new publication with existing publication                                             |
|streaming-source-flavors   |Specifies which media should be published to the streaming server                            |
|streaming-source-tags      |Specifies which media should be published to the streaming server                            |
|streaming-target-tags      |Add tags (comma separated) to published media                                                |
|streaming-target-subflavors|Subflavor to use for distributed material                                                    |
|merge-force-flavors        |Flavors of elements for which an update is enforced when merging catalogs.                  |
|                           |Defaults to `dublincore/*,security/*`.


Operation Example
-----------------

```xml
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
```
