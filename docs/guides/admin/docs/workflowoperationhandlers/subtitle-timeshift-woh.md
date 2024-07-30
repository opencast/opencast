Subtitle Timeshift Workflow Operation
=====================================

ID: `subtitle-timeshift`

Description
-----------

The subtitle timeshift operation can be used to offset the timestamps of WebVTT subtitle files. One use case regards 
bumper/intro videos. For example: If a bumper/intro video gets added in front of an already subtitled presenter track,
the subtitles would start too early. With this operation, you can select a video and a subtitle track and the timestamps
of the captions file will be shifted backwards by the duration of the selected video. This ensures that the subtitles
match the presenter track as intended.


Parameter Table
---------------

| configuration keys     | required | Example          | description                                                                                                  |
|------------------------|----------|------------------|--------------------------------------------------------------------------------------------------------------|
| subtitle-source-flavor | yes      | captions/source  | Flavor of the subtitle file(s) that shall be shifted.                                                        |
| video-source-flavor    | yes      | branding/bumper  | Flavor of the video that will be used to determine the duration for the shifting of the subtitle timestamps. |
| target-flavor          | yes      | captions/shifted | Flavor of the subtitle file(s) that will be created including the shifted timestamps                         |


Requirements
------------
Please select a flavor for `video-source-flavor` that will select only a single/unique video. If multiple videos are 
selected, or (in other words) if it's not clear which video file the operation has to use, the operation will fail.


Additional Notes
------------
The operation will simply skip if no subtitle file is found with the given `subtitle-source-flavor`.

On successful completion, the operation will create a new subtitle file in the process and 
will not override the old one. The tags of the original subtitle file will be copied to the new subtitle file.


Operation Examples
------------------

```XML
<operation
    id="subtitle-timeshift"
    description="Create new subtitle file with shifted timestamps">
  <configurations>
    <configuration key="subtitle-source-flavor">captions/source</configuration>
    <configuration key="target-flavor">captions/shifted</configuration>
    <configuration key="video-source-flavor">branding/bumper</configuration>
  </configurations>
</operation>
```
