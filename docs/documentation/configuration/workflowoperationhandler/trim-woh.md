# TrimWorkflowOperationHandler

## Description
The TrimWorkflowOperationHandler makes it possible to remove the undesired parts of the media at the beginning and the end of the recordings.

This operation UI also allows users to select/deselect tracks for being further processed and distributed (e.g. one could remove the presenter track if its quality does not meet the required standards). The recording metadata fields (e.g. title, presenter, series, etc.) may be also be edited in the UI provided.

## Parameter Table

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|duration-threshold	|"100"	|If the trimming "out point" is beyond a certain track's duration, this parameter specifies the maximum allowed difference between them (in milliseconds).|0|
|encoding-profile	|"trim.master"	|The encoding profile used to encode the trimmed tracks.	|EMPTY|
|source-flavors |"presentation/trimmed" |Indicates the flavor(s) that will be trimmed by this operation..|EMPTY|
|target-flavor-subtype	|"master"	|The flavors of the elements created after the trim will be modified by changing the second half of the flavor with the value of this parameter. E.g., if it is set to "trimmed", a source track's flavor "presenter/work" would become "presenter/trimmed".|EMPTY|

### Duration Threshold Tag
The "duration-threshold" parameter  accepts a threshold value in milliseconds. It is meant to deal with length differences between the tracks in a mediapackage (which in theory should have the same length). Since all the tracks in the mediapackage are trimmed at the same time points, the trimming point may be within a certain track's duration, but outside another. If the difference between the trim point and the track length is shorter than the threshold, then the outpoint is adjusted to the length of the track, for that track only. For instance, if the trim point is at 5'31'' but one of the tracks is 5'30'' long, with a threshold of 2000 (2 seconds), the shorter track will be trimmed to 5'30 instead, thus not failing. In the end, when some tracks are longer and others slightly shorter than the trim point, you will end up with a set of tracks that are trimmed either at the exact point or not shorter than <threshold> milliseconds

The parameter is currently in the default workflow, but commented. The threshold is 0 by default, i.e. no difference between the trim outpoint and the track length is allowed.

 
## Capture UI

![Trim UI](MatterhornTrimOperation.png)

## Operation Example

    <operation
     id="trim"
     retry-strategy="hold"
     fail-on-error="true"
     exception-handler-workflow="error"
     description="trimming and master generation">
      <configurations>
        <configuration key="duration-threshold">1000</configuration>
        <configuration key="source-flavor">*/work</configuration>
        <configuration key="target-flavor-subtype">master</configuration>
        <configuration key="encoding-profile">trim.master</configuration>
      </configurations>
    </operation>
