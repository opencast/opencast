Google Speech Start Transcription Workflow Operation
============================================================

ID: `google-speech-start-transcription`


Description
-----------

Google speech Start Transcription invokes the Google Speech-to-Text service by passing an audio file to be translated to 
text.


Parameter Table
---------------

|configuration keys|description|default value|example|
|------------------|-------|-----------|-------------|
|source-flavor|The flavor of the audio file to be sent for translation.|EMPTY|presenter/delivery|
|source-tag|The flavor of the audio file to be sent for translation.|EMPTY|transcript|
|skip-if-flavor-exists|If this flavor already exists in the media package, skip this operation.<br/>To be used when the media package already has a transcript file. Optional|false|captions/timedtext|
|language-code|The language code to use for the transcription. Optional. If set, it will override the configuration language code|EMPTY|en-US, supported language: https://cloud.google.com/speech-to-text/docs/languages|

**One of source-flavor or source-tag must be specified.**


Example
-------

```yaml
  # Encode audio to flac
  - id: encode
    description: Extract audio for transcript generation
    configurations:
      - source-flavor: '*/source'
      - target-flavor: audio/flac
      - target-tags: transcript
      - encoding-profile: audio-flac
      - process-first-match-only: true

  # Start Google Speech transcription job
  - id: google-speech-start-transcription
    description: Start Google Speech transcription job
    configurations:
      # Skip this operation if flavor already exists. Used for cases when mp already has captions.
      - skip-if-flavor-exists: captions/timedtext
      - language-code: en-US
      # Audio to be translated, produced in the previous compose operation
      - source-tag: transcript
```

### Encoding profile used in example above

```properties
profile.audio-flac.name = audio-flac
profile.audio-flac.input = stream
profile.audio-flac.output = audio
profile.audio-flac.suffix = -audio.flac
profile.audio-flac.mimetype = audio/flac
profile.audio-flac.ffmpeg.command = -i /#{in.video.path} -ac 1 #{out.dir}/#{out.name}#{out.suffix}
```
