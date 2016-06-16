# Concat workflow operation handler

** FFMPEG 1.1 is required for the encoding profile related to this operation! **

## Overview
The "concat" operation handler has been created to concatenate multiple video tracks into one video track. The concatenation process uses the ffmpeg scale filter which is always re-encoding the videos, this means the resulting video has most likely a loss of quality.

![Concat](workflowoperationhandlers/Concat.png)

The internal ffmpeg command is using the following filters:  scale, pad and setdar for scaling all videos to a similar size including letterboxing, aevalsrc for creating silent audio streams and of course the concat for the actual concatenation step. More info can be found here: https://trac.ffmpeg.org/wiki/FilteringGuide
### Sample complex concat filter command
    -filter_complex "[0:v]scale=iw*min(640/iw\,480/ih):ih*min(640/iw\,480/ih),pad=640:480:(ow-iw)/2:(oh-ih)/2,setdar=4:3[b];[1:v]scale=iw*min(640/iw\,480/ih):ih*min(640/iw\,480/ih),pad=640:480:(ow-iw)/2:(oh-ih)/2,setdar=4:3[c];[2:v]scale=iw*min(640/iw\,480/ih):ih*min(640/iw\,480/ih),pad=640:480:(ow-iw)/2:(oh-ih)/2,setdar=4:3[d];aevalsrc=0::d=1[silent];[b][0:a][c][silent][d][2:a]concat=n=3:v=1:a=1[v][a]" -map '[v]' -map '[a]'

##Usage
This operation is quite similar to the compose operation. The only difference is that the input properties are not only limited to one "source-flavor" and "source-tag". The operation supports multiple flavor and tags as input.  To add multiple source, add different key with the prefix "source-flavor-"/"source-tag-" and an incremental number starting with 0. For example:

 - source-flavor-0
 - source-flavor-1
 - source-flavor-..

## Configuration keys

|Key|Required|Description|Default Value|Example|
|---|--------|-----------|-------------|-------|
|source-flavor-part-X|false|An iterative list of part/flavor to use as input track.|NULL|
|presenter/trimmed|source-tag-part-X|false|An iterative list of part/tag to use as input track.|NULL|
|source-to-concate|source-flavor-part-X-mandatory|false	|Define the flavor part-X as an optional track for concatenation.|false	|true|
|source-tag-part-X-mandatory|false	|Define the tag part-X as an optional track for concatenation.|false |true|
|encoding-profile|**true**|Define the encoding-profile to use for the concatenation operation. See example of profile below.|NULL|concat|
|target-flavor|**true**|Define the flavor(s) to add to the output track. |NULL|presenter/concat|
|target-tags|false|Define the tag(s) to add to the output track|NULL|engage-download|
|output-resolution|**true**|Define the output resolution in width, height or take it from one of the given parts|NULL|1900x1080, part-1|

##Example
Example of an concat operation in a workflow definition.

    <!-- Add intro and outro part to the presenter track -->
    <operation
      id="concat"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Concatenate the presenter track and the intro/outro videos.">
      <configurations>
        <configuration key="source-flavor-part-0">intro/source</configuration>
        <configuration key="source-flavor-part-1">presenter/trimmed</configuration>
        <configuration key="source-flavor-part-1-mandatory">true</configuration>
        <configuration key="source-flavor-part-2">outro/source</configuration>
        <configuration key="target-flavor">presenter/concat</configuration>
        <configuration key="target-tags">engage-download,engage-streaming</configuration>
        <configuration key="encoding-profile">concat</configuration>
        <configuration key="output-resolution">1920x1080</configuration>
      </configurations>
    </operation>

##Encoding profile
The encoding profile command must contain the the #{concatCommand} parameter.

    # Concat
    profile.concat.name = concat
    profile.concat.input = visual
    profile.concat.output = visual
    profile.concat.suffix = -concatenated.mp4
    profile.concat.mimetype = video/mp4
    profile.concat.ffmpeg.command = #{concatCommand} -acodec libfaac -b:a 128k -vcodec mpeg4 -b:v 1200k
    -flags +aic+mv4 #{out.dir}/#{out.name}#{out.suffix}
