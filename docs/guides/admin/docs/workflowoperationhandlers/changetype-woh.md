Clone Workflow Operation
========================

ID: `changetype`

Description
-----------

The change type workflow operation can be used to change the type of media package elements.


Parameter Table
---------------

|Configuration Key         |Description                                       |Example           |
|--------------------------|--------------------------------------------------|------------------|
|source-flavors            |The source flavor(s) to clone                     |presenter/source  |
|source-tags               |Comma-separated list of source-tags               |archive           |
|target-flavor\*           |The target flavor                                 |presenter/target  |
|target-type\*             |The target type                                   |track             |

\* mandatory configuration key

Notes:

- *source-flavor* and *source-tags* may be used both together to select media package elements based on both flavors and
  tags If *source-flavor* is not specified, all media package elements matching *source-tags* will be selected
- In case that neither *source-flavor* nor *source-tags* are specified, the operation will be skipped
- In case no media package elements match *source-flavor* and *source-tags*, the operation will be skipped


Operation Example
-----------------

```xml
<operation
    id="changetype"
    description="Change type of captions to track">
  <configurations>
    <configuration key="source-flavors">captions/*</configuration>
    <configuration key="target-flavor">captions/source</configuration>
    <configuration key="target-type">track</configuration>
  </configurations>
</operation>
```
