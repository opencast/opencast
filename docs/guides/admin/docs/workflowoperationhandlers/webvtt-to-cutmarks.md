WebVTT to CutMarks Operation
===========================

ID: `webvtt-to-cutmarks`

Description
-----------

This operation parses a WebVTT Subtitle File and generates a JSON of CutMarks based on the Subtitle Timestamps.
It's primary intention is to be used as a silence detection based on available subtitle information.

With the CutMarks to SMIL workflow operation this JSON can be converted to a SMIL that
can be used in the [Video Editor](editor-woh.md) for cutting.


Parameter Table
---------------
Tracks are assumed to start at 0.

| Configuration Keys            | Default value     | Example/Other Possible values       | Description                                                                                                                                         |
|-------------------------------|-------------------|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `source-flavor`               | None, must be set | `captions/vtt+en`                   | The Flavor of the WebVTT Subtitle File to parse, used for creating the CutMarks.                                                                                                          |
| `target-flavor`               | None, must be set | `cut-marks/json`                    | The Flavor of the output JSON, which contains the CutMarks based on the subtitle information.                                                                                             |
| `min-time-silence-in-ms`      | `0`               | `5000`                              | Time (in ms) between two subtitles for them to be considered seperate cutting sections, otherwise they are merged. Basically the minimum length of a silent section for it to be cut out. |
| `buffer-time-around-subtitle` | `0`               | `1500`                              | How much buffer time (in ms) to add before and after a subtitle/non-silent section.                                                                                                       |
| `track-flavor`                | Optional/Empty    | `presenter/source`                  | The flavor of the track related to the WebVTT, used for determining the end time of the video.                                                                                            |
| `start-treatment`             | `IGNORE`          | `USE_FOR_MIN_TIME`/`ALWAYS_INCLUDE` | How to treat the beginning of the video in relation to the subtitle sections.                                                                                                             |
| `end-treatment`               | `IGNORE`          | `USE_FOR_MIN_TIME`/`ALWAYS_INCLUDE` | How to treat the end of the video in relation to the subtitle sections. (If not `IGNORE`, Needs `track-flavor` to be set)                                                                 |


Operation Example
-----------------

```yaml
  - id: webvtt-to-cutmarks
    description: Use WebVTT as a silence detection
    configurations:
      - source-flavor: captions/vtt+en
      - target-flavor: cut-marks/json
      - min-time-silence-in-ms: 5000
      - buffer-time-around-subtitle: 1500
      - track-flavor: presenter/source
      - start-treatment: USE_FOR_MIN_TIME
      - end-treatment: USE_FOR_MIN_TIME
```
