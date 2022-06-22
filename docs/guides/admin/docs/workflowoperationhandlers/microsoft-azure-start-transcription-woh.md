# Microsoft Azure Start Transcription

## Description

Microsoft Azure Start Transcription invokes the Azure Speech-to-Text service by passing a file with an audio track
to be translated to text.

## Parameter Table

|configuration keys|description|default value|example|
|------------------|-------|-----------|-------------|
|source-flavor|The flavor of the file to be sent for translation.|EMPTY|presenter/delivery|
|source-tag|The flavor of the file to be sent for translation.|EMPTY|transcript|
|skip-if-flavor-exists|If this flavor already exists in the media package, skip this operation.<br/>To be used when the media package already has a transcript file. Optional|false|captions/vtt+en-us|
|language-code|The language code to use for the transcription. Optional. If set, it will override the configuration language code|EMPTY|en-US, [supported languages](https://docs.microsoft.com/de-de/azure/cognitive-services/speech-service/language-support?tabs=speechtotext#speech-to-text)|

**One of source-flavor or source-tag must be specified.**

## Example

```xml
    <!-- Start Microsoft Azure transcription job -->
    <operation
        id="microsoft-azure-start-transcription"
        fail-on-error="true"
        exception-handler-workflow="partial-error"
        description="Start Microsoft Azure transcription job">
      <configurations>
        <!--  Skip this operation if flavor already exists. Used for cases when mediapackage already has captions. -->
        <configuration key="skip-if-flavor-exists">captions/vtt+de-DE</configuration>
        <configuration key="language-code">de-DE</configuration>
        <configuration key="source-flavor">presenter/prepared</configuration>
      </configurations>
    </operation>
```