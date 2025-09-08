Start Watson Transcription Workflow Operation
=============================================

ID: `start-watson-transcription`

Description
-----------

The start watson transcription operation invokes the IBM Watson Speech-to-Text service,
passing an audio file to be translated to text.

Parameter Table
---------------

|configuration keys|description|default value|example|
|------------------|-------|-----------|-------------|
|source-flavor|The flavor of the audio file to be sent for translation.|EMPTY|presenter/delivery|
|source-tag|The flavor of the audio file to be sent for translation.|EMPTY|transcript-audio|
|skip-if-flavor-exists|If this flavor already exists in the media package, skip this operation.<br/>To be used when the media package already has a transcript file.|false|captions/vtt+en|

**One of source-flavor or source-tag must be specified.**

Example
-------

```yaml
  # Extract audio from video in ogg/opus format
  - id: encode
    fail-on-error: true
    exception-handler-workflow: partial-error
    description: Extract audio for transcript generation
    configurations:
      - source-tags: engage-download
      - target-flavor: audio/ogg
      - target-tags: transcript
      - encoding-profile: audio-opus
      # If there is more than one file that match the source-tags, use only the first one
      - process-first-match-only: true

  # Start IBM Watson recognitions job
  - id: start-watson-transcription
    fail-on-error: true
    exception-handler-workflow: partial-error
    description: Start IBM Watson transcription job
    configurations:
      # Skip this operation if flavor already exists. Used for cases when mp already has captions.
      - skip-if-flavor-exists: captions/vtt+en
      # Audio to be translated, produced in the previous compose operation
      - source-tag: transcript
```

### Encoding profile used in example above

```properties
profile.audio-opus.name = audio-opus
profile.audio-opus.input = stream
profile.audio-opus.output = audio
profile.audio-opus.suffix = -audio.opus
profile.audio-opus.ffmpeg.command = -i /#{in.video.path} -c:a libvorbis -ac 1 -ar 16k -b:a 64k #{out.dir}/#{out.name}#{out.suffix}
```
