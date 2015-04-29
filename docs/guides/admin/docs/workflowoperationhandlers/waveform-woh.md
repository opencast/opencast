## WaveformWorkflowOperationHandler

## Description
The waveform operation creates an image showing the temporal audio activity within the recording. This is be done with a probably well known waveform (see example image).
![waveform](workflowoperationhandlers/waveform.png)
The operation does not need an additional module, as it is not very work intensive to create such an image. The operation needs and audio-only file to create the image and it provides an PNG image.

## Parameter Table

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|source-flavors|"*/audio"|Input parameter is the source-flavor of the audio files for which a waveform should be created. The *-operator can be used if the waveform should be created for all flavors with a certain subtypes (like "audio" in our example).|EMPTY|
|target-flavor	|"*/waveform"	|The output-parameter is target-flavor which should use the *-operator if it was used in the source-flavor too.|EMPTY|

## Operation Example

    <operation
      id="waveform"
      if="${trimHold}"
      fail-on-error="false"
      description="Generating waveform">
      <configurations>
        <configuration key="source-flavor">*/audio</configuration>
        <configuration key="target-flavor">*/waveform</configuration>
      </configurations>
    </operation>
