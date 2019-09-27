# Nibity Attach Transcription

## Description

Nibity Attach Transcription attaches the results file received from the Nibity Transcription Service
to the media package. If the results file includes a captions file, the captions are also attached.

## Parameter Table

|configuration keys|description|default value|example|
|------------------|-------|-----------|-------------|
|transcription-job-id|This is filled out by the transcription service when starting the workflow.|EMPTY|**Should always be "${transcriptionJobId}"**|
|target-flavor|The flavor of the caption/transcription file generated. Mandatory.|EMPTY|captions/timedtext|
|target-tag|The tag to apply to the caption/transcription file generated. Optional.|EMPTY|archive|
|target-caption-format|The caption format to be generated. Optional. If not entered, the raw resulting file will be attached to the media package.|EMPTY|vtt|

## Example

```xml
    <!-- Attach caption/transcript -->
    <operation id="nibity-attach-transcription"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Attach captions/transcription">
      <configurations>
        <!-- This is filled out by the transcription service when starting this workflow -->
        <configuration key="transcription-job-id">${transcriptionJobId}</configuration>
        <!-- Caption generated will have the default flavor based on the target-caption-format and language e.g. captions/vtt+en -->
        <configuration key="target-caption-format">vtt</configuration>
        <configuration key="target-tag">engage-download</configuration>
      </configurations>
    </operation>

```
