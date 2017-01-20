## WaveformWorkflowOperationHandler

## Description
The waveform operation creates an image showing the temporal audio activity within the recording.
This is be done with a probably well known waveform (see example image).
![waveform](waveform.png)
The implementation uses an ffmpeg filter that produces a waveform PNG image file from an audio/video file
with at least one audio channel.

## Parameter Table

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|source-flavors |*/audio    |Input parameter is the source-flavor of the media files for which a waveform should be created. The *-operator can be used if the waveform should be created for all flavors with certain subtypes (like "audio" in our example).|EMPTY|
|source-tags    |edit, waveform |Input parameter is the source-tag of the media files for which a waveform should be created. All media, that match source-flavors or source tags will be processed.|EMPTY|
|target-flavor	|*/waveform	|The output-parameter is target-flavor which should use the *-operator if it was used in the source-flavor too.|EMPTY|
|target-tags	  |preview	  |The output-parameter is a comma-separated list of tags that will be added to the waveform image media package attachment. |EMPTY|

## Operation Example

    <operation
      id="waveform"
      if="${trimHold}"
      fail-on-error="false"
      description="Generating waveform">
      <configurations>
        <configuration key="source-flavor">*/audio</configuration>
        <configuration key="target-flavor">*/waveform</configuration>
        <configuration key="target-tags">preview</configuration>
      </configurations>
    </operation>
