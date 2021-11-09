AnalyzeMediapackageWorkflowOperationHandler
=====================================


Description
-----------
The AnalyzeMediapackageWorkflowOperationHandler analyzes the mediapackage and sets workflow instance
variables based on the content of the medapackage. These variables can then be used to control if workflow
operations should be executed or skipped.

Workflow Instance Variables
---------------------------

|Name            |Example                        |Description                                                  |
|----------------|-------------------------------|-------------------------------------------------------------|
|*flavor*_exists |`presenter_source_exists=true` |Whether an element with given flavor is in the mediapackage. |
|*flavor*_type   |`presenter_source_type=Track`  |The type of the element with the given flavor. Possible values are: `Attachment`, `Catalog`, `Track`. |



Parameter Table
---------------

|Configuration Key|Example            |Description                                       |
|-----------------|-------------------|--------------------------------------------------|
|source-flavor    |`presentation/work`|The flavor of the element we are interested in.   |
|source-flavors   |`*/work`           |The comma separated list of flavors of the elements we are interested in. |
|source-tags      |`delivery, 1080p`  |The comma separated list of tags of the elements we are interested in.|


Operation Example
-----------------

    <operation
      id="analyze-mediapackage"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Analyze media package and set control variables">
      <configurations>
      </configurations>
    </operation>

The operation will create workflow instance variables like this:

    dublincore_episode_exists=true
    dublincore_episode_type=Catalog
    presentation_source_exists=true
    presentation_source_type=Track
    security_xacml_episode_exists=true
    security_xacml_episode_type=Attachment
