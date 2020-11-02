SelectTracksWorkflowOperationHandler
====================================


Description
-----------

The SelectTracksWorkflowOperationHandler can be used in case not all source tracks should be processed. For example,
given a recording with a presenter and a presentation track, the final recording to be published should only include the
video stream of the presenter track and the audio stream of the presentation track.

The workflow operation will use workflow properties set by the Opencast video editor to determine which tracks should be
selected for further processing and add them to the media package based on `target-flavor` and `target-tags`.

**IMPORTANT:** The input tracks need to be inspected using the workflow operation [inspect](inspect-woh.md) before
running this operation.


Parameter Table
---------------

Configuration Key | Example   | Description
:-----------------|:----------|:-----------
source-flavor\*   | */source  | The flavor of the track(s) to use as a source input
target-flavor\*   | */work    | The flavor of the target track(s)
target-tags       | download  | The tags applied to all target tracks
audio-muxing      | force     | Move single-audio media packages to a specific track (see below)
force-target      | presenter     | Target track for the `force` setting for `audio-muxing`

\* mandatory configuration key


Workflow Properties
-------------------

The names of the workflow properties that control which streams are included in the output tracks are

    "hide_" + source-flavor.type + "_audio"
    "hide_" + source-flavor.type + "_video"

Example:

For the source flavor `presenter/work`, use the boolean workflow properties `hide_presenter_audio` and
`hide_presenter_video` to control which streams should be included in the output tracks.

Those properties are set by the Opencast video editor and can also be set using a custom workflow configuration panel.


Audio Muxing
-----------------

The optional `audio-muxing` parameter has three possible values: `none` (same as omitting the option), `force` and
`duplicate`.

### `none` ###

If `none` is specified or the option is omitted, the audio stream is taken from the specified `source-flavor` track and is
edited according to the selections in video editor’s “Tracks” panel. The resulting tracks are stored in the
corresponding `target-flavor` and `target-tags` are applied.

Note: If your editing results in a single video and single audio (track/stream) they will be muxed together even if
this option is set to `none`.

### `force` ###

The parameter value `force` only applies to media packages that have exactly one non-hidden audio stream. For media
packages without an audio stream or with more than one audio stream, the behavior is the same as if the parameter were
omitted. The same applies to media packages for which there is only one audio stream, and it already belongs to the
track with flavor type given by `force-target` (or `presenter` if that parameter is omitted).

If, however, there is only one non-hidden audio stream and it does *not* belong to the track given by `force-target`,
then the WOH will “move” the audio stream to this target track. Specifically, it will mux the video stream of
`force-target` with the audio stream it found. Then, it removes the audio stream from the original track.

For example, consider a media package with two tracks, *presenter* and *presentation*. Both of these tracks have
audio components, however the *presenter* audio stream is hidden. This WOH will mux *presentations*'s audio stream
with *presenter*'s video, and remove the audio track from *presentation*'s video.

### `duplicate` ###

The parameter value `duplicate` only applies to media packages that have exactly one non-hidden audio stream and no
hidden video streams. For these media packages, the WOH will mux the audio stream it found to all video streams in
the media package. For media packages without an audio stream or with more than one audio stream, the behavior is
the same as if the parameter were omitted.

Encoding Profiles
-----------------

This workflow operation handler depends on the presence of the following encoding profiles:

Name            | Description
----------------|------------
video-only.work | Removes all audio streams from a media track
audio-only.work | Removes all video streams from a media track
mux-av.work     | Mux a video stream and an audio stream into a media track

Note that those encoding profiles are included in the default configuration of Opencast.

Operation Example
-----------------

    <operation
      id="select-tracks"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Select tracks for further processing">
      <configurations>
        <configuration key="source-flavor">*/source</configuration>
        <configuration key="target-flavor">*/work</configuration>
        <configuration key="audio-muxing">force</configuration>
      </configurations>
    </operation>
