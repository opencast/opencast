Analyze Audio Workflow Operation
================================

ID: `analyze-audio`


Description
-----------

The abalyze audio operation analyzes the first audio stream of a video or audio track through SoX
(http://sox.sourceforge.net/) and writes the result back to the given track.

This workflow operation handler can be used with audio and/or video files. At least one audio stream must be available
otherwise nothing happens. Here are the internal steps done by the different inputs:

### Used with Audio only file (forceTranscode is deactivated):

* Analyze the given audio file with SoX
* Write analyzed audio metadata back to the given track's mediapackage.

### Used with Video file or with Audio only file with forceTranscode activated:

* Extract audio file encoded as FLAC audio and save it temporary in a collection
* Analyze the previous encoded audio file with SoX
* Write analyzed audio metadata back to the given track's mediapackage.
* Delete the temporary encoded FLAC audio file

Example result track:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<track type="presentation/audio" id="audio">
  <mimetype>video/x-flac</mimetype>
  <tags />
  <url>fooVideo.flac</url>
  <checksum type="md5">46cb2e9df2e73756b0d96c33b1aaf055</checksum>
  <duration>65680</duration>
  <audio id="audio-1">
    <device />
    <encoder type="ADPCM" />
    <bitdepth>16</bitdepth>
    <channels>2</channels>
    <bitrate>62500.0</bitrate>
    <peakleveldb>-30</peakleveldb> <!-- NEW -->
    <rmsleveldb>-20</rmsleveldb> <!-- NEW -->
    <rmspeakdb>-10</rmspeakdb> <!-- NEW -->
  </audio>
</track>
```

Parameter Table
---------------

|configuration keys|example                           |description|default value|
|------------------|----------------------------------|-----------|-------------|
|source-flavors    |"presentation/work,presenter/work"|The "flavors" of the track to use as a source input|EMPTY|
|source-flavor     |"presentation/work"               |The "flavor" of the track to use as a source input|EMPTY|
|source-tags       |"engage,atom,rss"                 |The "tag" of the track to use as a source input|EMPTY|
|force-transcode   |"true" or "false"                 |Whether to force transcoding the audio stream (This is needed when trying to strip an audio stream from an audio only video container, because SoX can not handle video formats, so it must be encoded to an audio format)|FALSE|

Operation Example
-----------------

```xml
<operation
    id="analyze-audio"
    description="Analyze audio stream">
  <configurations>
    <configuration key="source-flavor">*/work</configuration>
    <configuration key="force-transcode">true</configuration>
  </configurations>
</operation>
```

Encoding Profiles
-----------------

Some of the encoding profiles necessary for this operation are not included
in Opencast per default, but the operation will not work without them.
You need to include the following encoding profiles by copy and pasting them in
a `.properties` file in the `etc/encoding` folder of your installation.

```properties
# SoX Audio only (strip video)
profile.sox-audio-only.work.name = sox audio only
profile.sox-audio-only.work.input = visual
profile.sox-audio-only.work.output = audio
profile.sox-audio-only.work.suffix = -work.flac
profile.sox-audio-only.work.ffmpeg.command = -i #{in.video.path} -vn -c:a flac #{out.dir}/#{out.name}#{out.suffix}
```
