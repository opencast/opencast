AmberScript Attach Transcription Workflow Operation
===================================================

ID: `amberscript-attach-transcription`


Description
-----------

The AmberScript Attach Transcription operation attaches the result file received from the transcription service
to the media package.


Parameter Table
---------------

| configuration keys    | required | description                                                                        | default               | example                                      |
|-----------------------|----------|------------------------------------------------------------------------------------|-----------------------|----------------------------------------------|
| transcription-job-id  | yes      | This is filled out by the transcription service when starting the workflow.        | -                     | **Should always be "${transcriptionJobId}"** |
| target-flavor         | yes      | The flavor to apply to the captions/transcriptions file.                           | -                     | captions/source                              |
| target-tag            | no       | A tag to apply to the caption/transcription file generated¹                        | -                     | generator-type:auto                          |
| target-tags           | no       | Comma separated list of tags to apply to generated file¹. Overwrites `target-tag`  | -                     | generator-type:auto,generator:amberscript    |
| target-caption-format | no       | The caption format to be generated.                                                | vtt                   | srt                                          |
| target-element-type   | no       | Define where to append the subtitles file. Accepted values: 'track', 'attachment'. | track                 | track                                        |

¹⁾For conventionally used tags see the general page on [Subtitles](../configuration/subtitles.md).