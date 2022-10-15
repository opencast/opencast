AmberScript Attach Transcription Workflow Operation
===================================================

ID: `amberscript-attach-transcription`


Description
-----------

The AmberScript Attach Transcription operation attaches the result file received from the transcription service
to the media package.


Parameter Table
---------------

| configuration keys    | description                                                                 | default               | example                                      |
|-----------------------|-----------------------------------------------------------------------------|-----------------------|----------------------------------------------|
| transcription-job-id  | This is filled out by the transcription service when starting the workflow. | ${transcriptionJobId} | **Should always be "${transcriptionJobId}"** |
| target-flavor         | The flavor to apply to the captions/transcriptions file. Optional.          | captions/srt          | captions/vtt+en                              |
| target-tag            | The tag to apply to the caption/transcription file generated. Optional.     | -                     | engage-download                              |
| target-caption-format | The caption format to be generated. Optional.                               | srt                   | vtt                                          |

Note: If you set the language property in
`org.opencastproject.transcription.amberscript.AmberscriptTranscriptionService.cfg` the target flavor will be appended
by +<language> (e.g. `captions/srt+de`).
