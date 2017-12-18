AnalyzeTracksWorkflowOperationHandler
=====================================


Description
-----------

The AnalyzeTracksWorkflowOperationHandler analyzes specified tracks in the mediapackage and sets workflow instance
variables based on the tracks audio and video properties. These variables can then be used to control if workflow
operations should be executed.

Note that this operation should be preceded by the inspect workflow operation handler.

For all tracks matching the flavor specified by the mandatory configuration key *source-flavor*, the following workflow
instance variables may be set:

|Name                                |Example                      |Description                                    |
|------------------------------------|-----------------------------|-----------------------------------------------|
|*flavor*\_media            |`presenter_source_media=true`         |Track with specific favor exists               |
|*flavor*\_audio            |`presenter_source_audio=true`         |Track contains at least one audio stream       |
|*flavor*\_video            |`presenter_source_video=true`         |Track contains at least one video stream       |
|*flavor*\_resolution\_x    |`presenter_source_resolution_x=1280`  |Horizontal resolution of the video stream      |
|*flavor*\_resolution\_y    |`presenter_source_resolution_y=720`   |Vertical resolution of the video stream        |
|*flavor*\_aspect           |`presenter_source_aspect=4/3`         |Exact aspect ratio of the video stream         |
|*flavor*\_aspect\_snap     |`presenter_source_aspect_snap=4/3`    |Nearest specified aspect ratio of the video    |


Parameter Table
---------------

|Configuration Key|Example            |Description                                       |
|-----------------|-------------------|--------------------------------------------------|
|source-flavor\*  |`presentation/work`|The "flavor" of the track to use as a source input|
|aspect-ratio     |`4/3,16/9`         |Snap to these aspect ratios if specified          |
|fail-no-tracks   |`false`            |Fail if flavor matches no tracks (Default: false) |

\* mandatory configuration key

Note that if there are multiple video streams with one flavor, only the information from the last video stream are
taken.


Snap to Aspect Ratio
--------------------

Snap-to-aspect can be used to deal with slightly off resolutions.  Given an SAR of 1, for example, a video with the
resolution of 640x481 pixels has almost an aspect ration of 4/3, but is 1 pixel too wide. For special encoding options
or cover generation, it would still be reasonable to use the 4/3 settings. If 4/3 is listed in the `aspect-ratio`
option, `…_aspect_snap` would be set to 4/3.


Operation Example
-----------------

    <operation
      id="analyze-tracks"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Analyze tracks in media package and set control variables">
      <configurations>
        <configuration key="source-flavor">*/source</configuration>
        <configuration key="aspect-ratio">4/3,16/9</configuration>
      </configurations>
    </operation>

If a video track with a resolution of 1280x720 and an included audio stream is passed to this operation as
`presentiation/source`, the resulting variables would be:

    presentation_source_aspect=16/9
    presentation_source_aspect_snap=16/9
    presentation_source_audio=true
    presentation_source_media=true
    presentation_source_resolution_x=1280
    presentation_source_resolution_y=720
    presentation_source_video=true
