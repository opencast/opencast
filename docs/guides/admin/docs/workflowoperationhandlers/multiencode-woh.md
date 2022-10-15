Multi-encode Workflow Operation
=======================================

ID: `multiencode`


Description
-----------

The MultiencodeWorkflowHandler is used to encode source media into multiple formats concurrently.
The source recording are selected by source-flavors AND source-tags.
Each source media selector (eg presenter or presentation) can have an independent set of encoding profile ids
(one for each target recording) and target tags.
Encoding of each source medium runs as one FFmpeg command.
This operation will generate one multiencode operation per source medium,
all of them running concurrently on the same or on different workers.
In addition, the target media can be optionally tagged with the encoding profile ids.

### Configuration Details

This workflow handles each source selector independently as a section.
The parameters for each configuration, such as flavors are separated positionally into sections by "**;**".
The use of the semi-colon is optional. If it is absent, there is only one section.


```xml
<configuration key="source-flavors">*/source</configuration>
```

> One source selector means that all the matching recording will be processed the same way.

```xml
<configuration key="source-flavors">presenter/source;presentation/source</configuration>
```

> Two different source selectors means that all the matching recordings in the first selector will be processed
> according to the parameters in the first section and the all the matching recordings in the second selector will
> be processed according to the parameters in next section.

Each source selector can have only one corresponding section.
If there is only one section in one parameter, eg: target-flavors,
but multiple sections in another, eg: source-flavors,
then the sections are collapsed into one.
For example:

```xml
<configuration key="target-flavors">*/preview</configuration>
```

> All targets are flavored the same way, using the example above, becomes "presenter/preview"
> and "presentation/preview"

Each source selector can have its own set of target tags and flavors, defined as a comma delimited list.
For example:

```xml
<configuration key="target-tags">engage-streaming,rss,atom;engage-download,rss,atom</configuration>
```

> Using the example above.
> "presenter/preview" is tagged with "engage-streaming,rss,atom".
> "presentation/preview" is tagged with "engage-download,rss,atom".

When a configuration has the same number of sections as the source, then the configurations for the operation
are taken from the corresponding sections.

Each section runs independently as a parallel encoding job.

For example, if presenter/source is to encoded with "mp4-low.http,mp4-medium.http" and
presentation/source is to be encoded with "mp4-hd.http,mp4-hd.http"

The target flavors are presenter/delivery and presentation/delivery and all are tagged "rss, archive".
The target flavors are additionally tagged with encoding profiles, so that they can selected individually.

This workflow supports HLS adaptive streaming.
By:

1. Using only H.264/HENV encodings in the list of encoding profiles. 
2. In the encoding profile, use the `-<option>:<a or v>` form in FFmpeg options when appropriate
   (eg: "-b:a" instead of "-ab"),
   and add the suffix ":v" for options that apply to video and ":a" to options that apply to audio,
   (eg: -maxrate:v, -g:v )
3. Use the same keyframe intervals (`-keyint <int>` and `-keyint_min <int>`) in each profile and 
   segment size (see below) should be a multiple of this integer.
4. Adding a special encoding profile "multiencode-hls" to the list of encoding profiles. By default,
   the segments size is 6s (-hls-time).

HLS Playlists are generated as part of the encoding process. Each mp4 is a fragmented MP4.
A variant playlist is created for each mp4 and a master playlist is used to access all the different qualities.

To make sure that stream switching works as expected, state the bitrates explicitly for each of mp4 encoding profiles used.
For advices on how to pick bitrates see:
https://developer.apple.com/documentation/http_live_streaming/hls_authoring_specification_for_apple_devices

For more details on HLS, see:
https://tools.ietf.org/html/rfc8216
https://tools.ietf.org/html/draft-pantos-http-live-streaming-23

Without HLS, it will look like the following.

Parameter Table
---------------

|configuration keys | example                     | description                                                         |
|-------------------|-----------------------------|---------------------------------------------------------------------|
|source-flavors     | presenter/source*;*presentation/source  | Which media should be encoded                               |
|target-flavors     | \*/preview                | Specifies the flavor of the new media                               |
|target-tags        | rss,archive              | Specifies the tags of the new media                                 |
|encoding-profiles  | mp4-low.http,mp4-medium.http*;*mp4-hd.http,mp4-hd.http | Encoding profiles for each source flavor |
|tag-with-profile   | true (default to false)  | target medium are tagged with corresponding encoding profile Id      |



## Operation Example
```xml
<operation
    id="multiencode"
    description="Encode to delivery formats, with different encoding settings for each video source">
  <configurations>
    <configuration key="source-flavors">presenter/work;presentation/work</configuration>
    <configuration key="target-flavors">*/delivery</configuration>
    <configuration key="target-tags">rss,archive</configuration>
    <configuration key="encoding-profiles">
        hls-full-res-presenter-mp4,
        hls-half-res-presenter-mp4,
        hls-quarter-15fps-presenter-mp4,
        multiencode-hls
    </configuration>
    <configuration key="tag-with-profile">true</configuration>
  </configurations>
</operation>
```

On subsequent operations that run on the encoded files (e.g. `image`,`segment-video`, `segmentpreviews`,
`timelinepreviews`, `extract-text`), you will **have to** specify on which encoding the operations run on, otherwise
they will fail. This is done by adding a `source-tags` key to each operation like so:

```xml
<configuration key="source-tags">hls-full-res-presenter-mp4</configuration>
```

Encoding Profile
----------------

The encoding profiles necessary for this operation are not included in Opencast per default, but the operation will not
work without them.  Depending on which encoding profiles you want to use in your operation, you will need to include
some or all of the following encoding profiles in your Opencast. Copy and pasting them in a new
`HLS-streaming-movies.properties` file in the `etc/encoding` folder of your installation.

<details>
<summary>Click me to see the encoding profiles</summary>

```properties
###
# Profile definitions for serverless HLS using multiencode
# Presenter and presentation can have different resolutions
# These profiles assumes 30 fps and and uses a keyframe interval of 2s
# to support 4 or 6s segments.
#
# -force_key_frames:v makes sure that there is at least one kf every 2 seconds
# There may be other keyframes, you can use no-scenecuts to remove them

# In production, use libfdk_aac instead of aac in your ffmpeg build
# We notice a audio delay with aac in some decoder (firefox)
# It will require building ffmpeg with libfdk library
#
# NOTE:# Do not use more than one -vf clause in each profile
#
##

## HLS
# TODO: When ffmpeg is fixed - add profile:v baseline
# A very low bitrate stream with 15 fps to support mobile - (under 192kb/s)
profile.hls-quarter-15fps-presenter-mp4.name = hls-quarter-15fps-presenter-mp4
profile.hls-quarter-15fps-presenter-mp4.input = visual
profile.hls-quarter-15fps-presenter-mp4.output = visual
profile.hls-quarter-15fps-presenter-mp4.suffix = -presenter-mobile-15fps.mp4
profile.hls-quarter-15fps-presenter-mp4.mimetype = video/mp4
profile.hls-quarter-15fps-presenter-mp4.ffmpeg.command = -i #{in.video.path} -r:v 15 \
 -force_key_frames:v expr:eq(mod(n,30),0) \
 -x264opts:v rc-lookahead=30:keyint=60:min-keyint=30:no-open-gop=1 \
 -preset:v veryfast -movflags +faststart \
 -c:v libx264 -c:a aac \
 -vf scale=min(320\\,trunc(iw/8)*2):-2 \
 -b:v 100k -minrate:v 90k -maxrate:v 110k -bufsize:v 100k \
 -b:a 48k #{out.dir}/#{out.name}#{out.suffix}
profile.hls-quarter-15fps-presenter-mp4.jobload=4.0

# maximum of width = 320 or 1/8 original width, keeping aspect ratio
profile.hls-quarter-res-presenter-mp4.name = hls-quarter-res-presenter-mp4
profile.hls-quarter-res-presenter-mp4.input = visual
profile.hls-quarter-res-presenter-mp4.output = visual
profile.hls-quarter-res-presenter-mp4.suffix = -presenter-verylow-30fps.mp4
profile.hls-quarter-res-presenter-mp4.mimetype = video/mp4
profile.hls-quarter-res-presenter-mp4.ffmpeg.command = -i #{in.video.path} -r:v 30 \
 -force_key_frames:v expr:eq(mod(n,60),0) \
 -x264opts:v rc-lookahead=60:keyint=120:min-keyint=60:no-open-gop=1 \
 -preset:v veryfast -movflags +faststart \
 -c:v libx264 -c:a aac \
 -vf scale=min(320\\,trunc(iw/8)*2):-2 \
 -b:v 200k -maxrate:v 220k -bufsize:v 200k \
 -b:a 48k #{out.dir}/#{out.name}#{out.suffix}
profile.hls-quarter-res-presenter-mp4.jobload=4.0
# -vf scale=min(320\\,trunc(iw/8)*2:-2) \

# 16x9 or 4X3 presenter (camera) - scale to max of w=640 or half - preserves aspect ratio
profile.hls-half-res-presenter-mp4.name = hls-half-res-presenter-mp4
profile.hls-half-res-presenter-mp4.input = visual
profile.hls-half-res-presenter-mp4.output = visual
profile.hls-half-res-presenter-mp4.suffix = -presenter-low-30fps.mp4
profile.hls-half-res-presenter-mp4.mimetype = video/mp4
profile.hls-half-res-presenter-mp4.ffmpeg.command = -i #{in.video.path} -r:v 30 \
 -force_key_frames:v expr:eq(mod(n,60),0) \
 -x264opts:v rc-lookahead=60:keyint=120:min-keyint=60:no-open-gop=1 \
 -preset:v veryfast -movflags +faststart \
 -c:v libx264 -c:a aac \
 -vf scale=min(640\\,trunc(iw*0.25)*2):-2 \
 -b:v 1200k -maxrate:v 1320k -bufsize:v 1M  \
 -b:a 64k #{out.dir}/#{out.name}#{out.suffix}
profile.hls-half-res-presenter-mp4.jobload=4.0

# 16x9 or 4X3 presenter (screen) and presenter (camera) - scale to max of 960 or three quarters - preserves original aspect ratio
profile.hls-threequarters-res-presenter-mp4.name = hls-threequarters-res-presenter-mp4
profile.hls-threequarters-res-presenter-mp4.input = visual
profile.hls-threequarters-res-presenter-mp4.output = visual
profile.hls-threequarters-res-presenter-mp4.suffix = -presenter-med-30fps.mp4
profile.hls-threequarters-res-presenter-mp4.mimetype = video/mp4
profile.hls-threequarters-res-presenter-mp4.ffmpeg.command = -i #{in.video.path} -r:v 30 \
 -force_key_frames:v expr:eq(mod(n,60),0) \
 -x264opts:v rc-lookahead=60:keyint=120:min-keyint=60:no-open-gop=1 \
 -preset:v veryfast -movflags +faststart \
 -c:v libx264 -c:a aac \
 -vf scale=min(960\\,trunc(iw*0.375)*2):-2 \
 -b:v 2500k -maxrate:v 2700k -bufsize:v 2M \
 -b:a 96k #{out.dir}/#{out.name}#{out.suffix}
profile.hls-threequarters-res-presenter-mp4.jobload=4.0

# -bf:v 3 -b_strategy:v 2 -refs:v 5 \
# scale to 1920
profile.hls-full-res-presenter-mp4.name = hls-full-res-presenter-mp4
profile.hls-full-res-presenter-mp4.input = visual
profile.hls-full-res-presenter-mp4.output = visual
profile.hls-full-res-presenter-mp4.suffix = -presenter-high-30fps.mp4
profile.hls-full-res-presenter-mp4.mimetype = video/mp4
profile.hls-full-res-presenter-mp4.ffmpeg.command = -i #{in.video.path} -r:v 30 \
 -force_key_frames:v expr:eq(mod(n,60),0) \
 -x264opts:v rc-lookahead=60:keyint=120:min-keyint=60:no-open-gop=1 \
 -preset:v veryfast -movflags +faststart \
 -vf scale=min(1280\\,trunc(iw/2)*2):-2 \
 -pix_fmt:v yuv420p -c:v libx264 -c:a aac \
 -b:v 4500k -maxrate:v 4900k  -bufsize:v 4M  \
 -b:a 128k #{out.dir}/#{out.name}#{out.suffix}
profile.hls-full-res-presenter-mp4.jobload=4.0


# scale to w=1/4 original, maximum of 480, keeping aspect ratio, 15 fps at 2s keyframe
profile.hls-quarter-15fps-presentation-mp4.name = hls-quarter-15fps-presentation-mp4
profile.hls-quarter-15fps-presentation-mp4.input = visual
profile.hls-quarter-15fps-presentation-mp4.output = visual
profile.hls-quarter-15fps-presentation-mp4.suffix = -presentation-mobile-15fps.mp4
profile.hls-quarter-15fps-presentation-mp4.mimetype = video/mp4
profile.hls-quarter-15fps-presentation-mp4.ffmpeg.command = -i #{in.video.path} -r:v 15 \
 -force_key_frames:v expr:eq(mod(n,15),0) \
 -x264opts:v rc-lookahead=30:keyint=60:min-keyint=30:no-open-gop=1 \
 -preset:v veryfast -movflags +faststart \
 -c:v libx264 -c:a aac \
 -vf scale=min(480\\,trunc(iw/8)*2):-2 \
 -b:v 100k -minrate:v 90k -maxrate:v 110k -bufsize:v 100k \
 -b:a 48k #{out.dir}/#{out.name}#{out.suffix}
profile.hls-quarter-15fps-presentation-mp4.jobload=4.0

# scale to w=1/4 original, maximum of 480, keeping aspect ratio
profile.hls-quarter-res-presentation-mp4.name = hls-quarter-res-presentation-mp4
profile.hls-quarter-res-presentation-mp4.input = visual
profile.hls-quarter-res-presentation-mp4.output = visual
profile.hls-quarter-res-presentation-mp4.suffix = -presentation-verylow-30fps.mp4
profile.hls-quarter-res-presentation-mp4.mimetype = video/mp4
profile.hls-quarter-res-presentation-mp4.ffmpeg.command = -i #{in.video.path} -r:v 30 \
 -force_key_frames:v expr:eq(mod(n,60),0) \
 -x264opts:v rc-lookahead=60:keyint=120:min-keyint=60:no-open-gop=1 \
 -preset:v veryfast -movflags +faststart \
 -pix_fmt:v yuv420p -c:v libx264 -c:a aac \
 -vf scale=min(480\\,trunc(iw/8)*2):-2 \
 -b:v 250k -maxrate:v 275k -bufsize:v 250k  \
 -b:a 48k #{out.dir}/#{out.name}#{out.suffix}
profile.hls-quarter-res-presentation-mp4.jobload=4.0

# 16x9 or 4X3 presentation (screen) (camera) - scale to half - preserves original aspect ratio
profile.hls-half-res-presentation-mp4.name = hls-half-res-presentation-mp4
profile.hls-half-res-presentation-mp4.input = visual
profile.hls-half-res-presentation-mp4.output = visual
profile.hls-half-res-presentation-mp4.suffix = -presentation-low-30fps.mp4
profile.hls-half-res-presentation-mp4.mimetype = video/mp4
profile.hls-half-res-presentation-mp4.ffmpeg.command = -i #{in.video.path} -r:v 30 \
 -force_key_frames:v expr:eq(mod(n,60),0) \
 -x264opts:v rc-lookahead=60:keyint=120:min-keyint=60:no-open-gop=1 \
 -pix_fmt:v yuv420p -c:v libx264 -c:a aac \
 -preset:v veryfast -movflags +faststart \
 -vf scale=min(960\\,trunc(iw*0.25)*2):-2 \
 -b:v 1M -maxrate:v 1100k -bufsize:v 1M \
 -b:a 64k #{out.dir}/#{out.name}#{out.suffix}
profile.hls-half-res-presentation-mp4.jobload=4.0

# 16x9 or 4X3 presentation (screen) (camera) - scale to threee quarters - preserves original aspect ratio
profile.hls-threequarters-res-presentation-mp4.name = hls-threequarters-res-presentation-mp4
profile.hls-threequarters-res-presentation-mp4.input = visual
profile.hls-threequarters-res-presentation-mp4.output = visual
profile.hls-threequarters-res-presentation-mp4.suffix = -presentation-med-30fps.mp4
profile.hls-threequarters-res-presentation-mp4.mimetype = video/mp4
profile.hls-threequarters-res-presentation-mp4.ffmpeg.command = -i #{in.video.path} -r:v 30 \
 -force_key_frames:v expr:eq(mod(n,60),0) \
 -x264opts:v rc-lookahead=60:keyint=120:min-keyint=60:no-open-gop=1 \
 -pix_fmt:v yuv420p -c:v libx264 -c:a aac \
 -preset:v veryfast -movflags +faststart \
 -vf scale=min(1440\\,trunc(iw*0.375)*2):-2 \
 -b:v 2M -maxrate:v 2200k -bufsize:v 2M \
 -b:a 96k #{out.dir}/#{out.name}#{out.suffix}
profile.hls-threequarters-res-presentation-mp4.jobload=4.0
# -vf scale=trunc(iw*0.375)*2:-2 \

# 16x9 or 4X3 presentation (screen) - no scaling - preserves original aspect ratio and size
profile.hls-full-res-presentation-mp4.name = hls-full-res-presentation-mp4
profile.hls-full-res-presentation-mp4.input = visual
profile.hls-full-res-presentation-mp4.output = visual
profile.hls-full-res-presentation-mp4.suffix = -presentation-high-30fps.mp4
profile.hls-full-res-presentation-mp4.mimetype = video/mp4
profile.hls-full-res-presentation-mp4.ffmpeg.command = -i #{in.video.path} -r:v 30 \
 -force_key_frames:v expr:eq(mod(n,60),0) \
 -x264opts:v rc-lookahead=60:keyint=120:min-keyint=60:no-open-gop=1 \
 -preset:v veryfast -movflags +faststart \
 -vf scale=min(1920\\,trunc(iw/2)*2):-2 \
 -pix_fmt:v yuv420p -c:v libx264 -c:a aac \
 -b:v 4M -maxrate:v 4400k -bufsize:v 4M \
 -b:a 128k #{out.dir}/#{out.name}#{out.suffix}
profile.hls-full-res-presentation-mp4.jobload=4.0


# HLS Group profile with 6 second segments - use with multiencode/process-smil to add HLS adaptive streaming
# multiencode only support HLS currently
# adaptive type to state that it is only used in a group to supplement encoding
# HLS Group profile - use with multiencode/process-smil to add HLS adaptive streaming
# process will map each stream and generate a master manifest and one for each delivery format
# if bitrates are missing in any of the video encoding profiles (highest bitrate first)
# See https://developer.apple.com/documentation/http_live_streaming/hls_authoring_specification_for_apple_devices
# See https://gist.github.com/tayvano/6e2d456a9897f55025e25035478a3a50 for ffmpeg options

profile.multiencode-hls.name = multiencode-hls
# adaptive type - only used in a group to supplement encoding only HLS is supported
profile.multiencode-hls.adaptive.type = HLS
profile.multiencode-hls.input = visual
# manifest type means that it is a supplement
profile.multiencode-hls.output = manifest
# Only master playlist has the suffix and can be tagged with profile name in process-smil or multi-encode
profile.multiencode-hls.suffix = -master.m3u8
profile.multiencode-hls.mimetype = application/x-mpegURL
#-forced-idr 1 \
profile.multiencode-hls.ffmpeg.command = -hls_time 6 \
-strict strict \
-flags +global_header+cgop -movflags +faststart \
-err_detect compliant \
-copyts -muxdelay 0 \
-pix_fmt yuv420p -ac 2 \
-hls_segment_type fmp4 -f hls -hls_list_size 0 -hls_playlist_type vod \
-hls_flags single_file+program_date_time+independent_segments+round_durations \
-hls_segment_filename #{out.dir}/segment_%v.mp4 \
-master_pl_name #{out.name}#{out.suffix} #{out.dir}/variant_%v.m3u8
profile.multiencode-hls.jobload=1.0
#-hls_flags single_file+discont_start+program_date_time+independent_segments+round_durations \

# 4 second segments - no muxdelay or 2 channel audio
profile.multiencode-hls-4s.name = multiencode-hls-4s
# adaptive type - only used in a group to supplement encoding only HLS is supported
profile.multiencode-hls-4s.adaptive.type = HLS
profile.multiencode-hls-4s.input = visual
# manifest type means that it is a supplement
profile.multiencode-hls-4s.output = manifest
# Only master playlist has the suffix and can be tagged with profile name in process-smil or multi-encode
profile.multiencode-hls-4s.suffix = -master.m3u8
profile.multiencode-hls-4s.mimetype = application/x-mpegURL
profile.multiencode-hls-4s.ffmpeg.command = -flags +global_header+cgop -hls_time 4 \
-hls_segment_type fmp4 -f hls -hls_list_size 0 -hls_playlist_type vod \
-hls_flags single_file+discont_start+program_date_time+independent_segments+round_durations \
-hls_segment_filename #{out.dir}/segment_%v.mp4 \
-master_pl_name #{out.name}#{out.suffix} #{out.dir}/variant_%v.m3u8
profile.multiencode-hls-4s.jobload=1.0
```

</details>


Note (Important)
----------------

Each source flavor generates all the target formats in one FFmpeg call by incorporating relevant parts
of the encoding profile commands.

* Care must be taken that no FFmpeg complex filters are used in the encoding profiles used for this workflow,
  as it can cause a conflict.

* Encoded target recording are distinguished by the suffix, it is important that all the encoding profiles used have
  distinct suffixes to use "tag-with-profile" configuration, for example:

```properties
profile.mp4-vga-medium.http.suffix = -vga-medium.mp4
profile.mp4-medium.http.suffix = -medium.mp4
```
