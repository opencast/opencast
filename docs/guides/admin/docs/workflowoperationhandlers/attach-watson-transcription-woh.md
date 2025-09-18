Attach Watson Transcription Workflow Operation
==============================================

ID: `attach-watson-transcription`


Description
-----------

The attach Watson transcription operation converts the results file received from the IBM Watson Speech-to-Text service
in json format, converts it to the desired caption format, and adds it to the media package.


Parameter Table
---------------

|configuration keys   | required | description                                                                        | default               | example |
|---------------------|----------|------------------------------------------------------------------------------------|-----------------------|-------------|
|transcription-job-id | yes      | This is filled out by the transcription service when starting the workflow.        | ${transcriptionJobId} |**Should always be "${transcriptionJobId}"**|
|target-flavor        | yes      | The flavor of the generated caption/transcription file.                            | EMPTY                 | captions/source |
|target-tag           | no       | The tag to apply to the caption/transcription file generated.*                     | EMPTY                 | generator-type:auto   |
|target-caption-format| no       | The caption format to be generated.                                                | vtt                   | vtt |
|target-element-type  | no       | Define where to append the subtitles file. Accepted values: 'track', 'attachment'. | track                 | track                                        |

*For conventionally used tags see the general page on [Subtitles](../configuration/subtitles.md).

Example
-------

```yaml
  # Attach caption/transcript
  - id: attach-watson-transcription
    description: Attach captions/transcription
    configurations:
      # This is filled out by the transcription service when starting this workflow so just use this as is
      - transcription-job-id: ${transcriptionJobId}
      - target-flavor: captions/source
      - target-tag: engage-download,lang:en,generator-type:auto
      - target-caption-format: vtt

  # Merge caption/transcript to existing publication and republish
  - id: publish-engage
    description: Distribute and publish to engage server
    configurations:
      - download-source-tags: engage-download
      - strategy: merge
      - check-availability: true
```
