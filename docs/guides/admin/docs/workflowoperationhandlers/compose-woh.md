# ComposeWorkflowHandler

## Description
The ComposeWorkflowHandler is used to encode media files to different formats using FFmpeg.

## Parameter Table

|configuration keys|example             |description                                                                    |
|------------------|--------------------|-------------------------------------------------------------------------------|
|source-flavor     | presenter/work     | Which media should be encoded                                                 |
|target-flavor     | presenter/delivery | Specifies the flavor of the new media                                         |
|source-tags       | sometag            | Tags of media to encode                                                       |
|target-tags       | sometag            | Specifies the tags of the new media                                           |
|encoding-profile  | webm-hd            | Specifies the encoding profile to use                                         |
|tags-and-flavors  | true               | When false (default), the operation selects input elements that have EITHER any of the source tags OR the source flavor. When true, the operation selects input elements that have BOTH the source-flavor AND any of the source tags |


## Operation Example

    <operation
        id="compose"
        fail-on-error="true"
        exception-handler-workflow="error"
        description="Encoding presenter (camera) video to Flash download">
        <configurations>
            <configuration key="source-flavor">presenter/trimmed</configuration>
            <configuration key="target-flavor">presenter/delivery</configuration>
            <configuration key="target-tags">engage</configuration>
            <configuration key="encoding-profile">flash.http</configuration>
        </configurations>
    </operation>
