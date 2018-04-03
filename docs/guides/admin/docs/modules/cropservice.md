# Cropping Service
## Installation and Configuration
1. You need a built version of Opencast that you intend to use.
2. Build the plugin. Therefore you need the exact version of your Opencast (`-Dopencast.version`) and the Dopencast
main directory (`-DdeployTo`). If you provide an unused directory for the deploy, you will get all files you need in there.
`mvn clean install -Dopencast.version=4.0 -DdeployTo=/opt/opencast`
3. Restart your Opencast server

## Workflow operation
The plugin provides the workflow operation `crop-video`. This workflow operation excutes ffmpeg command "cropdetect". `cropdetect`
checks for black bars on the sides of the track of the workflow instance. If `corptetect` is successfully, then ffmpeg command `crop` is
executed. `crop´ removes these black bars.
### Example for crop-video in a workflow

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definition xmlns="http://workflow.opencastproject.org">

  <id>ng-crop</id>
  <title>cropping</title>
  <tags/>
  <description/>

  <configuration_panel/>

  <operations>

 <operation
      id="crop-video"
      fail-on-error="false"
      description="Detecting slide transitions in presentation track">
      <configurations>
        <configuration key="source-flavor">*/source</configuration>
        <configuration key="target-tags">engage-download</configuration>
      </configurations>
    </operation>

    <!-- Archive the current state of the media package -->

    <operation
      id="snapshot"
      fail-on-error="true"
      exception-handler-workflow="ng-partial-error"
      description="Archiving">
      <configurations>
        <configuration key="source-tags">archive</configuration>
      </configurations>
    </operation>

</operations>
</definition>
```
