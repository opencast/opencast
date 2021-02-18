Live Schedule Service
=====================

Overview
--------

The Live Schedule Service manages a live event in the Search index on the engage server.

When an event is scheduled and the publishLive configuration is set, a live media package is published to the Search
index. The live media package contains track(s) with live streaming URLs.

The live media package is retracted from the Search index when the capture finishes or if it fails.

If event metadata, such as title or duration, are updated, the live media package in the Search index is updated
accordingly.

Pre-requisites
--------------

To use this service, you need to have:

1. A streaming server (Wowza, nginx-rtmp) or CDN already set up to stream live content
2. A capture agent capable of streaming to it
3. A player capable of playing live streams. The Paella player supports the HLS protocol.
   Other players/protocols have not been tested.

Configuration
-------------

### Step 1: Configure the service

Edit  _etc/org.opencastproject.liveschedule.impl.LiveScheduleServiceImpl.cfg_.

If your capture agent does not register a _capture.device.live.resolution.WIDTHxHEIGHT_ property, it's mandatory to
configure the _live.streamingUrl_.

The _live.streamingUrl_ should be set to your streaming server's output URL (or
the subscriber URL specified by your CDN) and is indexed by the engage server.

This is the URL that the player will use to play the live stream. For instance,
if using rtmp, set it to something like:
rtmp://STREAMING_SERVER_HOST:PORT/STREAMING_APPLICATION/

For HLS, see below:
```
# Configuration for the Live Schedule Service

#
# If the capture agent doesn't register the capture.device.live.resolution.WIDTHxHEIGHT property,
# specify live.streamingUrl, live.resolution, and live.streamName below:
#
# -----------------------------

# The base URL that the player will use to play the live stream
live.streamingUrl=http://streaming.server/hls/

# If a comma-separated list is provided, several resolutions will be generated for each flavor
live.resolution=1920x540,960x270

# Possible variable substitutions:
# #{id} = media package id
# #{flavor} = type-subtype of flavor
# #{caName} = capture agent name
# #{resolution} = video resolution e.g. 1920x1080
#live.streamName=#{caName}-#{flavor}.stream-#{resolution}
live.streamName=#{caName}/playlist.m3u8

# -----------------------------

# The same mime-type applies to all flavors and resolutions
live.mimeType=application/x-mpegURL

# If a comma-separated list is provided, several streams links will be generated, one for each
# resolution-targetFlavor combination.
# Default is presenter/delivery
#live.targetFlavors=presenter/delivery

# The distribution service to use: download or aws.s3
live.distributionService=download

# A list of combinations with target flavor and resolution for which streaming URIs should be published.
# For example: live.publishStreaming=presenter/delivery:1920x540
# Default is not to publish streaming URIs
# live.publishStreaming=
```

### Step 2: Configure the capture agent

#### Capture agent does not register the _capture.device.live.resolution.WIDTHxHEIGHT_ property

Configure the capture agent to stream to your streaming server (or the publisher URL specified by your CDN), using the
same stream name specified in live.streamName.

#### Capture agent registers the _capture.device.live.resolution.WIDTHxHEIGHT_ property

If your capture agent supports configuring custom capture agent properties, instead of configuring the
live.streamingUrl, live.resolution, live.streamName, you can update the capture agent firmware to pass the following
when registering to Opencast:

* capture.device.names: add 'live' to the current list of devices
* capture.device.live.resolution.WIDTHxHEIGHT=STREAMING_URL_USED_BY_PLAYER: one for each desired stream

Then, the LiveScheduleService will generate as many live tracks as the resolutions registered, with their streaming
URLs, using 'presenter/delivery' (or the flavor configured, but only one flavor can be used).

If a property capture.device.live.resolution.WIDTHxHEIGHT was registered, it will take precedence over the
LiveScheduleService configuration.

#### Example 1:

#### Capture agent does not register with capture.device.live.resolution.WIDTHxHEIGHT

If:

* live.streamingUrl=rtmp://STREAMING_SERVER_HOST:PORT/STREAMING_APPLICATION
* live.streamName=#{caName}-#{flavor}.stream
* live.targetFlavors=presenter/delivery
* capture agent name: ca01

Then, the capture agent should stream to ('/' is replaced by '-'):
rtmp://STREAMING_SERVER_HOST:PORT/STREAMING_APPLICATION/ca01-presenter-delivery.stream

Note: Please refer to your streaming server or CDN documentation for the correct syntax of the streaming URL. The
_live.streamingUrl_ may be very different from the URL the capture agent streams to. For instance, with Akamai, the URL
used by the player will be something like live.streamingUrl=rtmp://xyz.live.edgefcs.net/live/ and the capture agent's
publish URL something like rtmp://a.bcd.e.akamaientrypoint.net/EntryPoint. The stream name should always match.

#### Example 2:

#### Capture agent registers with capture.device.live.resolution.WIDTHxHEIGHT

If the capture agent registers itself with:

|property  key|value|
|-------------|-----|
|capture.device.names|presentation,presenter,live|
|capture.device.presentation.flavor|presentation/source|
|capture.device.presenter.flavor|presenter/source|
|capture.device.live.resolution.1920x540|rtmp://xyz.live.edgefcs.net/live/presenter.stream-1920x540@12345|
|capture.device.live.resolution.960x270|rtmp://xyz.live.edgefcs.net/live/presenter.stream-960x270@12345|

The LiveScheduleService will generate a media package with two live tracks having the following urls:

* rtmp://xyz.live.edgefcs.net/live/presenter.stream-1920x540@12345
* rtmp://xyz.live.edgefcs.net/live/presenter.stream-960x270@12345


### Step 3: Configure the Workflow

When scheduling a live event via the admin UI, the workflow needs to have the _publishLive_ configuration set to true
(this is already included in the sample workflows).
If not using the sample Opencast workflows, add to the `<configuration_panel>`:

```
        <fieldset>
          <legend>Publish live stream:</legend>
          <ul>
            <li>
              <input id="publishLive" name="publishLive" type="checkbox" class="configField" value="false" />
              <label for="publishLive">Add live event to Opencast Media Module</label>
            </li>
          </ul>
        </fieldset>
```

And to the _defaults_ operation:

```
    <operation
      id="defaults"
      description="Applying default configuration values">
      <configurations>
        <configuration key="comment">false</configuration>
        <configuration key="publishToMediaModule">true</configuration>
        <configuration key="publishToOaiPmh">true</configuration>
        <configuration key="uploadedSearchPreview">false</configuration>
        <configuration key="publishLive">false</configuration>
      </configurations>
    </operation>
```
