ProbeResolutionWorkflowOperationHandler
=======================================


Description
-----------

The ProbeResolutionWorkflowOperationHandler analyzes specified tracks in the mediapackage and sets workflow instance
variables based on the video resolution and the mapping set-up.


Parameter Table
---------------

|Configuration Key|Example             |Description                                       |
|-----------------|--------------------|--------------------------------------------------|
|source-flavor\*  |`presentation/work` |The "flavor" of the track to use as a source input|
|var:VARNAME      |`1280x720,1920x1080`|Resolutions to variable mapping                   |
|val:VARNAME      |`16/9`              |Value to set if resolution matches                |

\* mandatory configuration key

There can be an arbitrary number of variable parameters. They must be prefixed by `var:`, followed by the variable name
to set to true if the video has a resolution listed. The `var:` prefix will not be part of the resulting variable name
but will be replaced with a representation of the tracks flavor.

By default, the variable will be set to `true` if the resolution matches. If a `val:VARNAME` configuration is present
which matches a `var:VARNAME`, the value from that configuration key will be used instead.

Note that if there are multiple video streams with one flavor, only the information from the last video stream are
taken.


Operation Example
-----------------

    <operation
      id="probe-resolution"
      fail-on-error="true"
      exception-handler-workflow="ng-partial-error"
      description="Set control variables based on video resolution">
      <configurations>
        <configuration key="source-flavor">*/source</configuration>
        <configuration key="var:aspect">1280x720,1920x1080,2592x1080</configuration>
        <configuration key="val:aspect">16/9</configuration>
        <configuration key="var:is_720">1280x720</configuration>
        <configuration key="var:is_1080">1920x1080,2592x1080</configuration>
      </configurations>
    </operation>

If a video track with a resolution of 1280x720 is passed to this operation as `presentation/source`, the resulting
variables would be:

    presentation_source_is_720=true
    presentation_source_aspect=16/9
