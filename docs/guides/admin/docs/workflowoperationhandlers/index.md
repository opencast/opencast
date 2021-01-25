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

## Default Workflow Operations

The following table contains the workflow operations that are available in an out-of-the-box Opencast installation:

|Operation Handler   |Description                                                    |Details|
|--------------------|---------------------------------------------------------------|------------------------------------|
|add-catalog         |Add a catalog to the media package                             |[Documentation](add-catalog-woh.md)|
|analyze-audio       |Analyze first audio stream                                     |[Documentation](analyzeaudio-woh.md)|
|analyze-tracks      |Analyze tracks in media package                                |[Documentation](analyze-tracks-woh.md)|
|animate             |Create animated video sequence                                 |[Documentation](animate-woh.md)|
|amberscript-start-transcription|Start AmberScript Transcription                     |[Documentation](amberscript-start-transcription-woh.md)|
|amberscript-attach-transcription|Attach AmberScript Transcription                   |[Documentation](amberscript-attach-transcription-woh.md)|
|asset-delete        |Deletes the current mediapackage from the Archive              |[Documentation](asset-delete-woh.md)|
|attach-watson-transcription|Attaches automated transcripts to mediapackage          |[Documentation](attach-watson-transcription-woh.md)|
|cleanup             |Cleanup the working file repository                            |[Documentation](cleanup-woh.md)|
|clone               |Clone media package elements to another flavor                 |[Documentation](clone-woh.md)|
|comment             |Add, resolve or delete a comment                               |[Documentation](comment-woh.md)|
|composite           |Compose two videos on one canvas.                              |[Documentation](composite-woh.md)|
|concat              |Concatenate multiple video tracks into one video track         |[Documentation](concat-woh.md)|
|configure-by-dcterm |Set workflow parameter if dublincore term matches value        |[Documentation](configure-by-dcterm-woh.md)|
|copy                |Copy media package elements to target directory                |[Documentation](copy-woh.md)|
|cover-image         |Generate a cover-image containing metadata                     |[Documentation](coverimage-woh.md)|
|crop-video          |Checks for black bars on the sides of the video                |[Documentation](cropvideo-woh.md)|
|cut-marks-to-smil   |Parses timestamps into a SMIL for the editor workflow          |[Documentation](cut-marks-to-smil-woh.md)|
|defaults            |Applies default workflow configuration values                  |[Documentation](defaults-woh.md)|
|demux               |Demuxes streams to multiple output files                       |[Documentation](demux-woh.md)|
|duplicate-event     |Create an event by cloning an existing one                     |[Documentation](duplicate-event-woh.md)|
|editor              |Waiting for user to review, then cut video based on edit-list  |[Documentation](editor-woh.md)|
|encode              |Encode media files to differents formats in parallel           |[Documentation](encode-woh.md)|
|error-resolution    |Internal operation to pause a workflow in error                |[Documentation](error-resolution-woh.md)|
|execute-many        |Execute a command for each matching element in a MediaPackage  |[Documentation](execute-many-woh.md)
|execute-once        |Execute a command for a MediaPackage                           |[Documentation](execute-once-woh.md)
|export-wf-properties|Export workflow properties                                     |[Documentation](export-wf-properties-woh.md)|
|extract-text        |Extracting text from presentation segments                     |[Documentation](extracttext-woh.md)|
|failing             |Operations that always fails                                   |[Documentation](failing-woh.md)|
|google-speech-attach-transcription|Attaches automated transcripts to mediapackage   |[Documentation](google-speech-attach-transcription-woh.md)|
|google-speech-start-transcription|Starts automated transcription provided by Google Speech|[Documentation](google-speech-start-transcription-woh.md)|
|http-notify         |Notifies an HTTP endpoint about the process of the workflow    |[Documentation](httpnotify-woh.md)|
|image               |Extract images from a video using FFmpeg                       |[Documentation](image-woh.md)|
|image-convert       |Convert images using FFmpeg                                    |[Documentation](image-convert-woh.md)|
|image-to-video      |Create a video track from a source image                       |[Documentation](imagetovideo-woh.md)|
|import-wf-properties|Import workflow properties                                     |[Documentation](import-wf-properties-woh.md)|
|incident            |Testing incidents on a dummy job                               |[Documentation](incident-woh.md)|
|include             |Include workflow definition in current workflow                |[Documentation](include-woh.md)|
|ingest-download     |Download files from external URL for ingest                    |[Documentation](ingestdownload-woh.md)|
|inspect             |Inspect the media (check if it is valid)                       |[Documentation](inspect-woh.md)|
|log                 |Log workflow status                                            |[Documentation](log-woh.md)|
|multiencode         |Encode to multiple profiles in one operation                   |[Documentation](multiencode-woh.md)|
|normalize-audio     |Normalize first audio stream                                   |[Documentation](normalizeaudio-woh.md)|
|partial-import      |Import partial tracks and process according to a SMIL document |[Documentation](partial-import-woh.md)|
|partial-retract     |Retract a subset of the mediapackage from a publication        |[Documentation](partial-import-woh.md)|
|post-mediapackage   |Send mediapackage to remote service                            |[Documentation](postmediapackage-woh.md)|
|prepare-av          |Preparing audio and video work versions                        |[Documentation](prepareav-woh.md)|
|probe-resolution    |Set workflow instance variables based on video resolution      |[Documentation](probe-resolution-woh.md)|
|process-smil        |Edit and Encode media defined by a SMIL file                   |[Documentation](process-smil-woh.md)|
|publish-aws         |Distribute and publish media to Amazon S3 and Cloudfront       |[Documentation](publish-aws-woh.md)|
|publish-configure   |Distribute and publish media to the configured publication     |[Documentation](publish-configure-woh.md)|
|publish-engage      |Distribute and publish media to the engage player              |[Documentation](publish-engage-woh.md)|
|publish-oaipmh      |Distribute and publish media to a OAI-PMH repository           |[Documentation](publish-oaipmh-woh.md)|
|publish-youtube     |Distribute and publish media to YouTube                        |[Documentation](publish-youtube-woh.md)|
|republish-oaipmh    |Update media in a OAI-PMH repository                           |[Documentation](republish-oaipmh-woh.md)|
|retract-aws         |Retracts media from AWS S3 and Cloudfront publication          |[Documentation](retract-aws-woh.md)|
|retract-configure   |Retracts media from configured publication                     |[Documentation](retract-configure-woh.md)|
|retract-engage      |Retracts media from Opencast Media Module publication          |[Documentation](retract-engage-woh.md)|
|retract-oaipmh      |Retracts media from a OAI-PMH repository                       |[Documentation](retract-oaipmh-woh.md)
|retract-youtube     |Retracts media from YouTube                                    |[Documentation](retract-youtube-woh.md)|
|segment-video       |Extracting segments from presentation                          |[Documentation](segmentvideo-woh.md)|
|segmentpreviews     |Extract segment images from a video using FFmpeg               |[Documentation](segmentpreviews-woh.md)|
|select-streams       |Select streams for further processing                           |[Documentation](select-streams-woh.md)|
|send-email          |Sends email notifications at any part of a workflow            |[Documentation](send-email-woh.md)|
|series              |Apply series to the mediapackage                               |[Documentation](series-woh.md)|
|silence             |Silence detection on audio of the mediapackage                 |[Documentation](silence-woh.md)|
|snapshot            |Archive the current state of the mediapackage                  |[Documentation](snapshot-woh.md)|
|start-watson-transcription|Starts automated transcription provided by IBM Watson    |[Documentation](start-watson-transcription-woh.md)|
|start-workflow      |Start a new workflow for given media package ID                |[Documentation](start-workflow-woh.md)|
|statistics-writer   |Log statistical data about the video                           |[Documentation](statistics-writer.md)|
|tag                 |Modify the tag sets of media package elements                  |[Documentation](tag-woh.md)|
|tag-by-dcterm       |Modify the tags if dublincore term matches value               |[Documentation](tag-by-dcterm-woh.md)|
|theme               |Make settings of themes available to processing                |[Documentation](theme-woh.md)|
|timelinepreviews    |Create a preview image stream from a given video track         |[Documentation](timelinepreviews-woh.md)|
|transfer-metadata   |Transfer metadata fields between catalogs                      |[Documentation](transfer-metadata-woh.md)|
|waveform            |Create a waveform image of the audio of the mediapackage       |[Documentation](waveform-woh.md)|
|zip                 |Create zipped archive of the current state of the mediapackage |[Documentation](zip-woh.md)|

## State Mappings
Technically, a workflow can be in one of the following states:

| Technical State | Description | What the Admin UI displays in the events table|
|-----------------|-------------|-----------------------------------------------|
|**instantiated**| The workflow is queued and will be started as soon as possible | "Pending" |
|**running**| The workflow is running, no problems so far | "Running" |
|**stopped**| The workflow was aborted by the user | "Processing canceled" |
|**paused**| The workflow was paused and can be continued | "Paused" |
|**succeeded**| The workflow has completed successfully | "Finished" |
|**failed**| The workflow failed due to an error | "Processing failure" |
|**failing**| The workflow is still running, but there were errors. It will fail. | "Running" |

Using state mappings, it is possible to refine the labels displayed in the Admin UI events table for a particular
workflow.

Here is an example which displays "Retracting" instead of "Running" for the retract workflow:

```
<?xml version="1.0" encoding="UTF-8"?>
<definition xmlns="http://workflow.opencastproject.org">
  <id>retract</id>
  ...
  <state-mappings>
    <state-mapping state="running">retracting</state-mapping>
    <state-mapping state="failing">retracting</state-mapping>
  </state-mappings>
```

When no state mappings are configured for a workflow, the generic default labels will be displayed.

When a workflow includes other workflows, the event table only shows the state of the including workflow.
