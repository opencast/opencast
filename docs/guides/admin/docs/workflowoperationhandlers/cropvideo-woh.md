# Cropping Service

## Workflow operation

The plugin provides the workflow operation `crop-video`. This workflow operation excutes ffmpeg command "cropdetect". 
`cropdetect` checks for black bars on the sides of the track of the workflow instance. If `corptetect` is successfully,
then ffmpeg command `crop` is executed. `crop` removes these black bars.

### Example for crop-video in a workflow

```xml
    <operation
      id="crop-video"
      fail-on-error="false"
      description="Detecting slide transitions in presentation track">
      <configurations>
        <configuration key="source-flavor">*/source</configuration>
        <configuration key="target-tags">engage-download</configuration>
      </configurations>
    </operation>

```
