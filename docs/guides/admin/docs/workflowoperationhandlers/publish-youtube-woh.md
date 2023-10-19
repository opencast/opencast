Publish YouTube Workflow Operation
==================================

ID: `publish-youtube`


Description
-----------

The publish YouTube operation publishes a single stream to YouTube. This stream must meet YouTube's format
requirements, and may consist of one audio and/or video stream. If you want to publish both your presenter and presentation
streams we suggest using the [Composite](composite-woh.md) workflow operation handler to prepare a composite file
with both streams inside of it.  


Parameter Table
---------------

|configuration keys         |description                                                                   |
|---------------------------|------------------------------------------------------------------------------|
|source-flavors             |The flavors to publish to YouTube                                             |
|source-tags                |The tags to publish to YouTube                                                |


Operation Example
-----------------

```xml
<operation
    id="publish-youtube"
    description="Publishing to YouTube">
  <configurations>
    <configuration key="source-tags">youtube</configuration>
  </configurations>
</operation>
```
