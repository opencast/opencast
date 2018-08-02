# MultiencodeWorkflowHandler

## Description

The MultiencodeWorkflowHandler is used to encode source media into multiple formats concurrently.
The source recording are selected by source-flavors AND source-tags.
Each source media selector (eg presenter or presentation) can have an independent set of encoding profile ids
(one for each target medium) and target tags.
Encoding of each source medium runs as one ffmpeg command.
This operation will generate one multiencode operation per source medium,
all of them running concurrently on the same or on different workers.
In addition, the target media can be optionally tagged with the encoding profile ids.

### Configuration details

This workflow handles each source selector independently as a section.
The parameters for each configuration, such as flavors are separated positionally into sections by "**;**".
The use of the semi-colon is optional. If it is absent, there is only one section.


```
<configuration key="source-flavors">*/source</configuration>
```
> One source selector means that all the matching recording will be processed the same way.
>
```
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

```
<configuration key="target-flavors">*/preview</configuration>
```
>   All targets are flavored the same way, using the example above, becomes "presenter/preview"
> and "presentation/preview"
>

Each source selector can have its own set of target tags and flavors, defined as a comma delimited list.
For example:

```
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

## Parameter Table

|configuration keys | example                     | description                                                         |
|-------------------|-----------------------------|---------------------------------------------------------------------|
|source-flavors     | presenter/source*;*presentation/source  | Which media should be encoded                               |
|target-flavors     | */preview                  | Specifies the flavor of the new media                               |
|target-tags        | rss,archive              | Specifies the tags of the new media                                 |
|encoding-profiles  | mp4-low.http,mp4-medium.http*;*mp4-hd.http,mp4-hd.http | Encoding profiles for each source flavor |
|tag-with-profile   | true  | target media are tagged with coresponding encoding profile Id (false if omitted)     |



## Operation Example

    <operation
        id="multiencode"
        fail-on-error="true"
        exception-handler-workflow="error"
        description="Encoding media to delivery formats">
        <configurations>
            <configuration key="source-flavors">presenter/work;presentation/work</configuration>
            <configuration key="target-flavors">*/delivery</configuration>
            <configuration key="target-tags">rss,archive</configuration>
            <configuration key="encoding-profiles">mp4-low.http;mp4-hd.http</configuration>
            <configuration key="tag-with-profile">true</configuration>
        </configurations>
    </operation>

## Note: (Important)
Each source flavor generates all the target formats in one ffmpeg call by incorporating relevant parts
of the encoding profile commands.

* Care must be taken that no ffmpeg complex filters are used in the encoding profiles used for this workflow,
as it can cause a conflict.

* Encoded target media are distinguished by the suffix, it is important that all the encoding profiles used have
distinct suffixes to use "tag-with-profile" configuration, for example:
```
profile.mp4-vga-medium.http.suffix = -vga-medium.mp4
profile.mp4-medium.http.suffix = -medium.mp4
```
