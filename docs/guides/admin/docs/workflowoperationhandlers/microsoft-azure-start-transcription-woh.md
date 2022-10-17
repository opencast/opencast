Microsoft Azure Start Transcription Workflow Operation
======================================================

ID: `microsoft-azure-start-transcription`

Description
-----------

Microsoft Azure Start Transcription invokes the Azure Speech-to-Text service by passing a file with an audio track
to be translated to text.

Parameter Table
---------------

|configuration keys|description|default value|example|
|------------------|-------|-----------|-------------|
|source-flavor|The flavor of the file to be sent for translation.|EMPTY|presenter/delivery|
|source-tag|The flavor of the file to be sent for translation.|EMPTY|transcript|
|skip-if-flavor-exists|If this flavor already exists in the media package, skip this operation.<br/>To be used when the media package already has a transcript file. Optional|false|captions/vtt+en-us|
|language-code|The language code to use for the transcription. Optional. If set, it will override the configuration language code|EMPTY|en-US, [supported languages](https://docs.microsoft.com/de-de/azure/cognitive-services/speech-service/language-support?tabs=speechtotext#speech-to-text)|
|auto-detect-language|Activate automatic language detection by Azure. Optional. Overrides the language set in language-code. If set, will override the value in the configuration|false|true
|auto-detect-languages|A list of language codes. The Azure language auto detection will pick it's detected language from one of these. The language auto detection cannot detect any languages not specified in this list. The list needs to have at least one element and can have at most four elements.|EMPTY|en-US,de-DE,it-IT

**One of source-flavor or source-tag must be specified.**

Examples
--------

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

```xml
    <!-- Start Microsoft Azure transcription job, auto detect language -->
    <operation
        id="microsoft-azure-start-transcription"
        fail-on-error="true"
        exception-handler-workflow="partial-error"
        description="Start Microsoft Azure transcription job">
      <configurations>
        <configuration key="auto-detect-language">true</configuration>
        <configuration key="auto-detect-languages">es-ES,fr-FR,nl-NL,ja-JP</configuration>
      </configurations>
    </operation>
```
