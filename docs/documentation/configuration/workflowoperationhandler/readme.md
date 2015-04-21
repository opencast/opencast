# Workflow Operation Handler

## Introduction
Workflows are the central element to define how a media package is being processed by the Matterhorn services. Their definitions consist of a list of workflow operations, which basically map a piece of configuration to Matterhorn code:

    <definition xmlns="http://workflow.opencastproject.org">
         ....
        <operation
          id="tag"
          <configurations>
            <configuration key="source-flavors">presentation/trimmed</configuration>
            <configuration key="target-flavor">presentation/tagged</configuration>
          </configurations>
       </operation>
        ...
    </definition>

##Default Workflow Operations
The following table contains the workflow operations that are available in an out-of-the-box Matterhorn installation:

|Operation Handler	|Description						|Details|
|-----------------------|-------------------------------------------------------|-------|
|defaults		|Applies default workflow configuration values		|[Documentation](defaults-woh.md)|
|email			|Sends email notifications at any part of a workflow	|[Documentation](email-woh.md)|
|republish		|Republishes elements to search				|[Documentation](republish-woh.md)|
|tag			|Modify the tag sets of media package elements		|[Documentation](tag-woh.md)|
|apply-acl		|Apply ACL rom series to the mediapackage		|[Documentation](applyacl-woh.md)|
|inspect			|Inspect the media (check if it is valid)		|[Documentation](inspect-woh.md)|
|prepare-av		|Preparing audio and video work versions		|[Documentation](prepareav-woh.md)|
|compose			|Encode media files using FFmpeg			|[Documentation](compose-woh.md)|
|trim			|Waiting for user to review, then trim the recording	|[Documentation](trim-woh.md)|
|caption			|Waiting for user to upload captions			|[Documentation](caption-woh.md)|
|segment-video		|Extracting segments from presentation			|[Documentation](segmentvideo-woh.md)|
|image			|Extract images from a video using FFmpeg		|[Documentation](image-woh.md)|
|segmentpreviews		|Extract segment images from a video using FFmpeg	|[Documentation](segmentpreviews-woh.md)|
|extract-text		|Extracting text from presentation segments		|[Documentation](extracttext-woh.md)|
|publish-engage		|Distribute and publish media to the engage player	|[Documentation](publishengage-woh.md)|
|archive			|Archive the current state of the mediapackage		|[Documentation](archive-woh.md)|
|cleanup			|Cleanup the working file repository			|[Documentation](cleanup-woh.md)|
|zip			|Create a zipped archive of the current state of the mediapackage |[Documentation](zip-woh.md)|
|image-to-video		|Create a video track from a source image		|[Documentation](imagetovideo-woh.md)|
|composite		|Compose two videos on one canvas.			|[Documentation](composite-woh.md)|
|concat			|Concatenate multiple video tracks into one video track	|[Documentation](concat-woh.md)|
|post-mediapackage	|Send mediapackage to remote service			|[Documentation](postmediapackage-woh.md)|
|http-notify		|Notifies an HTTP endpoint about the process of the workflow |[Documentation](httpnotify-woh.md)|
|incident		|Testing incidents on a dummy job			|[Documentation](incident-woh.md)|
|ingest-download	|Download files from external URL for ingest		|[Documentation](ingestdownload-woh.md)|
|analyze-audio		|Analyze first audio stream				|[Documentation](analyzeaudio-woh.md)|
|normalize-audio		|Normalize first audio stream				|[Documentation](normalizeaudio-woh.md)|
|editor			|Waiting for user to review, then create a new file based on edit-list |[Documentation](editor-woh.md)|
|silence			|Silence detection on audio of the mediapackage		|[Documentation](silence-woh.md)|
|waveform		|Create a waveform image of the audio of the Mediapackage |[Documentation](waveform-woh.md)|

