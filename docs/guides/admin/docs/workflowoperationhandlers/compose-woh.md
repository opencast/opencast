# ComposeWorkflowHandler

## Description
The ComposeWorkflowHandler is used to encode media files to different formats using FFmpeg.

## Parameter Table

|configuration keys|example                   |description                                                                    |
|------------------|--------------------------|-------------------------------------------------------------------------------|
|source-flavor     | presenter/work           | Which media should be encoded                                                 |
|target-flavor     | presenter/delivery       | Specifies the flavor of the new media                                         |
|source-tags       | sometag                  | Tags of media to encode                                                       |
|target-tags       | sometag                  | Specifies the tags of the new media                                           |
|encoding-profile  | mp4-hd.http              | Specifies the encoding profile to use                                         |
|encoding-profiles | mp4-low.http,mp4-hd.http | Specifies a comma-separated encoding profiles to use                           |
|tags-and-flavors  | true                     | When false (default), the operation selects input elements that have EITHER any of the source tags OR the source flavor. When true, the operation selects input elements that have BOTH the source-flavor AND any of the source tags |


## Operation Examples

Encoding presenter (camera) video to MP4 medium quality:

    <operation
        id="compose"
        fail-on-error="true"
        exception-handler-workflow="partial-error"
        description="Encoding presenter (camera) video to MP4 medium quality">
        <configurations>
            <configuration key="source-flavor">presenter/trimmed</configuration>
            <configuration key="target-flavor">presenter/delivery</configuration>
            <configuration key="target-tags">engage-download</configuration>
            <configuration key="encoding-profile">mp4-medium.http</configuration>
        </configurations>
    </operation>

Encoding 480p, 720p and 1080p video to MP4 adaptive streaming:

    <operation
        id="compose"
        fail-on-error="true"
        exception-handler-workflow="partial-error"
        description="Encoding 480p, 720p and 1080p video to MP4 streaming">
        <configurations>
            <configuration key="source-flavor">*/work</configuration>
            <configuration key="target-flavor">*/delivery</configuration>
            <configuration key="target-tags">engage-download,engage-streaming</configuration>
            <configuration key="encoding-profiles">adaptive-480p.http,adaptive-720p.http,adaptive-1080p.http</configuration>
        </configurations>
    </operation>