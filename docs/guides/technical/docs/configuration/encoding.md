Encoding Profile Configuration
==============================

A workflow defines which operations are applied to media ingested into Matterhorn and the order of these operations. An
operation can be something general like “encode this video”. The encoding profiles then specify exactly how a media is
ancoded, which filters are applied, which codecs are used and in which container these will be stored, …

Matterhorn comes with a set of such profiles generating files for both online playback and download. These profiles are
build to work for everyone, meaning that in most cases optimization can be done according to local needs. So modifying
these profiles or building new ones often makes sense. This document will help you modify or augment Matterhorn's
default encoding profiles for audio, video and still images.

Default Profiles and Possible Settings
--------------------------------------

This section contains some notes about the default profiles, explaining some thoughts behind those profiles and pointing
at things you might want to change depending on your local set-up.

### A/V-Muxing: From lossless to safe

The audio/video muxing (profile.mux-av.work) is applied if audio and video are send to Matterhorn separately. The basic
idea behind this is, to combine these separate files into one file which can later be converted in one step.

Possible settings:

 - If you get an audio and a video file separately, it is possivle to just copy the streams and put them together into a
   new file. This is very fast (you only have to copy the streams) and most importantly, it is lossless, as no
   re-encoding is done. The question is: What a/v container format can/should you use for such an operation.
 - You can try to use the video container the input video came in and just add the audio. This means that you will never
   have an unexpected video container you don't know of. I.e. if you put an .mp4 video in, it still uses and .mp4
   container after musing, etc. This might, however, lead to problems if you throw in an audio file that cannot be muxen
   in the specific container format (i.e. you have a FLAC audio file and an FLV container). This is, what Matterhorn
   does at the moment.
 - To circumvent the container problem, we could also use a container format which can hold almost everything (i.e. mkv)
   regardless of the input. This would mean that MH can handle more combinations of a/v streams but you will always end
   up with a Matroska file after muxing. Of cause you can then encode it to mp4, etc. later on.

The safest option for muxing is, of cause, to always re-encode the streams. It is far slower. It always means a quality
loss. But it can handle everything FFmpeg can handle which (dependin on your FFmpeg configuration) is quite a lot.


### Trimming: Fast and lossless VS accurate

The profile trim.work is used after you send your recording to trim/hold and selected new start and end points. Here you
have basically two choices:

 - As you only want to cut the video, you don't have to re-encode all of it. You just split the stream and put it into a
   new file. This is fast. This is lossless. But depending on the video format this might not be accurate. You can only
   cut a stream at an I-frame (a frame holding the full image). Doing this, FFmpeg will cut the video at the nearest
   I-frame before the selected position meaning that you might end up with a bit more video than you thought you would
   get. You can minimize this by making sure that the input video has a high I-frame frequency (probably a good idea
   anyway). This is what Matterhorn does at the moment.
 - The alternative is to cut between two I-frames. This is possible, but requires the video to be re-encoded completely.
   This means that you will spend a lot of time on this process and will always loose quality.


Create an Encoding Profile
--------------------------

This section will help you to understand how you can modify an existing profile or create a completly new one.

Creating a new encoding profile is a matter of creating a configuration file and placing it in the encoding profiles
watch folder.

### Encoding Profile Folder

The `<config_dir>/encoding` folder allows you to quickly augment Matterhorn's existing behavior, simply by modifying or
adding new configuration files. The file names should follow the pattern `*.properties`.


###The Encoding Profile

Encoding profiles consist of a set of key-value pairs that conform to the following pattern:

    profile.<name>.<context>.<property> = <value>

For example:

    profile.flash.http.name = flash download

All profiles should have the following properties:

    .name
    .input  = [audio|visual|stream|image]
    .output = [audio|visual|stream|image]
    .suffix
    .mimetype
    .ffmpeg.command

For example:

    // My audio/video encoding profile
    profile.my-av-profile.http.name           = my audio/video encoding profile
    profile.my-av-profile.http.input          = visual
    profile.my-av-profile.http.output         = visual
    profile.my-av-profile.http.suffix         = -encoded.enc
    profile.my-av-profile.http.mimetype       = video/x-enc
    profile.my-av-profile.http.ffmpeg.command = -i #{in.video.path} -c:v videoencoder -c:a audioencoder #{out.dir}/#{out.name}#{out.suffix}

The most important part of this profile is the `ffmpeg.command`. This line specifies FFmpeg command line options using
`#{expression}` for string replacement.


### FFmpeg

To create a new profile you have basically one task to do: Find an appropriate FFmpeg command line for whatever you want
to do. For more information about FFmpeg, its options and how you can build FFmpeg with additional functionality have a
look at the [Official FFmpeg Wiki](http://trac.ffmpeg.org/wiki). For trying out new encoding settings, just call FFmpeg
from the command line.


Using a Profile
---------------

Once defined, use your encoding profile in your workflow by setting the encoding-profile property to the profiles name:

    <operation
        id="compose"
        fail-on-error="true"
        exception-handler-workflow="error"
        description="Encode presenter using my audio/video encoding profile">
      <configurations>
        <configuration key="source-flavor">presenter/work</configuration>
        <configuration key="target-flavor">presenter/delivery</configuration>
        <configuration key="target-tags">rss, atom, captioning</configuration>
        <configuration key="encoding-profile">my-av-profile.http</configuration>
      </configuration>
    </operation>

Have a look at the Workflow Configuration section for more details about this.
