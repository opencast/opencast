Crop Workflow Operation
=======================

ID: `crop`


Description
-----------

The plugin provides the workflow operation `crop-video`. This workflow operation executes FFmpeg command `cropdetect`.
`cropdetect` checks for black bars on the sides of the track of the workflow instance. If `cropdetect` is successful,
then FFmpeg command `crop` is executed. `crop` removes these black bars.


Parameter Table
---------------

| configuration keys    | example          | description
| :-------------        | :----------------| :-------------
| source-flavor         | \*/source        | which media should be encoded
| target-tags           | sometag          | Specifies the tags of the new media
| target-flavor         | presenter/cropped| Flavor of the cropped media track


Example
-------

```xml
<operation
    id="crop-video"
    description="Detecting black bars in presentation track">
  <configurations>
    <configuration key="source-flavor">*/source</configuration>
    <configuration key="target-tags">engage-download</configuration>
  </configurations>
</operation>
```
