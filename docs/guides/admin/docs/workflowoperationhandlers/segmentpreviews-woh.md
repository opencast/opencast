Segment Previews Workflow Operation
===================================

ID: `segmentpreviews`

Description
-----------

The segment previews operation will extract still images from a video using FFmpeg, a given encoding profile and
previous discovered segments.

Parameter Table
---------------

| configuration keys | example           | description                                                                                |
|--------------------|-------------------|--------------------------------------------------------------------------------------------|
| source-flavor      | presenter/source  | Specifies which media should be processed. At least one source flavor or tag is required.  |
| target-flavor      | presenter/work    | Specifies the flavor the new files will get.                                               |
| source-tags        | engage            | Specifies which media should be processed.  At least one source flavor or tag is required. |
| target-tags        | engage            | Specifies the tags the new files will get.                                                 |
| encoding-profile   | search-cover.http | The encoding profile to use.                                                               |
| reference-flavor   | presentation/work | Flavor of the segments to use.                                                             |
| reference-tags     | engage            | Tags of the segments to use.                                                               |

Operation Example
-----------------

```yaml
  - id: segmentpreviews
    description: Encoding presentation (screen) to segment preview image
    configurations:
      - source-flavor: presentation/trimmed
      - source-tags: ''
      - target-flavor: presentation/segment+preview
      - reference-flavor: presentation/delivery
      - reference-tags: engage
      - target-tags: engage
      - encoding-profile: player-slides.http
```
