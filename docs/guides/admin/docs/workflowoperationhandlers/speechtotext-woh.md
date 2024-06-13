Speech to Text Workflow Operation
==============================

ID: `speechtotext`

Description
-----------

The speech to text operation can be used to generate subtitles from video or audio files. Currently, there are three
STT engines available, [Whisper](../configuration/transcription.configuration/whisper.md),
[WhisperC++](../configuration/transcription.configuration/whispercpp.md) and
[Vosk](../configuration/transcription.configuration/vosk.md).
The subtitles file format ist WebVTT.


Parameter Table
---------------

| configuration keys       | required | Example           | description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
|--------------------------|----------|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| source-flavor            | yes      | source/presenter  | The source media package to use                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| target-flavor            | yes      | archive           | Flavor of the produced subtitle file.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| target-element           | no       | track             | Define where to append the subtitles file. Possibilities are: as a 'track' or as an 'attachment'. The default is "track".                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| language-code            | no       | de                | The language of the video or audio source (default is "eng"). Vosk only: It has to match the name of the language model directory. See 'vosk-cli'.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| language-fallback        | yes*     | en                | The fallback value if the dublin core/media package language field is not present.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| target-tags              | no       | delivery/captions | Tags for the subtitle file.** The `generator` and `generator-type` tags will be set automatically. (Whisper/WhisperC++ only: If no `language-code` is set, the `lang` tag will be auto-generated.)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| translate                | no       | true              | Transcription is translated into English, valid values `true` or `false` (Whisper/WhisperC++ only)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| limit-to-one             | no       | true              | Limits the maximum of generated subtitles to one. In most cases only one track will have the audio with the speech. Use this to prevent multiple transcription jobs that wastes computing resources. To have more control over which tracks shall be tried first for subtitle generation, use the `track-selection-strategy` option.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| track-selection-strategy | no       | everything        | Use this in combination with `limit-to-one`. Define your strategy what tracks shall be selected for subtitle generation. If you use the `source-flavor` `*/source`, because your are unsure which track of your dual stream includes the audio, you can use the `track-selection-strategy` to try transcribing the presenter track first with the option`try_presenter_first` for example. These are the possible strategy options: `presenter_or_nothing`: only uses presenter tracks. `presentation_or_nothing`: only uses presentation tracks. `try_presenter_first`: looks for presenter tracks first, if there are no usable, try to transcribe the other tracks. `try_presentation_first`: looks for presentation tracks first, if there are no usable, try to transcribe the other tracks. `everything`: just transcribe everything (this is the default). |


*Vosk Only, default value can be modified on Vosk config file.
**For conventionally used tags see the general page on [Subtitles](../configuration/subtitles.md).

Requirements
------------

In order for it to work, you have to install the vosk-cli, whisper or whispercpp package.


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

Language code
-------------------------

The accepted language code are the two letter codes defined in ISO 639-1. A reference list can be found here:

[List of ISO 639-1 codes](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)
