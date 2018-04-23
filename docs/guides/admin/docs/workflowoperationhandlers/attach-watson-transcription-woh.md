Attach Watson Transcription
===========================

Description
-----------

The Attach Watson Transcription converts the results file received from the IBM Watson Speech-to-Text service in json
format, converts it to the desired caption format, and adds it to the media package.


Parameter Table
---------------

|configuration keys   |description|default value|example|
|---------------------|-------|-----------|-------------|
|transcription-job-id |This is filled out by the transcription service when starting the workflow.|EMPTY|**Should always be "${transcriptionJobId}"**|
|target-flavor        |The flavor of the caption/transcription file generated. Mandatory only if target-caption-format not informed.|captions/`target-caption-format`+`language`|captions/vtt+en|
|target-tag           |The tag to apply to the caption/transcription file generated. Optional.|EMPTY|archive|
|target-caption-format|The caption format to be generated. Optional. If not entered, the raw resulting file will be attached to the media package with the flavor `target-flavor`.|EMPTY|vtt|


Example
-------

```xml
<!-- Attach caption/transcript -->
<operation id="attach-watson-transcription"
  fail-on-error="true"
  exception-handler-workflow="partial-error"
  description="Attach captions/transcription">
  <configurations>
    <!-- This is filled out by the transcription service when starting this workflow so just use this as is -->
    <configuration key="transcription-job-id">${transcriptionJobId}</configuration>
    <configuration key="target-tag">archive</configuration>
    <!-- Caption generated will have the default flavor based on the target-caption-format and language e.g. captions/vtt+en -->
    <configuration key="target-caption-format">vtt</configuration>
    <configuration key="target-tag">engage-download</configuration>
  </configurations>
</operation>

<!-- Merge caption/transcript to existing publication and republish -->
<operation id="publish-engage"
  fail-on-error="true"
  exception-handler-workflow="partial-error"
  description="Distribute and publish to engage server">
  <configurations>
    <configuration key="download-source-tags">engage-download</configuration>
    <configuration key="strategy">merge</configuration>
    <configuration key="check-availability">true</configuration>
  </configurations>
</operation>
```
