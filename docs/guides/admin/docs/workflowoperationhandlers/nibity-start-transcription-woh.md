# Nibity Start Transcription

## Description

Nibity Start Transcription invokes the Nibity Transcription Service by submitting
an audio or video file to be transcribed and captioned.

## Parameter Table

|configuration keys|description|default value|example|
|------------------|-------|-----------|-------------|
|source-flavor|The flavor of the audio file to be sent for translation.|EMPTY|presenter/delivery|
|source-tag|The flavor of the audio file to be sent for translation.|EMPTY|transcript|
|skip-if-flavor-exists|If this flavor already exists in the media package, skip this operation.<br/>To be used when the media package already has a transcript file. Optional|false|captions/timedtext|

**One of source-flavor or source-tag must be specified.**

## Example

```xml
    <!-- Start Nibity transcription job -->
    <operation
      id="nibity-start-transcription"
      max-attempts="3"
      retry-strategy="hold"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Start Nibity captions job">
      <configurations>
        <!-- Skip this operation if flavor already exists. Used for cases when mp already has captions. -->
        <configuration key="skip-if-flavor-exists">captions/vtt+en</configuration>
        <!-- Video to be captioned, produced in the previous compose operation -->
        <configuration key="source-flavor">*/${preview_out_flavor}</configuration>
      </configurations>
    </operation>
```
