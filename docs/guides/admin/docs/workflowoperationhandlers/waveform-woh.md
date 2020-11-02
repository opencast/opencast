WaveformWorkflowOperationHandler
================================

Description
-----------

The waveform operation creates an image showing the temporal audio activity within the recording like this:

![waveform](waveform.png)

The implementation uses an FFmpeg filter that produces a waveform PNG image file from an audio/video file with at least
one audio channel.


Parameter Table
---------------

configuration     |example     |description                                                     |default
------------------|------------|----------------------------------------------------------------|-------
source-flavors    |`*/audio`   |Flavor specifying tracks for which a waveform should be created |n/a
source-tags       |`edit`      |Tags specifying tracks for which a waveform should be created   |n/a
target-flavor     |`*/waveform`|Flavor used for the generated waveform                          |n/a
target-tags       |`preview`   |Comma-separated list of tags to be added to the waveform        |n/a
pixels-per-minute |400         |Width of waveform image in pixels per minute                    |200
min-width         |10000       |Minimum width of waveform image in pixels                       |5000
max-width         |30000       |Maximum width of waveform image in pixels                       |20000
height            |60          |Height of waveform image in pixels                              |500
color             |black       |Color of waveform image, see [ffmpeg.org/ffmpeg-all.html#Color](https://www.ffmpeg.org/ffmpeg-all.html#Color) |black
Additional notes:

- All media, that match either source-flavors or source tags will be processed.
- Using a wildcard in the `target-flavor` will cause the main flavor of the input being used.


Operation Example
-----------------

    <operation
      id="waveform"
      description="Generating waveform">
      <configurations>
        <configuration key="source-flavor">*/audio</configuration>
        <configuration key="target-flavor">*/waveform</configuration>
        <configuration key="target-tags">preview</configuration>
        <configuration key="pixels-per-minute">200</configuration>
        <configuration key="min-width">5000</configuration>
        <configuration key="max-width">20000</configuration>
        <configuration key="height">60</configuration>
        <configuration key="color">black</configuration>
      </configurations>
    </operation>
