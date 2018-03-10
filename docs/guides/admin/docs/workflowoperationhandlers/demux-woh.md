# DemuxWorkflowHandler

## Description

The DemuxWorkflowHandler is used to demux presenter and presentation source source media from capture agents (such as epiphan pearl)
that mux the two together for uploads.
It uses a special encoding profile that has two outputs, it then flavors the target media in the order listed in the encoding profile output.

The source recording are selected by source-flavors 

# Example

## profile:

profile.epiphan-demux.name = epiphan-demux
profile.epiphan-demux.input = visual
profile.epiphan-demux.output = visual
profile.epiphan-demux.suffix = -30fps.mp4
profile.epiphan-demux.mimetype = video/mp4
profile.epiphan-demux.ffmpeg.command = -strict unofficial -i #{in.video.path} -itsoffset 0.149 -i #{in.video.path} -map 0:0 -map 1:1 -c:v copy -c:a copy #{out.dir}/#{out.name}_presenter#{out.suffix} -c:v copy -c:a copy -map 0:2 -map 1:3  #{out.dir}/#{out.name}_presentation#{out.suffix}

## Parameter Table

|configuration keys | example                     | description                                                         |
|-------------------|-----------------------------|---------------------------------------------------------------------|
|source-flavors     | multitrack/source           | Which media should be encoded                               |
|target-tags        | archive                     | Specifies the tags of the new media                               |
|target-flavors     | presenter/source,presentation/source  | Specifies the flavors of the new media                       |
|encoding-profile   | epiphan-demux               | Specifies the encoding profile |

	 
 
## Operation Example

   <operation
      id="demux"
      exception-handler-workflow="ng-partial-error"
      description="Extract epiphan presenter and presentation video from multitrack source (target listed in the same order in encoding profile)">
      <configurations>
        <configuration key="source-flavors">multitrack/source</configuration>
        <configuration key="target-flavors">presenter/source,presentation/source</configuration>
        <configuration key="target-tags">archive</configuration>
        <configuration key="encoding-profile">epiphan-demux</configuration>
      </configurations>
    </operation>
