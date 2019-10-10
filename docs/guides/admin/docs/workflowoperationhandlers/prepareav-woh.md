# PrepareAVWorkflowOperation

## Description
The PrepareAVWorkflowOperation works is like this:

If there are two tracks with the same flavor, and one of them contains a video stream only, while the other contains an
audio stream only, the implementation will call the composer's "mux" method, with the result that the audio will be
muxed with the video, using the video's movie container.

If it there is one track with a certain flavor, the "encode" method is called which will rewrite (vs. encode) the file
using the same container and codec (-vcodec copy, -a codec copy), while the container format is determined by ffmpeg via
the file's extension. The reason for doing this is that many media files are in a poor state with regard to their
compatibility (most often, the stream's codec contains differing information from the container), so we are basically
asking ffmepg to rewrite the whole thing, which will in many cases eliminate problems that would otherwhise occur later
in the pipeline (encoding to flash, mjpeg etc.).

## Parameter Table

|configuration keys|example|description|
|------------------|-------|-----------|
|source-flavor|presenter/source|Specifies which media should be processed.|
|target-flavor|presenter/work|Specifies the flavor the new files will get.|
|mux-encoding-profile    |mux-av.prepared    |The encoding profile to use for media that needs to be muxed (default is 'mux-av.work')|
|audio-video-encoding-profile    |av.prepared    |The encoding profile to use for media that is audio-video already and needs to be re-encodend (default is av.work)     |
|video-encoding-profile    |video-only.prepared    |The encoding profile to use for media that is only video and needs to be re-encodend (default is video-only.work)     |
|audio-encoding-profile    |audio-only.prepared    |The encoding profile to use for media that is only audio and needs to be re-encodend (default is audio-only.work)     |
|rewrite    |true    |Should files be rewritten     |
|audio-muxing-source-flavors|presentation/source,presentation/\*,\*/\*    |If there is no matching flavor to mux, search for a track with audio that can be muxed by going from left to right through this comma-separated list of source flavors|


## Operation Example

    <operation
      id="prepare-av"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Preparing presenter audio and video work versions">
      <configurations>
        <configuration key="source-flavor">presenter/source</configuration>
        <configuration key="target-flavor">presenter/work</configuration>
        <configuration key="rewrite">false</configuration>
        <configuration key="audio-muxing-source-flavors">*/?,*/*</configuration>
      </configurations>
    </operation>

## Audio Muxing
The PrepareAVWorkflowOperation can be used for audio muxing in case a matching source video track has no audio. Audio
muxing is performed as described below:

In case the *source-flavor* matches to exactly two tracks whereas one track is a video-only track and the other is an
audio-only track, those tracks will be merged into a single audio-video track.

If there is no such matching flavor to mux, additional audio muxing facilities can be controlled by the use of the
configuration key *audio-muxing-source-flavors*. That configuration key contains a comma-separated list of flavors that
defines the search order of how to find an audio track.

The following two wildcard characters can be used in flavors in that list:

* '*' will match to any type or subtype
* '?' will match to the type or subtype of the matching *source-flavor*

Note: In case that a flavor used with *audio-muxing-source-flavors* matches to multiple tracks within the media package
resulting in a list of matching tracks, the search order within that list is undefined, i.e. PrepareAVWorkflowOperation
will just pick any of those tracks that has audio.

### Example

    [...]
    <configuration key="source-flavor">presenter/*</configuration>
    <configuration key="audio-muxing-source-flavors">presenter-audio/?, presentation/?,presentation/*,?/audio,*/*</configuration>
    [...]

Let's assume that exactly one video-only track of flavor presenter/source in the media package and another track of
flavor audio/track that has audio.


In this example, the PrepareAVWorkflowOperation would perform the following steps:

1. Search tracks of flavor presenter-audio/source (presenter-audio/?)
2. Search tracks of flavor presentation/source (presentation/?)
3. Search tracks of flavor presentation/*
4. Search tracks of flavor presenter/audio (?/audio)
5. Search tracks of flavor \*/\*



