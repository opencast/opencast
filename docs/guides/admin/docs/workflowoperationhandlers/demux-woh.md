Demux Workflow Operation
========================

Description
-----------

The demux operation can be used to demux multiple streams (e.g. presenter and presentation) from one container and put
them into separate tracks. It uses a special encoding profile that has two outputs. It flavors the target media in the
order listed in the encoding profile output.

Parameter Table
---------------

|Configuration Key |Example                      |Description                            |
|------------------|-----------------------------|---------------------------------------|
|source-flavors    |`multitrack/source`          |Which media should be encoded          |
|target-tags       |`archive,rss;rss`            |Specifies the tags of the new media    |
|target-flavors    |`presenter/*,presentation/*` |Specifies the flavors of the new media |
|encoding-profile  |`demux`                      |Specifies the encoding profile         |

Note that `target-tags` can hold multiple sets of tags separated by `;`. Each set is applied to the matching set of
output files (same order). Target flavors are separated by `,` as usual. They are applied in order as well.


Operation Example
-----------------

```xml
<operation
  id="demux"
  exception-handler-workflow="partial-error"
  description="Extract presenter and presentation video from multitrack source">
  <configurations>
    <configuration key="source-flavors">multitrack/source</configuration>
    <configuration key="target-flavors">presenter/source,presentation/source</configuration>
    <configuration key="target-tags">archive</configuration>
    <configuration key="encoding-profile">demux</configuration>
  </configurations>
</operation>
```

Example Profile
---------------

```
profile.demux.name = demux
profile.demux.input = visual
profile.demux.output = visual
profile.demux.suffix = .mp4
profile.demux.ffmpeg.command = -i #{in.video.path} -c copy \
  -map 0:a:0 -map 0:v:0 #{out.dir}/#{out.name}_presenter#{out.suffix} \
  -map 0:a:1 -map 0:v:1 #{out.dir}/#{out.name}_presentation#{out.suffix}
```
