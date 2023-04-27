Analyze Media Package Workflow Operation
========================================

ID: `analyze-mediapackage`


Description
-----------

The analyze media package operation  analyzes the mediapackage and sets workflow instance
variables based on the content of the medapackage. These variables can then be used to control if workflow
operations should be executed or skipped.

Workflow Instance Variables
---------------------------

|Name              |Example                        |Description                                                  |
|------------------|-------------------------------|-------------------------------------------------------------|
|`*flavor*_exists` |`presenter_source_exists=true` |Whether an element with given flavor is in the mediapackage. |
|`*flavor*_type`   |`presenter_source_type=Track`  |The type of the element with the given flavor. Possible values are: `Attachment`, `Catalog`, `Track`. |



Parameter Table
---------------

If no configuration keys are specified, workflow instance variables will be set for every mediapackage element.

If no mediapackage element matches a configuration key, no workflow instance variables will be set for that key. For example, the operation will never generate `presentation_work_exists=false`.

|Configuration Key|Example            |Description                                       |
|-----------------|-------------------|--------------------------------------------------|
|source-flavors   |`*/work`           |The comma separated list of flavors of the elements we are interested in. |
|source-tags      |`delivery, 1080p`  |The comma separated list of tags of the elements we are interested in.|


Operation Example
-----------------

```xml
<operation
    id="analyze-mediapackage"
    description="Analyze media package and set control variables">
</operation>
```

The operation will create workflow instance variables like this:

```
dublincore_episode_exists=true
dublincore_episode_type=Catalog
presentation_source_exists=true
presentation_source_type=Track
security_xacml+episode_exists=true
security_xacml+episode_type=Attachment
```
