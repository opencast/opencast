Google Speech Attach Transcription Workflow Operation
=============================================================

ID: `google-speech-attach-transcription`


Description
-----------

Google Speech Attach Transcription converts the json format file received from the Google Speech-to-Text service 
into the desired caption format and adds it to the media package.


Parameter Table
---------------

|configuration keys    |required |description                                                                                                                 |default value|example|
|----------------------|---------|----------------------------------------------------------------------------------------------------------------------------|-------------|-------------|
|transcription-job-id  |yes      |This is filled out by the transcription service when starting the workflow.                                                 |EMPTY        |**Should always be "${transcriptionJobId}"**|
|line-size             |no       |The line size (number of characters) of the transcripts to display at a time.                                               |EMPTY        |100|
|target-flavor         |yes      |The flavor of the caption/transcription file generated.                                                                     |EMPTY        |captions/source|
|target-tag            |no       |The tag to apply to the caption/transcription file generated.*                                                              |EMPTY        |archive,generator-type:auto   |
|target-caption-format |no       |The caption format to be generated. If not entered, the raw resulting file will be attached to the media package.           |EMPTY        |vtt|
|target-element-type   |no       |Define where to append the subtitles file. Accepted values: 'track', 'attachment'.                                          |track        |track                                        |

*For conventionally used tags see the general page on [Subtitles](../configuration/subtitles.md).

Example
-------

```xml
<!-- Attach caption/transcript -->
<operation id="google-speech-attach-transcription"
    description="Attach captions/transcription">
  <configurations>
    <!-- This is filled out by the transcription service when starting this workflow -->
    <configuration key="transcription-job-id">${transcriptionJobId}</configuration>
    <configuration key="line-size">100</configuration>
    <configuration key="target-flavor">captions/source</configuration>
    <configuration key="target-tag">archive,generator-type:auto</configuration>
    <configuration key="target-caption-format">vtt</configuration>
  </configurations>
</operation>

<!-- Publish to engage player -->
<operation id="publish-engage"
    description="Distribute and publish to engage server">
  <configurations>
    <configuration key="download-source-flavors">dublincore/*,security/*,captions/*</configuration>
    <configuration key="strategy">merge</configuration>
    <configuration key="check-availability">false</configuration>
  </configurations>
</operation>
```
