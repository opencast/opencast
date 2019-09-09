SilenceDetectionWorkflowOperationHandler
========================================

Description
-----------

The silence operation performs a silence detection on an audio-only input file.

## Parameter Table

|configuration keys      |example    |description|default value|
|------------------------|-----------|-----------|-------------|
|source-flavors          |`*/audio`  |The input parameter source-flavors takes one flavor/sub-type or multiple input flavors with the \*-operator followed by the sub-type|EMPTY|
|reference-tracks-flavor|`*/preview`|The input parameter reference-tracks-flavor is the subtype of the media files that should be included in the provided SMIL file. The * should not be modified here. In most cases it is not important which reference-tracks-flavor is selected as long as all relevant flavors are available within this feature. "preview" is not a bad choice as all files available within the video editor UI are also available with this flavor, unlike "source" where not all flavors may be available, as some recorders record all streams to one file and the tracks are separated afterwards. The editor operation afterwards will anyway try to select the best available quality.|  EMPTY|
|smil-flavor-subtype     |`smil`     |The output parameter is smil-flavor-subtype which provides the modificatory for the flavor subtype after this operation. The main flavor will be consistent and only the subtype will be replaced. |EMPTY|


Operation Example
-----------------

    <operation
      id="silence"
      description="Executing silence detection">
      <configurations>
        <configuration key="source-flavors">*/audio</configuration>
        <configuration key="smil-flavor-subtype">smil</configuration>
        <configuration key="reference-tracks-flavor">*/preview</configuration>
      </configurations>
    </operation>
