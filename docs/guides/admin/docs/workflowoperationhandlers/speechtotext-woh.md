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

| Configuration Keys       | Required | Example          | Description
|--------------------------|----------|------------------|-------------
| source-flavor            | yes      | presenter/source | The source media package to use
| target-flavor            | yes      | archive          | Flavor of the produced subtitle
| target-tags              | no       | captions/source  | Tags applies to the resulting subtitle element²³.
| target-element           | no       | track            | Define where to append the subtitles file. Possibilities are: as a 'track' or as an 'attachment' (default: `track`).
| language-code            | no       | de               | The language of the video or audio source⁴.
| language-fallback        | no¹      | en               | Optional fallback value if the dublin core/media package language field is not set.
| translate                | no       | true             | Transcription is translated into English, valid values `true` or `false` (Whisper/WhisperC++ only)
| limit-to-one             | no       | true             | Limits the maximum of generated subtitles to one.
| track-selection-strategy | no       | everything       | Define what tracks shall be selected for subtitle generation if used together with `limit-to-one` (default: `everything`).
| async                    | no       | false            | Start transcription in the background. Use [`speechtotext-attach`](speechtotext-attach-woh.md) to get the finished transcriptions later in the workflow (default: `false`).


1. Vosk default value can be modified on Vosk config file.
2. For conventionally used tags see the general page on [Subtitles](../configuration/subtitles.md). The `generator`
   and `generator-type` tags will be set automatically. For Whisper, iIf no `language-code` is set, the `lang` tag will
   be auto-generated.
3. This has no effect if the transcription is run asynchronously. It only applies to attached in this operation.
3. Vosk only: It has to match the name of the language model directory. See 'vosk-cli'.

Requirements
------------

In order for it to work, you have to install the vosk-cli, whisper or whispercpp package.

Track Selection Strategy
------------------------

Use the tack selection strategy in combination with the `limit-to-one` option to define  what tracks are selected for
subtitle generation.

For example, if you set `source-flavor` to `*/source` because your are unsure which track includes the audio, you can
use the `track-selection-strategy` to have Opencast prefer the presenter track for transcriptions.

Available options are:

 - `presenter_or_nothing`: only uses presenter tracks.
 - `presentation_or_nothing`: only uses presentation tracks.
 - `try_presenter_first`: look for presenter tracks first, if there are no usable, try to transcribe the other tracks.
 - `try_presentation_first`: look for presentation tracks first, falling back to other tracks if none are usable.
 - `everything`: just transcribe everything (this is the default).


Operation Examples
------------------

```yaml
- id: speechtotext
  description: Generates subtitles for video and audio files
  configurations:
    - source-flavor: '*/source'
    - target-flavor: captions/source
    - target-tags: engage-download
    - limit-to-one: true
```
