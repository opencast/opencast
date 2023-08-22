# Video Editor: Architecture

## Modules Of The Video Editor

The Video Editor consists of the following modules. Additional to this there is a Workflow Operation Handler within the
Conductor module that provides the UI elements for the Video Editor.

* silencedetection-api
    * API for the silence detection
* silencedetection-impl
    * Implementation of the silence detection service
    * Provides a SMIL file that can be used by the Video Editor UI or the Video Editor service to create a new cut
      file.
* silencedetection-remote
    * Remote implementation of the silence detection service to enable load balancing in a distributed setup.
* smil-api
    * API for the SMIL service
* smil-impl
    * The SMIL service allows creation and manipulation of SMIL files. This is more or less a helper class to create
      consistent SMIL files.
* videoeditor-api
    * The API for the Video Editor which takes a SMIL file as an input to create a cut version of the media files.
* videoeditor-ffmpeg-impl
    * The Video Editor service creates new media files that will be cut based on the information provided in a SMIL
      file. In the current implementation GStreamer with the gnonlin module is used to process the files.
* videoeditor-remote
    * Remote implementation of the video editor service to enable load balancing in a distributed setup.

Several other changes have been made on other Opencast modules to provide a better user experience for the video
editor (i.e. byte-range request on the working-file-repository).

## Edit List Format

The video editor uses SMIL 3.0 as a standardized Data format for the edit lists (cutting information). Some conventions
and namespace extensions have been made to make sure that Opencast is able to find the files.

* As we usually have two (or more) parallel media files, these files are grouped in a `<par>`-element which forms a
  segment that should be included in the resulting video.  This means the included `<video>`-files will be played in
  parallel.
* The clipBegin and clipEnd attributes a provided as milliseconds. Usually these should be identical for all `<videos>`
  within a `<par>`.  For each segment a `<par>` is created.
* In the result of the silence detection segments with silence are omitted within the SMIL files, so only segments
  within the SMIL doc will be in the resulting video.
* The segments within the SMIL file will be in the order they are written down. If the sequence of the segments is
  changed, the sequence within the resulting video is changed too.

**Example SMIL file**

    <smil xmlns="http://www.w3.org/ns/SMIL" baseProfile="Language" version="3.0" xml:id="s-524c7815-4520-48e4-bb5e-94dcfdb3229f">
        <head xml:id="h-03b31c8d-68cf-49ea-8bae-d94abddf8f09">
            <meta name="track-duration" content="6000841ms" xml:id="meta-32069ddb-351d-4dca-a742-b9be490080f8"/>
            <paramGroup xml:id="pg-bb1e4ab7-08e8-4ae7-9da8-1f6d46b56387">
                <param value="9f373445-5f46-4bdd-8d93-dca5e1094c38" name="track-id" valuetype="data" xml:id="param-d509b427-b239-4c4b-985a-f8b4ea31bbfb"/>
                <param value="http://my.server.tld/files/mediapackage/98c91b97-51de-46ae-992d-c497798f16c8/9f373445-5f46-4bdd-8d93-dca5e1094c38/lecturer.mp4" name="track-src" valuetype="data" xml:id="param-411e0015-af0e-463c-898d-9a2bc594df46"/>
                <param value="presenter/preview" name="track-flavor" valuetype="data" xml:id="param-5ea022cd-189d-420f-9cea-4f6775af285e"/>
            </paramGroup>
            <paramGroup xml:id="pg-35035f9c-ab9a-49a7-9ef8-9825190b949b">
                <param value="9af21dad-cb92-4e18-bc4c-b8c9b7ce4e2f" name="track-id" valuetype="data" xml:id="param-c3c427ad-ef8a-4a71-9b0c-9208dd8a6bed"/>
                <param value="http://my.server.tld/files/mediapackage/98c91b97-51de-46ae-992d-c497798f16c8/9af21dad-cb92-4e18-bc4c-b8c9b7ce4e2f/screen.mp4" name="track-src" valuetype="data" xml:id="param-c15e1ed7-f773-456d-a007-fc237d9e0665"/>
                <param value="presentation/preview" name="track-flavor" valuetype="data" xml:id="param-97d5b5ac-1258-4267-a013-dc3882d7e242"/>
            </paramGroup>
        </head>
        <body xml:id="b-c233c9ef-42d9-4f50-a1d2-29e3bbff003d">
            <par xml:id="par-7955133a-bcbe-40f8-87fd-47e78b3357c0">
                <video src="http://my.server.tld/files/mediapackage/98c91b97-51de-46ae-992d-c497798f16c8/9f373445-5f46-4bdd-8d93-dca5e1094c38/lecturer.mp4" paramGroup="pg-bb1e4ab7-08e8-4ae7-9da8-1f6d46b56387" clipEnd="5522400ms" clipBegin="157880ms" xml:id="v-61f5d0ee-dd36-4b1d-af3d-3f09f8807179"/>
                <video src="http://my.server.tld/files/mediapackage/98c91b97-51de-46ae-992d-c497798f16c8/9af21dad-cb92-4e18-bc4c-b8c9b7ce4e2f/screen.mp4" paramGroup="pg-35035f9c-ab9a-49a7-9ef8-9825190b949b" clipEnd="5522400ms" clipBegin="157880ms" xml:id="v-c68260e7-fd0d-4df6-8696-cc475ab3b3f8"/>
            </par>
        </body>
    </smil>
