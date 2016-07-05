# Workflow Operation Handler

## Introduction

Workflows are the central element to define how a media package is being processed by the Opencast services. Their
definitions consist of a list of workflow operations, which basically map a piece of configuration to Opencast code:

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

The following table contains the workflow operations that are available in an out-of-the-box Opencast installation:

|Operation Handler   |Description                              |Details|
|--------------------|---------------------------------------------------------------|------------------------------------|
|analyze-tracks      |Analyze tracks in media package                                |[Documentation](analyze-tracks-woh.md)|
|analyze-audio       |Analyze first audio stream                                     |[Documentation](analyzeaudio-woh.md)|
|append              |Hold for user to select workflow to continue with              |[Documentation](append-woh.md)|
|archive             |Archive the current state of the mediapackage                  |[Documentation](archive-woh.md)|
|caption             |Waiting for user to upload captions                            |[Documentation](caption-woh.md)|
|cleanup             |Cleanup the working file repository                            |[Documentation](cleanup-woh.md)|
|comment             |Add, resolve or delete a comment                               |[Documentation](comment-woh.md)|
|compose             |Encode media files using FFmpeg                                |[Documentation](compose-woh.md)|
|composite           |Compose two videos on one canvas.                              |[Documentation](composite-woh.md)|
|concat              |Concatenate multiple video tracks into one video track         |[Documentation](concat-woh.md)|
|copy                |Copy media package elements to target directory                |[Documentation](copy-woh.md)|
|cover-image         |Generate a cover-image containing metadata                     |[Documentation](coverimage-woh.md)|
|defaults            |Applies default workflow configuration values                  |[Documentation](defaults-woh.md)|
|editor              |Waiting for user to review, then cut video based on edit-list  |[Documentation](editor-woh.md)|
|email               |Sends email notifications at any part of a workflow            |[Documentation](email-woh.md)|
|encode              |Encode media files to differents formats in parallel           |[Documentation](encode-woh.md)|
|export-wf-properties|Export workflow properties                                     |[Documentation](export-wf-properties-woh.md)|
|extract-text        |Extracting text from presentation segments                     |[Documentation](extracttext-woh.md)|
|http-notify         |Notifies an HTTP endpoint about the process of the workflow    |[Documentation](httpnotify-woh.md)|
|image               |Extract images from a video using FFmpeg                       |[Documentation](image-woh.md)|
|image-to-video      |Create a video track from a source image                       |[Documentation](imagetovideo-woh.md)|
|import-wf-properties|Import workflow properties                                     |[Documentation](import-wf-properties-woh.md)|
|incident            |Testing incidents on a dummy job                               |[Documentation](incident-woh.md)|
|ingest-download     |Download files from external URL for ingest                    |[Documentation](ingestdownload-woh.md)|
|inspect             |Inspect the media (check if it is valid)                       |[Documentation](inspect-woh.md)|
|normalize-audio     |Normalize first audio stream                                   |[Documentation](normalizeaudio-woh.md)|
|partial-import      |Import partial tracks and process according to a SMIL document |[Documentation](partial-import-woh.md)|
|post-mediapackage   |Send mediapackage to remote service                            |[Documentation](postmediapackage-woh.md)|
|prepare-av          |Preparing audio and video work versions                        |[Documentation](prepareav-woh.md)|
|publish-configure   |Distribute and publish media to the configured publication     |[Documentation](publishconfigure-woh.md)|
|publish-engage      |Distribute and publish media to the engage player              |[Documentation](publishengage-woh.md)|
|republish           |Republishes elements to search                                 |[Documentation](republish-woh.md)|
|retract-configure   |Retracts media from configured publication                     |[Documentation](retractconfigure-woh.md)|
|segment-video       |Extracting segments from presentation                          |[Documentation](segmentvideo-woh.md)|
|segmentpreviews     |Extract segment images from a video using FFmpeg               |[Documentation](segmentpreviews-woh.md)|
|series              |Apply series to the mediapackage                               |[Documentation](series-woh.md)|
|silence             |Silence detection on audio of the mediapackage                 |[Documentation](silence-woh.md)|
|tag                 |Modify the tag sets of media package elements                  |[Documentation](tag-woh.md)|
|trim                |Waiting for user to review, then trim the recording            |[Documentation](trim-woh.md)|
|waveform            |Create a waveform image of the audio of the mediapackage       |[Documentation](waveform-woh.md)|
|zip                 |Create zipped archive of the current state of the mediapackage |[Documentation](zip-woh.md)|
