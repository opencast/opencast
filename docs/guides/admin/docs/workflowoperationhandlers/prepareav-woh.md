# PrepareAVWorkflowOperation

## Description
The PrepareAVWorkflowOperation works is like this: 

If there are two tracks with the same flavor, and one of them contains a video stream only, while the other contains an audio stream only, the implementation will call the composer's "mux" method, with the result that the audio will be muxed with the video, using the video's movie container. 

If it there is one track with a certain flavor, the "encode" method is called which will rewrite (vs. encode) the file using the same container and codec (-vcodec copy, -a codec copy), while the container format is determined by ffmpeg via the file's extension. The reason for doing this is that many media files are in a poor state with regard to their compatibility (most often, the stream's codec contains differing information from the container), so we are basically asking ffmepg to rewrite the whole thing, which will in many cases eliminate problems that would otherwhise occur later in the pipeline (encoding to flash, mjpeg etc.). 

## Parameter Table

|configuration keys|example|description|
|------------------|-------|-----------|
|source-flavor|presenter/source|Specifies which media should be processed.|
|target-flavor|presenter/work|Specifies the flavor the new files will get.|
|mux-encoding-profile	|mux-av.work	|The encoding profile to use for media that needs to be muxed (default is 'mux-av.work')|
|audio-video-encoding-profile	|av.work	|The encoding profile to use for media that is audio-video already and needs to be re-encodend (default is av.work)	 |
|video-encoding-profile	|video-only.work	|The encoding profile to use for media that is only video and needs to be re-encodend (default is video-only.work)	 |
|audio-encoding-profile	|audio-only.work	|The encoding profile to use for media that is only audio and needs to be re-encodend (default is audio-only.work)	 |
|rewrite	|true	|Should files be rewritten	 |
|promiscuous-audio-muxing	|true	|If there is no matching flavor to mux, try other flavors as well	 |
 	 	 	 
 
## Operation Example

    <operation
      id="prepare-av"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Preparing presenter audio and video work versions">
      <configurations>
        <configuration key="source-flavor">presenter/source</configuration>
        <configuration key="target-flavor">presenter/work</configuration>
        <configuration key="rewrite">false</configuration>
        <configuration key="promiscuous-audio-muxing">true</configuration>
      </configurations>
    </operation>
