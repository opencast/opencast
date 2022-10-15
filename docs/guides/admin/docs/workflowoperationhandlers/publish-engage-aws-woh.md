Publish Engage AWS/S3 Workflow Operation
========================================

ID: `publish-engage-aws`


Description
-----------

The publish-engage-aws operation will publish your recording to the normal publication channel (e.g. engage), but the
media files will be hosted via AWS S3/Cloudfront.


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
|merge-force-flavors        |Flavors of elements for which an update is enforced when mergeing catalogs.                  |
|                           |Defaults to `dublincore/*,security/*`.


Operation Example
-----------------

```xml
<operation
    id="publish-engage-aws"
    max-attempts="2"
    fail-on-error="true"
    exception-handler-workflow="error"
    description="Distribute and publish to engage player using AWS S3">
  <configurations>
    <configuration key="download-source-tags">engage,atom,rss</configuration>
    <configuration key="check-availability">true</configuration>
    <configuration key="strategy">merge</configuration>
  </configurations>
</operation>
```
