# AnalyzeWorkflowOperationHandler

## Description
The AnalyzeWorkflowOperationHandler analyzes specified tracks in the mediapackage and sets workflow instance variables that
can be used to control if workflow operations should be executed.

For all tracks matching the flavor *type*/*subtype* as specified by the mandatory configuration key *source-flavor*,
the following workflow instance variables will be set:

|Name                  |Type   |Description                                                                 |
|----------------------|-------|----------------------------------------------------------------------------|
|*type*_*subtype*_audio|Boolean|Set to *true* if track contains at least one audio stream, *false* otherwise|
|*type*_*subtype*_video|Boolean|Set to *true* if track contains at least one video stream, *false* otherwise|

## Parameter Table

|Configuration Key|Example            |Description                                       |
|-----------------|-------------------|--------------------------------------------------|
|source-flavor*   |"presentation/work"|The "flavor" of the track to use as a source input|

\* mandatory configuration key

##Operation Example

    <operation
      id="analyze"
      fail-on-error="true"
      exception-handler-workflow="ng-partial-error"
      description="Analyze tracks and set control variables">
      <configurations>
        <configuration key="source-flavor">*/work</configuration>
      </configurations>
    </operation>

