Live Schedule Service
=====================

Overview
--------

The Live Schedule Service manages a live event in the Search index on the engage server.

When an event is scheduled and the publishLive configuration is set, a live media package is published to the Search index. The live media package contains track(s) with live streaming urls.

The live media package is retracted from the Search index when the capture finishes or if it fails.

If event metadata are updated, such as title, duration, the live media package in the Search index is updated accordingly.

Pre-requisites
--------------

To use this service, you need to have:

1. A streaming server (Wowza, Adobe Media Server) or CDN already set up to stream live content
2. A capture agent capable of streaming to it
3. A player capable of playing live streams. The Paella player using the Flash component supports the rtmp protocol. Other players/protocols have not been tested.

Configuration
-------------

### Step 1: Configure the service

Edit  _etc/org.opencastproject.liveschedule.impl.LiveScheduleServiceImpl.cfg_:

- _live.streamingUrl_ should be set to your streaming server url (or the subscriber url specified by your CDN).
This is the url that the player will use to play the live stream.
For instance, if using rtmp, set it to something like: rtmp://STREAMING_SERVER_HOST:PORT/STREAMING_APPLICATION/

```
# Configuration for the Live Schedule Service

# The streaming base url e.g. rtmp://streaming.server/live/
# This is mandatory if not using MHPearl capture agents
#live.streamingUrl=rtmp://streaming.server/live

# The same mime-type applies to all flavors and resolutions
live.mimeType=video/x-flv

# If a comma-separated list is informed, several resolutions will be generated for each flavor
live.resolution=1920x540,960x270

# If a comma-separated list is informed, several streams links will be generated, one for each
# resolution-targetFlavor combination.
# Default is presenter/delivery
#live.targetFlavors=presenter/delivery

# Possible variable substitutions:
# #{id} = media package id
# #{flavor} = type-subtype of flavor
# #{caName} = capture agent name
# #{resolution} = video resolution e.g. 1920x1080
#live.streamName=#{id}-#{flavor}.stream
live.streamName=#{caName}-#{flavor}.stream-#{resolution}

# The distribution service to use: download or aws.s3
live.distributionService=download
```

### Step 2: Configure the capture agent

Configure the capture agent to stream to your streaming server (or the publisher url specified by your CDN), using the same stream name specified in live.streamName.

####Example

If:

- live.streamingUrl=rtmp://STREAMING_SERVER_HOST:PORT/STREAMING_APPLICATION
- live.streamName=#{caName}-#{flavor}.stream
- live.targetFlavors=presenter/delivery
- capture agent name: ca01

Then, the capture agent should stream to ('/' is replaced by '_'): rtmp://STREAMING_SERVER_HOST:PORT/STREAMING_APPLICATION/ca01-presenter_delivery.stream

Note: Please refer to your streaming server or CDN documentation for the correct syntax of the streaming url.
The _live.streamingUrl_ may be very different from the url the capture agent streams to.
For instance, with Akamai, the url used by the player will be something like live.streamingUrl=rtmp://xyz.live.edgefcs.net/live/ and the capture agent's publish url something like rtmp://a.bcd.e.akamaientrypoint.net/EntryPoint. The stream name should always match.

### Step 3: Configure the Workflow

When scheduling a live event via the admin UI, the workflow needs to have the _publishLive_ configuration set to true (this is already included in the sample workflows).
If not using the sample opencast workflows, add to the `<configuration_panel>`:

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








