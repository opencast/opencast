Speech to Text Workflow Operation
==============================

ID: `speechtotext`

Description
-----------

The speech to text operation can be used to generate subtitles for Videos or Audio files. Currently, there are two STT
engines available, [Whisper](../modules/transcription.modules/whisper.md) and 
[Vosk](../modules/transcription.modules/vosk.md). The subtitles file format ist WebVTT.

This operation is designed to only work with one source file per flavor. If you want to use multiple source files in one
flavor, you need to set the same tag to each element.

Parameter Table
---------------

| configuration keys | required | Example           | description                                                                                                                                        |
|--------------------|----------|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| source-flavor      | yes      | source/presenter  | The source media package to use                                                                                                                    |
| source-tags        | no       | 1080p             | The tags that the source track uses                                                                                                                   |
| target-flavor      | yes      | archive           | Flavor of the produced subtitle file. The subflavor supports the language-code placeholder `#{lang}`                                               |
| target-element     | no       | attachment        | Define where to append the subtitles file. Possibilities are: as a 'track' or as an 'attachment'. The default is "attachment".                     |
| language-code      | no       | de                | The language of the video or audio source (default is "eng"). Vosk only: It has to match the name of the language model directory. See 'vosk-cli'. |
| language-fallback  | yes*     | en                | The fallback value if the dublin core/media package language field is not present.                                                                 |
| target-tags        | no       | delivery/captions | Tags for the subtitle file (Whisper only: If no `language-code`,the tag `lang:{code}` will be auto generated)                                      |
 | translate          | no       | true              | Transcription is translated into English, valid values `true` or `false` (Whisper Only)                                                            |

 *Vosk Only, default value can be modified on Vosk config file.

Requirements
------------

In order for it to work, you have to install the vosk-cli or whisper package


Operation Examples
------------------

If you want to display the subtitles in the Paella player, the WebVTT files have to be published and
the target SubFlavor has to be configured like this: `vtt+LANG`, where `LANG` is
the language option, that will be displayed in the Paella player. Please note the description above
for the `language-code` field.

Paella Player also can detect the tag `lang:{code}` to show the language in the subtitle menu.

```XML
<operation
    id="speechtotext"
    description="Generates subtitles for video and audio files">
  <configurations>
    <configuration key="source-flavor">*/source</configuration>
    <configuration key="target-flavor">captions/vtt+en</configuration>
    <configuration key="target-element">attachment</configuration>
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
      <configuration key="target-flavor">captions/vtt+#{lang}</configuration>
      <configuration key="target-element">attachment</configuration>
      <configuration key="target-tags">archive,subtitle,engage-download</configuration>
    </configurations>
</operation>
```

Language code
-------------------------

The accepted language code are the two letter codes defined in ISO 639-1. A reference list can be found here:

[List of ISO 639-1 codes](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)