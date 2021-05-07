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
|smil-flavor-subtype     |`smil`     |The output parameter is smil-flavor-subtype which provides the modification for the flavor subtype after this operation. The main flavor will be consistent and only the subtype will be replaced. |EMPTY|
|export-segments-duration |`true`    |Set this value to true and this operation will set two workflow properties for each analyzed track, the sum of duration of each non silent segment and same value in relation to the whole track length (in percent). |`false` |

### Workflow properties generated if export-segments-duration is set to true

For each source track the silence detection will run as expected. As a result we get a list of non-silent segments.
Each segment has a start and end timestamp, where we can calculate the segment duration.
The sum of duration of all non-silent segments will be set as workflow property with the name
`<source_flavor_type>_<source_flavor_subtype>_active_audio_duration` and value in seconds.
The relation to the whole track length will be set with the workflow property named
`<source_flavor_type>_<source_flavor_subtype>_active_audio_duration_percent` as percent value (0-100).


Example output for an 120 minutes long presenter/source track:
```
presenter_source_active_audio_duration = 5400
presenter_source_active_audio_duration_percent = 75
```


Operation Example
-----------------

    <operation
      id="silence"
      description="Executing silence detection">
      <configurations>
        <configuration key="source-flavors">*/audio</configuration>
        <configuration key="smil-flavor-subtype">smil</configuration>
        <configuration key="reference-tracks-flavor">*/preview</configuration>
        <configuration key="export-segments-duration">true</configuration>
      </configurations>
    </operation>
