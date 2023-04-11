Speech to Text Workflow Operation
==============================

ID: `speechtotext`

Description
-----------

The speech to text operation can be used to generate subtitles for Videos or Audio files. Currently, there are two STT
engines available, [Whisper](../modules/transcription.modules/whisper.md) and 
[Vosk](../modules/transcription.modules/vosk.md). The subtitles file format ist WebVTT.


Parameter Table
---------------

| configuration keys | required | description                                                                                                                                        |
|--------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| source-flavor      | yes      | The source media package to use.                                                                                                                   |
| target-flavor      | yes      | Flavor of the produced subtitle file.                                                                                                              |
| target-element     | no       | Define where to append the subtitles file. Possibilities are: as a 'track' or as an 'attachment'. The default is "track".                          |
| language-code      | no       | The language of the video or audio source (default is "eng"). Vosk only: It has to match the name of the language model directory. See 'vosk-cli'. |
| language-fallback  | yes*     | The fallback value if the dublin core/media package language field is not present.                                                                 |
| target-tags        | no       | Tags for the subtitle file.**                                                                                                                      |
| translate          | no       | Transcription is translated into English (Whisper Only)                                                                                            |


*Vosk Only, default value can be modified on Vosk config file.
**For conventionally used tags see the general page on [Subtitles](../../modules/subtitles).

Requirements
------------

In order for it to work, you have to install the vosk-cli or whisper package


Operation Examples
------------------

```XML
<operation
    id="speechtotext"
    description="Generates subtitles for video and audio files">
  <configurations>
    <configuration key="source-flavor">*/source</configuration>
    <configuration key="target-flavor">captions/source</configuration>
    <configuration key="target-element">track</configuration>
    <configuration key="target-tags">archive,subtitle,engage-download</configuration>
    <configuration key="language-code">eng</configuration>
  </configurations>
</operation>
```

```XML
<operation
    id="speechtotext"
    description="Generates subtitles for video and audio files, derive language-code from metadata">
    <configurations>
      <configuration key="source-flavor">*/source</configuration>
      <configuration key="target-flavor">captions/source</configuration>
      <configuration key="target-element">track</configuration>
      <configuration key="target-tags">archive,subtitle,engage-download</configuration>
    </configurations>
</operation>
```
