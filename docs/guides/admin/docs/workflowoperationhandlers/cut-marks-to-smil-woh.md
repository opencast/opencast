Cut Marks to Smil Operation
===========================

ID: `cut-marks-to-smil`

Description
-----------

This operation parses a JSON containing cut marks into a SMIL that can be used by the [Video Editor](editor-woh.md).
It does this by attributing the given times to the specified tracks.

Tracks are assumed to start at 0.
Likewise, cut marks are assumed to be specified relative to the beginning of the tracks.


Parameter Table
---------------

|Configuration Keys    |Example              |Description                                                    |
|----------------------|---------------------|---------------------------------------------------------------|
|source-media-flavors  |`presenter/prepared` |The flavors containing the video tracks.                       |
|source-json-flavor    |`cut-marks/json`     |The flavor of the JSON. Must contain exactly one file.         |
|target-smil-flavor    |`smil/cutmarks`      |The flavor of the resulting SMIL.                              |
|target-tags           |`archive`            |(Optional) Tags to add to the resulting SMIL. Default is `null`|


JSON Format
-----------

The JSON structure specifies all segments which should be kept after cutting.
The property `begin` marks the start of a segment while `duration` its duration.
The values are specified in milliseconds.

```json
[
  {
    "begin": 1672,
    "duration": 7199
  }
]
```

Operation Example
-----------------

```xml
<operation
    id="cut-marks-to-smil"
    description="Process ingested cut marks by applying them to current tracks"
    fail-on-error="true"
    exception-handler-workflow="partial-error">
  <configurations>
    <configuration key="source-media-flavors">presenter/prepared,presentation/prepared</configuration>
    <configuration key="source-json-flavor">cut-marks/json</configuration>
    <configuration key="target-smil-flavor">smil/cutting</configuration>
  </configurations>
</operation>
```
