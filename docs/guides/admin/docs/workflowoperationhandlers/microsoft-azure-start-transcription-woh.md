# Microsoft Azure Start Transcription Workflow Operation

ID: `microsoft-azure-start-transcription`

## Description

Microsoft Azure Start Transcription invokes the Azure Speech service by passing a file with an audio track to transcript.

Note: You have to configure the `Microsoft Azure Transcription Service` first to make use this operation. Read the [documentation](../configuration/transcription.configuration/microsoftazure.md) how to achieve it.

## Parameter Table

| Configuration keys                | Description                                                                                                                                                                                                                                           | Default value             | Example                     |
|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------|-----------------------------|
| source-flavors                    | The flavors of the media files to use as audio input. Only the first available track will be used.                                                                                                                                                    | EMPTY                     | presenter/delivery          |
| source-tags                       | The comma separated list of tags of the file to transcribe.                                                                                                                                                                                           | EMPTY                     | transcript                  |
| skip-if-flavor-exists             | If this flavor already exists in the media package, skip this operation.<br/>To be used when the media package already has a transcript file. Optional                                                                                                | EMPTY                     | captions/source             |
| language                          | The language code to use for the transcription. Optional. If set, it will override the configuration language code. Read documentation for [supported languages](https://learn.microsoft.com/en-us/azure/ai-services/speech-service/language-support) | EMPTY                     | de-DE                       |
| audio-extraction-encoding-profile | The encoding profile to extract audio from media file for transcription.                                                                                                                                                                              | transcription-azure.audio | audio-to-opus.transcription |

**One of source-flavors or source-tags must be specified.**

## Examples

The example below will start the transcription on the first trimmed media file found in the media package, but only if `captions/source` element doesn't exist yet. The encoding profile to extract the audio stream is `custom-transcription-azure.audio`. 

```xml
<!-- This operation will start the transcription job -->
<operation
  id="microsoft-azure-start-transcription"
  fail-on-error="true"
  exception-handler-workflow="partial-error"
  description="Start Microsoft Azure transcription job">
  <configurations>
    <configuration key="source-flavors">*/trimmed</configuration>
    <!-- Skip this operation if flavor already exists. -->
    <!-- Used for cases when mediapackage already has captions. -->
    <configuration key="skip-if-flavor-exists">captions/source</configuration>
    <configuration key="audio-extraction-encoding-profile">custom-transcription-azure.audio</configuration>
  </configurations>
</operation>
```

The next example shows you how to create a transcription of the `presenter/trimmed` media file with the language code `de-DE`. The transcription will start if `captions/source` element is missing.

```xml
<!-- This operation will start the transcription job -->
<operation
  id="microsoft-azure-start-transcription"
  fail-on-error="true"
  exception-handler-workflow="partial-error"
  description="Start Microsoft Azure transcription job">
  <configurations>
    <configuration key="source-flavors">presenter/trimmed</configuration>
    <!-- Skip this operation if flavor already exists. -->
    <!-- Used for cases when mediapackage already has captions. -->
    <configuration key="skip-if-flavor-exists">captions/source</configuration>
    <configuration key="language">de-DE</configuration>
  </configurations>
</operation>
```
