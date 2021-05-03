# AmberScript Attach Transcription

## Description

AmberScript Attach Transcription workflow operation attaches the result file received from the Transcription Service
to the media package.

## Parameter Table

| configuration keys    | description                                                                 | default               | example                                      |
|-----------------------|-----------------------------------------------------------------------------|-----------------------|----------------------------------------------|
| transcription-job-id  | This is filled out by the transcription service when starting the workflow. | ${transcriptionJobId} | **Should always be "${transcriptionJobId}"** |
| target-flavor         | The flavor to apply to the captions/transcriptions file. Optional.          | captions/vtt          | captions/vtt+en                              |
| target-tag            | The tag to apply to the caption/transcription file generated. Optional.     | -                     | engage-download                              |
| target-caption-format | The caption format to be generated.                                         | vtt                   | srt                                          |
