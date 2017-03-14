AnalyzeTracksWorkflowOperationHandler
=====================================


Description
-----------

The AnalyzeTracksWorkflowOperationHandler analyzes specified tracks in the mediapackage and sets workflow instance
variables based in the tracks audio and video properties. These variables can then be used to control if workflow
operations should be executed.

Note that this operation should be preceded by the inspect workflow operation handler.

For all tracks matching the flavor specified by the mandatory configuration key *source-flavor*, the following boolean
workflow instance variables will be set:

|Name                                |Example                          |Description                                    |
|------------------------------------|---------------------------------|-----------------------------------------------|
|*type*\_*subtype*\_audio            |`presenter_source_audio`         |Track contains at least one audio stream       |
|*type*\_*subtype*\_video            |`presenter_source_video`         |Track contains at least one video stream       |
|*type*\_*subtype*\_xresolution      |`presenter_source_xresolution`   |Horizontal resolution of the video stream      |
|*type*\_*subtype*\_yresolution      |`presenter_source_yresolution`   |Vertical resolution of the video stream        |
|*type*\_*subtype*\_aspect           |`presenter_source_aspect`        |Aspect ratio of the video stream as fraction   |
|*type*\_*subtype*\_resolution\_*res*|`presenter_source_resolution_720`|Video has minimal vertical resolution of *res* |
|*type*\_*subtype*\_aspect\_*ratio*  |`presenter_source_aspect_4_3`    |Video has an aspect ratio of *ratio*           |


Parameter Table
---------------

|Configuration Key|Example            |Description                                       |
|-----------------|-------------------|--------------------------------------------------|
|source-flavor\*  |`presentation/work`|The "flavor" of the track to use as a source input|
|xresolution      |`480,720,1080`     |Resolutions to check                              |
|yresolution      |`480,720,1080`     |Resolutions to check                              |
|aspect-ratio     |`4/3,16/9`         |Aspect ratio to check                             |

\* mandatory configuration key

Note that if there are multiple video streams with one flavor, only the information from the last video stream are
taken.


Operation Example
-----------------

    <operation
      id="analyze-tracks"
      fail-on-error="true"
      exception-handler-workflow="ng-partial-error"
      description="Analyze tracks in media package and set control variables">
      <configurations>
        <configuration key="source-flavor">*/source</configuration>
        <configuration key="resolution">480,720,1080</configuration>
        <configuration key="aspect-ratio">4/3,16/9</configuration>
      </configurations>
    </operation>

If a video track with a resolution of 1280x720 and an included audio stream is passed to this operation as
`presentiation/source`, the resulting variables would be:

    presentation_source_aspect=16/9
    presentation_source_aspect_16_9=true
    presentation_source_aspect_4_3=false
    presentation_source_audio=true
    presentation_source_resolution_x=1280
    presentation_source_resolution_y=720
    presentation_source_resolution_y_1080=false
    presentation_source_resolution_y_720=true
    presentation_source_resolution_y_480=true
    presentation_source_video=true
