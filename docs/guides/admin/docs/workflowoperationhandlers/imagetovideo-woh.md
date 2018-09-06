# ImageToVideo Workflow Operation Handler

## Description

The ImageToVideo Workflow Operation Handler allows to create a video track from a source image.

## Parameters table

Tags and flavors can be used in combination. But combined they should match one image.

|configuration keys |example                   |description|default value|
|-------------------|--------------------------|-----------|-------------|
|**source-tags**\*  |intro                     |A comma separated list of tags of the input image|EMPTY|
|**source-flavor**\*|intro/source              |The "flavor" of the image to use as a source input|EMPTY|
|target-tags        |composite,rss,atom,archive|The tags to apply to the output video track|EMPTY|
|target-flavor      |intro/work                |The flavor to apply to the output video track|EMPTY|
|**duration**\*     |5                         |The length of the output video in seconds.|EMPTY|
|**profile**\*      |image-movie               |Define the encoding-profile to use to create the output video. See example of profile below.|EMPTY|

\* **mandatory**

## Operation example

```xml
<operation
  id="image-to-video"
  fail-on-error="true"
  exception-handler-workflow="error"
  description="Composite">
  <configurations>
    <configuration key="source-tags">intro</configuration>
    <configuration key="source-flavor">intro/source</configuration>
    <configuration key="target-tags">intro-video</configuration>
    <configuration key="target-flavor">intro/video</configuration>
    <configuration key="duration">10</configuration>
    <configuration key="profile">image-movie</configuration>
  </configurations>
</operation>
```

## Encoding profile example

    # Image to video
    profile.image-movie.name = image to video
    profile.image-movie.input = image
    profile.image-movie.output = visual
    profile.image-movie.suffix = -image-video.mp4
    profile.image-movie.ffmpeg.command = -loop 1 -i #{in.video.path} -c:v libx264 -r 25 -t #{time} -pix_fmt yuv420p #{out.dir}/#{out.name}#{out.suffix}
