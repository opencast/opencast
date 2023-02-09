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

```xml
<operation
        id="webvtt-to-cutmarks"
    description="Use WebVTT as a silence detection"
    fail-on-error="true"
    exception-handler-workflow="partial-error">
  <configurations>
    <configuration key="source-flavor">captions/vtt+en</configuration>
    <configuration key="target-flavor">cut-marks/json</configuration>
    <configuration key="min-time-silence-in-ms">5000</configuration>
    <configuration key="buffer-time-around-subtitle">1500</configuration>
    <configuration key="track-flavor">presenter/source</configuration>
    <configuration key="start-treatment">USE_FOR_MIN_TIME</configuration>
    <configuration key="end-treatment">USE_FOR_MIN_TIME</configuration>
  </configurations>
</operation>
```
