# Concat Workflow Operation Handler


## Overview


The "concat" operation handler has been created to concatenate multiple video tracks into one video track.
There are two modes: General and Same Codec.

### General - When there is no restriction on source tracks codecs
The general process re-encode the videos first to the same format (framerate/timebase/codec, etc) before concatenation.

![Concat](Concat.png)

The internal ffmpeg command for re-encoding is using the following filters: fps, scale, pad and setdar for scaling all videos to a similar size including letterboxing, aevalsrc for creating silent audio streams and of course the concat for the actual
concatenation step.

This requires an output-resolution and an optional output-framerate for the pre-concatenation encode.

####FFmpeg Filter Example


    -filter_complex '
      [0:v]fps=fps=25.0,scale=iw*min(640/iw\,480/ih):ih*min(640/iw\,480/ih),pad=640:480:(ow-iw)/2:(oh-ih)/2,setdar=4:3[b];
      [1:v]fps=fps=25.0,scale=iw*min(640/iw\,480/ih):ih*min(640/iw\,480/ih),pad=640:480:(ow-iw)/2:(oh-ih)/2,setdar=4:3[c];
      [2:v]fps=fps=25.0,scale=iw*min(640/iw\,480/ih):ih*min(640/iw\,480/ih),pad=640:480:(ow-iw)/2:(oh-ih)/2,setdar=4:3[d];
      aevalsrc=0::d=1[silent];
      [b][0:a][c][silent][d][2:a]concat=n=3:v=1:a=1[v][a]' -map '[v]' -map '[a]'



### Same Codec - When the source tracks share the same av codecs (same timebase/resolution/encoding, etc)

The sources files can be arranged into one container losslessly without re-encoding first.
This is often the case if the tracks came from the same camera/recorder for example,

This mode uses the __ffmpeg concat demuxer__, which puts all the video content into a single container without any re-encoding. The encoding profile then operates on the source in this container. If "-c copy" is used in the encoding profile, then the concatenation is lossless.

The ffmpeg command is:
        -f concat -safe 0 -i videolist.txt

where videolist.txt contains a line in the form "file <path to video>" for each source track.

The switch to use this mode is `same-codec`


Usage
-----

This operation is quite similar to the compose operation. The only difference is that the input properties are not only
limited to one `source-flavor` and `source-tag`. The operation supports multiple flavor and tags as input.  To add
multiple source, add different key with the prefix `source-flavor-`/`source-tag-` and an incremental number starting
with 0. For example:

 - `source-flavor-part-0`
 - `source-flavor-part-1`
 - `source-flavor-part-..`


 Aternatively, the source flavor can support an undetermined number of ordered inport files by using `source-flavor-numbered-files` as input tracks. This option cannot be used in combination with `source-flavor-part-X` or `source-tag-part-X`.
 This is used when the number of input files cannot be known in advance, such as chunked output files from some camera/recorders, and the names are ordered by number or timestamps and to be sorted lexicographically.
 For example, the flavor can be

 - `source-flavor-numbered-files`: multipart/part+source

 and the ordered input tracks are:
     * video-201711201020.mp4
     * video-201711201030.mp4
     * video-201711201040.mp4



Configuration Keys
------------------

|Key                             |Required|Description                                            |Default|Example|
|--------------------------------|--------|-------------------------------------------------------|-------|-------|
|`source-flavor-part-X`          |false   |An iterative list of part/flavor to use as input track.|`NULL` |`presenter/trimmed`|
|`source-tag-part-X`             |false   |An iterative list of part/tag to use as input track.   |`NULL` |`source-to-concate`|
|`source-flavor-part-X-mandatory`|false   |Define the flavor part-X as optional for concatenation.|`false`|`true`|
|`source-tag-part-X-mandatory`   |false   |Define the tag part-X as optional for concatenation.   |`false`|`true`|
|`encoding-profile`              |true    |Encoding profile to use for the concatenation.         |`NULL` |`concat`|
|`target-flavor`                 |true    |Flavor(s) to add to the output track.                  |`NULL` |`presenter/concat`|
|`target-tags`                   |false   |Tag(s) to add to the output track                      |`NULL` |`engage-download`|
|`output-resolution`             |true    |Output resolution in width, height or a source part    |`NULL` |`1900x1080`, `part-1`|
|`output-framerate`              |false   |Output frame rate in frames per second or a source part|`-1.0` |`25`, `23.976`, `part-1`|
|`source-flavor-numbered-files`  |false   |Files of this flavor are ordered lexicographically to use as input track.  |`NULL` |`multipart/sections`|
|`same-codec`                    |false   |All source files have identical formats.               |`false` |`true`|



Example
-------

Example of a concat operation in a workflow definition.

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
        <configuration key="output-framerate">part-1</configuration>
      </configurations>
    </operation>

Example of a lossless concat operation for videos with identical formats in a workflow definition.

    <!-- Concatenate chunked video from camera -->
    <operation
      id="concat"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Concatenate the generated videos.">
      <configurations>
        <configuration key="source-flavor-numbered-files">multipart/chunkedsource</configuration>
        <configuration key="target-flavor">presenter/concat</configuration>
        <configuration key="target-tags">engage-download,engage-streaming</configuration>
        <!-- do not encode before concatenation -->
        <configuration key="same-codec">true</configuration>
        <configuration key="encoding-profile">1920x1080</configuration>
      </configurations>
    </operation>


Encoding Profile
----------------

The encoding profile command must contain the the #{concatCommand} parameter.

    profile.concat.name = concat
    profile.concat.input = visual
    profile.concat.output = visual
    profile.concat.suffix = -concatenated.mp4
    profile.concat.mimetype = video/mp4
    profile.concat.ffmpeg.command = #{concatCommand} \
      -c:a aac -b:a 128k \
      -c:v mpeg4 -b:v 1200k -flags +aic+mv4 \
      #{out.dir}/#{out.name}#{out.suffix}

