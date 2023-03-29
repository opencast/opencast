AmberScript Attach Transcription Workflow Operation
===================================================

ID: `amberscript-attach-transcription`


Description
-----------

The AmberScript Attach Transcription operation attaches the result file received from the transcription service
to the media package.


Parameter Table
---------------

| configuration keys    | required | description                                                                 | default               | example                                      |
|-----------------------|----------|-----------------------------------------------------------------------------|-----------------------|----------------------------------------------|
| transcription-job-id  | yes      | This is filled out by the transcription service when starting the workflow. | ${transcriptionJobId} | **Should always be "${transcriptionJobId}"** |
| target-flavor         | yes      | The flavor to apply to the captions/transcriptions file.                    | -                     | captions/source                              |
| target-tag            | no       | The tag to apply to the caption/transcription file generated.*              | -                     | generator-type:auto                          |
| target-caption-format | no       | The caption format to be generated.                                         | vtt                   | srt                                          |

*For conventionally used tags see the general page on [Subtitles](../../modules/subtitles).