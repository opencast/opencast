# NormalizeAudioWorkflowOperationHandler

## Description
The NormalizeAudioiWorkflowOperationHandler normalizes the first audio stream of a video or audio track through SoX (http://sox.sourceforge.net/), it creates a new track with a reference to the original track which can be flavored and tagged.
This workflow operation handler can be used with audio and/or video files, at least one audio stream must be available otherwise nothing happens. Here are the internal steps done by the different inputs:

### Used with Audio only file (forceTranscode is deactivated):

 - Check if necessary RMS Lev dB value is already in the track's metadata. If not run audio analyzation.
 - Run audio normalization with original audio file.
 - Replace the normalized audio file with the original.
 - Write analyzed audio metadata to the track's mediapackage.
 - Delete all used temporary files

###Used with Audio only file and forceTranscode activated:

 - Check if necessary RMS Lev dB value is already in the track's metadata. If not run audio analyzation.
 - (forceTranscode step) Encode audio to FLAC. (Must be used when given audio file format is not supported by SoX)
 - Run audio normalization with original audio file or encoded FLAC audio file.
 - (forceTranscode step) Mux normalized audio file back to the original audio container by replacing it with the original audio stream.
 - Write analyzed audio metadata to the track's mediapackage.
 - Delete all used temporary files

### Used with Video file:

 - Extract audio file encoded as FLAC audio and save it temporary in a collection
 - Check if necessary RMS Lev dB value is already in the track's metadata. If not run audio analyzation.
 - Run audio normalization with extracted audio file.
 - Mux normalized audio file back to the original video container by replacing it with original audio stream.
 - Write analyzed audio metadata to the track's mediapackage.
 - Delete all used temporary files

Example result track:

    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <track ref="track:track-2" type="presenter/normalized" id="70626874-17d2-480d-9d30-c10f0824961c">
        <mimetype>audio/x-flv</mimetype>
        <tags>
            <tag>norm</tag>
        </tags>
        <url>http://localhost:8080/files/mediapackage/8a510168-9102-425f-81e9-0943774dd229/70626874-17d2-480d-9d30-c10f0824961c/demo_slide_video_6min_buss.flv</url>
        <checksum type="md5">4e30d7d4305b0793f301816e796471db</checksum>
        <duration>414407</duration>
        <audio id="audio-1">
            <device/>
            <encoder type="MPEG Audio"/>
            <bitdepth>16</bitdepth>
            <channels>2</channels>
            <bitrate>64000.0</bitrate>
            <peakleveldb>-4.03</peakleveldb> <!-- NEW -->
            <rmsleveldb>-30.54</rmsleveldb> <!-- NEW -->
            <rmspeakdb>-10.85</rmspeakdb> <!-- NEW -->
        </audio>
    </track>

## Parameter Table

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|source-flavors	|"presentation/work,presenter/work"	|The "flavors" of the track to use as a source input	|EMPTY|
| source-flavor	|"presentation/work"	|The "flavor" of the track to use as a source input	|EMPTY|
|source-tags	|"engage,atom,rss"	|The "tag" of the track to use as a source input	|EMPTY|
|target-flavor	|"presentation/normalized"	|The flavor to apply to the normalized file	|EMPTY|
|target-tags	|"norm"	|The tags to apply to the normalized file	|EMPTY|
|**target-decibel**\*|-30.4	|The target RMS Level Decibel	|EMPTY|
|force-transcode	|"true" or "false"	|Whether to force transcoding the audio stream (This is needed when trying to strip an audio stream from an audio only video container, because SoX can not handle video formats, so it must be encoded to an audio format)	|FALSE|

\* **required keys**

##Operation Example

    <operation
      id="normalize-audio"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Normalize audio stream">
      <configurations>
        <configuration key="source-flavor">*/work</configuration>
        <configuration key="target-flavor">*/normalized</configuration>
        <configuration key="target-tags">norm</configuration>
        <configuration key="target-decibel">-30</configuration>
        <configuration key="force-transcode">true</configuration>
      </configurations>
    </operation>
