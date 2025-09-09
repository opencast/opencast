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

| configuration keys     | required | Example          | description                                                                                                    |
|------------------------|----------|------------------|----------------------------------------------------------------------------------------------------------------|
| subtitle-source-flavor | yes      | captions/source  | Flavor of the subtitle file(s) that shall be shifted.                                                          |
| video-source-flavors   | yes      | branding/bumper  | Flavors of the videos that will be used to determine the duration for the shifting of the subtitle timestamps. |
| target-flavor          | yes      | captions/shifted | Flavor of the subtitle file(s) that will be created including the shifted timestamps                           |


Requirements
------------
The operation will sum over all durations of all tracks in `video-source-flavors`. Please make sure the specified
flavors only contain tracks by which duration you want the subtitles shifted.


Additional Notes
------------
The operation will simply skip if no subtitle file is found with the given `subtitle-source-flavor`.

On successful completion, the operation will create a new subtitle file in the process and 
will not override the old one. The tags of the original subtitle file will be copied to the new subtitle file.


Operation Examples
------------------

```yaml
  - id: subtitle-timeshift
    description: Create new subtitle file with shifted timestamps
    configurations:
      - subtitle-source-flavor: captions/source
      - target-flavor: captions/shifted
      - video-source-flavor: branding/bumper
```
