# Start Watson Transcription
## Description

The Start Watson Transcription invokes the IBM Watson Speech-to-Text service, passing an audio file to be translated to
text.

## Parameter Table

|configuration keys|description|default value|example|
|------------------|-------|-----------|-------------|
|source-flavor|The flavor of the audio file to be sent for translation.|EMPTY|presenter/delivery|
|source-tag|The flavor of the audio file to be sent for translation.|EMPTY|transcript-audio|
|skip-if-flavor-exists|If this flavor already exists in the media package, skip this operation.<br/>To be used when the media package already has a transcript file.|false|captions/vtt+en|

**One of source-flavor or source-tag must be specified.**

## Example
```xml
<!-- Extract audio from video in ogg/opus format -->

<operation
  id="compose"
  fail-on-error="true"
  exception-handler-workflow="partial-error"
  description="Extract audio for transcript generation">
  <configurations>
    <configuration key="source-tags">engage-download</configuration>
    <configuration key="target-flavor">audio/ogg</configuration>
    <configuration key="target-tags">transcript</configuration>
    <configuration key="encoding-profile">audio-opus</configuration>
    <!-- If there is more than one file that match the source-tags, use only the first one -->
    <configuration key="process-first-match-only">true</configuration>
  </configurations>
</operation>

<!-- Start IBM Watson recognitions job -->

<operation
  id="start-watson-transcription"
  fail-on-error="true"
  exception-handler-workflow="partial-error"
  description="Start IBM Watson transcription job">
  <configurations>
    <!--  Skip this operation if flavor already exists. Used for cases when mp already has captions. -->
    <configuration key="skip-if-flavor-exists">captions/vtt+en</configuration>
    <!-- Audio to be translated, produced in the previous compose operation -->
    <configuration key="source-tag">transcript</configuration>
  </configurations>
</operation>
```

#### Encoding profile used in example above
```
profile.audio-opus.name = audio-opus
profile.audio-opus.input = stream
profile.audio-opus.output = audio
profile.audio-opus.suffix = -audio.opus
profile.audio-opus.ffmpeg.command = -i /#{in.video.path} -c:a libvorbis -ac 1 -ar 16k -b:a 64k #{out.dir}/#{out.name}#{out.suffix}
```
