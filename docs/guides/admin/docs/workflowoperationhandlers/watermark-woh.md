## WatermarkWorkflowOperationHandler

## Description
The WatermarkWorkflowOperationHandler adds a custom image (usually a small one) to the source video. 

The operation needs a source video and a custom image file to create a watermarked video.

## Parameter Table

|configuration keys       |example                                            |description|
|------------------------ |---------------------------------------------------|---------------------------------------------------------------------|
|source-flavor            |presenter/trimmed                                  |The flavor of the track to use as a video source input|
|watermark                |${karaf.etc}/branding/watermark.png                |The path of the image used as a watermark|
|target-flavor            |presenter/watermarked                              |The flavor to apply to the encoded file|
|target-tags              |sometag                                            |The tags to apply to the encoded file |
|encoding-profile         |watermark.branding                                 |Specifies the encoding profile to use |

## Operation Example

    <operation
      id="watermark"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Watermarking presenter (camera) video">
      <configurations>
        <configuration key="source-flavor">presenter/trimmed</configuration>
        <configuration key="watermark">${karaf.etc}/branding/watermark.png</configuration>
        <configuration key="target-flavor">presenter/watermarked</configuration>
        <configuration key="encoding-profile">watermark.branding</configuration>
      </configurations>
    </operation>
