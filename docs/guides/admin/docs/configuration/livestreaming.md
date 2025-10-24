Livestreaming
=============

Overview
--------

**Opencast itself currently does not do livestreaming** - you need a streaming server for this to work. Opencast can
however create a publication that references a livestream, making it work the same as a pre-recorded event.

The **Live Publication Service** is responsible for managing those publications. A live publication is created when the
`stream` setting is activated in the capture agent configuration of a scheduled event.
This might take a couple of minutes. (**Activating livestreams through a workflow parameter is no longer supported!**)
This publication contains the usual ACLs and metadata catalogs
as well as live tracks which link to the livestreams. It's available in the search index under /search, where it's also
used by Opencast's player, as well as the External API.

The publication is updated automatically when either metadata or ACLs or the scheduling settings (start & end date,
capture agent with configuration) are updated. When the recording finishes, it's automatically retracted.

Pre-requisites
--------------

To use this service, you need to have:

1. A streaming server (Wowza, nginx-rtmp) or CDN already set up to stream live content
2. A capture agent capable of streaming to it
3. A player capable of playing live streams. The Paella player supports the HLS protocol.
   Other players/protocols have not been tested.

Configuration
-------------

### If your capture agent does not register the _capture.device.live.resolution.WIDTHxHEIGHT_ property

#### 1. Edit  **etc/org.opencastproject.livepublication.impl.LivePublicationServiceImpl.cfg**.

Configure the `live.streamingUrl`. It should be set to your streaming server's output URL (or
the subscriber URL specified by your CDN). This is the URL that the player will use to play the live stream.

- For rtmp, set it to something like: rtmp://streaming.server:PORT/STREAMING_APPLICATION/
- For HLS, it could look like this: http://streaming.server/hls/

The `live.targetFlavors` (default: `presenter/delivery`) and `live.resolution` (default: `1920x540,960x270`) determine
the live tracks that will be generated.
You can configure multiple of each as a comma-separated list without spaces. There will be one track generated for each
flavor x resolution combination.

Each track contains a link to a livestream whose URL looks like this: STREAMING_URL/STREAM_NAME.
The latter is configured in `live.streamName` (default: `#{caName}/playlist.m3u8`. The following variables are supported:

- `#{id}` - media package id
- `#{flavor}` - type-subtype of flavor
- `#{caName}` - capture agent name
- `#{resolution}` - video resolution

(All `/` are replaced by `-`.)

Use these variables to keep the URL unique for each livestream, especially if you generate multiple tracks.

#### 2. Configure Capture Agent

Configure the capture agent to stream to your streaming server (or the publisher URL specified by your CDN).
Keep in mind that the capture agent might need to stream to a different URL than `live.streamingurl`, depending on your
streaming setup. Use the same stream name as specified in `live.streamName`.

### If your Capture agent registers the _capture.device.live.resolution.WIDTHxHEIGHT_ property

#### 1. Configure Capture Agent

If your capture agent supports configuring custom capture agent properties, you can update the settings to pass on the
following when registering to Opencast:

* `capture.device.live.resolution.WIDTHxHEIGHT`=STREAMING_URL - one for each desired stream
* Optional: `capture.device.names`: add `live` to the current list of devices

Then, the LivePublicationService will generate as many live tracks as there are resolutions configured, with the
respective streaming URLs, using the default flavor `presenter/delivery`. This will take precedence over the
LivePublicationService configuration.

### Optional Configuration

The `live.mimeType` (default: `application/x-mpegURL`) always applies to all generated live tracks.

You can also use AWS to distribute the attachments and catalogs by changing `DownloadDistributionService.target`
(default: `download`) to `aws.s3`.
