Mux Workflow Operation
=======================================

ID: `mux`


Description
-----------

The MuxWorkflowOprationHandler is used to combine multiple tracks into one. Some use-cases may be

- add additional audio streams
- integrate subtitle tracks
- apply organisational theme
- etc.

The encoding profile defines what should happen with each stream. Streams can be named to pick them up in the encoding profile specifically.


Parameter Table
---------------

| configuration keys      | example                           | description                                                                                                          |
|-------------------------|-----------------------------------|----------------------------------------------------------------------------------------------------------------------|
| source-flavors          | presenter/work, presentation/work | Which media should be used as input. In the encoding profile the inputs are named `video`¹                           |
| source-tags             | engage-player                     | Comma-separated list of tags of media to encode                                                                      |
| source-flavors-\<name\> | captions/work                     | Flavor of input tracks with specific `name`¹. This name can be used in the encoding profile to pick the right stream |
| source-tags-\<name\>    | lang:en, lang:de                  | Comma-separated list of tags of named media to encode.                                                               |
| target-flavor           | download/delivery                 | Flavor of the new media                                                                                              |
| target-tags             | download                          | Comma-separated list of tags to be assigned to the new media                                                         |
| encoding-profile        | mux.http                          | Encoding profile to use                                                                                              |
¹ Several tracks may potentially match, so the name will be suffixed with `.<index>`, where index begins with 1.

In some cases, encoding profiles require a specific order of input streams. One way to achieve this is by naming the inputs. For example, the first stream must be video, and all subsequent streams must be subtitles. To ensure this, the subtitle inputs are named differently. Multiple values with the same name are numbered consecutively and have the suffix `.<index>` (begins with 1).

Working with dynamic inputs may require to adjust ffmpeg command based on the inputs count. You will be able to do this with ffmpeg command extensions that will be automatically enabled. The extensions pattern are

| extension name                | example              | description                                                        |
|-------------------------------|----------------------|--------------------------------------------------------------------|
| if-input-count-eq-\<number\>  | if-input-count-eq-3  | On exact \<number\> input tracks (`eq` means equals)               |
| if-input-count-geq-\<number\> | if-input-count-geq-3 | On \<number\> or more input tracks (`geq` means greater or equals) |

See the example below for instructions on how to use the ffmpeg command extensions.

For tracks, having `lang:<locale>` tag set, special encoding parameter variable `#{in.<name>.language}` will be set with the locale value as ISO 639 (3 characters code).

### Operation Example
The captions should be included as separate streams for each language for the `download/video` track. Captions are available in three languages.
The workflow operation may look like

```xml
<operation
    id="mux"
    description="Mux captions with video">
  <configurations>
    <configuration key="source-flavor">download/video</configuration>
    <!-- Captions language information are set as tags on the tracks as documented here: -->
    <!-- https://docs.opencast.org/r/15.x/admin/#configuration/subtitles/#tags -->
    <configuration key="source-flavors-captions">captions/delivery</configuration>
    <configuration key="target-flavor">download/delivery</configuration>
    <configuration key="target-tags">download</configuration>
    <configuration key="encoding-profile">mp4-mux-captions.http</configuration>
  </configurations>
</operation>
```

Since there are four input tracks — the video itself and three caption tracks — the encoding profile option `if-input-count-eq-4` will be enabled. The video track (here, we assume it's only one) will be named `video` because it's the default name for unnamed inputs. The caption tracks are named `captions` and numbered beginning with 1. The encoding profile input variable for the first caption track is `#{in.captions.1.path}`, the second is `#{in.captions.2.path}`, and so on. The language values are set as variable `#{in.<name>.language}`. In our case `#{in.captions.1.language}`, `#{in.captions.2.language}`, and so on. 

```properties
profile.mp4-mux-captions.http.name = Mux multiple captions (up to three) to the media track
profile.mp4-mux-captions.http.input = visual
profile.mp4-mux-captions.http.output = visual
profile.mp4-mux-captions.http.jobload = 1
profile.mp4-mux-captions.http.suffix = .mp4
profile.mp4-mux-captions.http.ffmpeg.command = \
  -i #{in.video.1.path} \
  #{if-input-count-eq-2} \
  #{if-input-count-eq-3} \
  #{if-input-count-eq-4} \
  #{out.dir}/#{out.name}#{out.suffix}
profile.mp4-mux-captions.http.ffmpeg.command.if-input-count-eq-2 = \
  -i #{in.captions.1.path} \
  -c:v copy -c:a copy -c:s mov_text \
  -map 0:v -map 0:a -map 1:s -metadata:s:s:0 language=#{in.captions.1.language}
profile.mp4-mux-captions.http.ffmpeg.command.if-input-count-eq-3 = \
  -i #{in.captions.1.path} \
  -i #{in.captions.2.path} \
  -c:v copy -c:a copy -c:s mov_text \
  -map 0:v -map 0:a \
  -map 1:s -metadata:s:s:0 language=#{in.captions.1.language} \
  -map 2:s -metadata:s:s:1 language=#{in.captions.2.language}
profile.mp4-mux-captions.http.ffmpeg.command.if-input-count-eq-4 = \
  -i #{in.captions.1.path} \
  -i #{in.captions.2.path} \
  -i #{in.captions.3.path} \
  -c:v copy -c:a copy -c:s mov_text \
  -map 0:v -map 0:a \
  -map 1:s -metadata:s:s:0 language=#{in.captions.1.language} \
  -map 2:s -metadata:s:s:1 language=#{in.captions.2.language} \
  -map 3:s -metadata:s:s:2 language=#{in.captions.3.language}
```