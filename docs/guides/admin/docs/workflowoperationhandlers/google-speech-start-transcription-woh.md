# Google Speech Start Transcription

## Description

Google speech Start Transcription invokes the Google Speech-to-Text service by passing an audio file to be translated to 
text.

## Parameter Table

|configuration keys|description|default value|example|
|------------------|-------|-----------|-------------|
|source-flavor|The flavor of the audio file to be sent for translation.|EMPTY|presenter/delivery|
|source-tag|The flavor of the audio file to be sent for translation.|EMPTY|transcript|
|skip-if-flavor-exists|If this flavor already exists in the media package, skip this operation.<br/>To be used when the media package already has a transcript file. Optional|false|captions/timedtext|
|language-code|The language code to use for the transcription. Optional. If set, it will override the configuration language code|EMPTY|en-US, supported language: https://cloud.google.com/speech-to-text/docs/languages|

**One of source-flavor or source-tag must be specified.**

## Example

```xml
    <!--  Encode audio to flac -->
    <operation
      id="compose"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Extract audio for transcript generation">
      <configurations>
        <configuration key="source-flavor">*/source</configuration>
        <configuration key="target-flavor">audio/flac</configuration>
        <configuration key="target-tags">transcript</configuration>
        <configuration key="encoding-profile">audio-flac</configuration>
        <configuration key="process-first-match-only">true</configuration>
      </configurations>
    </operation> 

    <!-- Start Google Speech transcription job -->
    <operation
      id="google-speech-start-transcription"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Start Google Speech transcription job">
      <configurations>
        <!--  Skip this operation if flavor already exists. Used for cases when mp already has captions. -->
        <configuration key="skip-if-flavor-exists">captions/timedtext</configuration>
        <configuration key="language-code">en-US</configuration>
        <!-- Audio to be translated, produced in the previous compose operation -->
        <configuration key="source-tag">transcript</configuration>
      </configurations>
    </operation>
```

#### Encoding profile used in example above
```
profile.audio-flac.name = audio-flac
profile.audio-flac.input = stream
profile.audio-flac.output = audio
profile.audio-flac.suffix = -audio.flac
profile.audio-flac.mimetype = audio/flac
profile.audio-flac.ffmpeg.command = -i /#{in.video.path} -ac 1 #{out.dir}/#{out.name}#{out.suffix}
```