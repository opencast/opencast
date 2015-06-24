# Video Editor: Architecture

## Modules Of The Videoeditor
The Videoeditor consists of the following moduls. Additional to this there is a Workflow Operation Handler within the Conductor module that provides the UI elements for the Video Editor. 

 - matterhorn-silencedetection-api
   - API for the silence detection
 - matterhorn-silencedetection-impl
   - Implementation of the silence detection service
   - Provides a SMIL file that can be used by the Video Editor UI or the Video Editor service to create a new cutted file.
 - matterhorn-silencedetection-remote
   - Remote implementation of the silence detection service to enable load balancing in a distributed setup.
 - matterhorn-smil-api
   - API for the SMIL service
 - matterhorn-smil-impl
   - The SMIL service allows creation and manipulation of SMIL files. This is more or less a helper class to create consistent SMIL files.
 - matterhorn-videoeditor-api
   - The API for the Video Editor which takes a SMIL file as an input to create a cutted version of the media files.
 - matterhorn-videoeditor-impl
   - The Video Editor service creates new media files that will be cutted based on the information provided in a SMIL file. In the current implementation GStreamer with the gnonlin module is used to process the files.
 - matterhorn-videoeditor-remote
   - Remote implementation of the video editor service to enable load balancing in a distributed setup.

Several other changes have been made on other Matterhorn modules to provide a better user experience for the video editor (i.e. byte-range request on the working-file-repository).

## Edit List Format
The video editor uses SMIL 3.0 as a standardized Data format for the edit lists (cutting information). Some conventions and namespace extensions have been made to make sure that Matterhorn is able to find the files.

 - As we usually have two (or more) parallel media files, these files are grouped in a `<par>`-element which forms a segment that should be included in the resulting video.  This means the included `<video>`-files will be played in parallel.
 - The clipBegin and clipEnd attributes a provided as milliseconds. Usually these should be identical for all `<videos>` within a `<par>`.
For each segment a `<par>` is created.
 - In the result of the silence detection segments with silence are omitted within the SMIL files, so only segments within the SMIL doc will be in the resulting video.
 - The segments within the SMIL file will be in the order they are written down. If the sequence of the segments is changed, the sequence within the resulting video is changed too.
 
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

## Workflow Operations

### Waveform Operation
The **waveform** operation creates an image showing the temporal audio activity within the recording. This is be done with a probably well known waveform (see example image). 

![Waveform image example](../configuration/workflowoperationhandler/waveform.png)

The operation does not need an additional module, as it is not very work intensive to create such an image. The operation needs and audio-only file to create the image and it provides an PNG image.

Input parameter is the source-flavor of the audio files for which a waveform should be created. The *-operator can be used if the waveform should be created for all flavors with a certain subtypes (like "audio" in our example).

The output-parameter is target-flavor which should use the *-operator if it was used in the source-flavor too.

**Waveform Operation Template**

    <operation
      id="waveform"
      if="${trimHold}"
      fail-on-error="false"
      description="Generating waveform">
      <configurations>
        <configuration key="source-flavor">*/audio</configuration>
        <configuration key="target-flavor">*/waveform</configuration>
      </configurations>
    </operation>

### Silence Operation
The **silence** operation performs a silence detection on an audio-only input file. The operation needs the silence detection API and impl (or remote in a distributed system) modules to be installed to process the request.

The input parameters are source-flavors that takes one flavor/sub-type or multiple input flavors with the *-operator followed by the sub-type, and reference-tracks-flavour where the subtype of the media files that should be included in the provided SMIL file will be set. The * should not be modified here. In most cases it is not important which reference-tracks-flavour is selected as long as all relevant flavors are available within this feature. "preview" is not a bad choice as all files available within the video editor UI are also available with this flavor, unlike "source" where not all flavors may be available, as some recorders record all streams to one file and the tracks are separated afterwards. The editor operation afterwards will anyway try to select the best available quality.

The output parameter is smil-flavor-subtype which provides the modificatory for the flavor subtype after this operation. The main flavor will be consistent and only the subtype will be replaced. 

The output of this operation is a SMIL file (see the example above).

**Silence Operation Template**

    <operation
      id="silence"
      if="${trimHold}"
      fail-on-error="false"
      description="Executing silence detection">
      <configurations>
        <configuration key="source-flavors">*/audio</configuration>
        <configuration key="smil-flavor-subtype">smil</configuration>
        <configuration key="reference-tracks-flavor">*/preview</configuration>
      </configurations>
    </operation>

### Editor Operation
The **editor** operation provides the UI for editing trim hold state and processes the edited files. This operation needs the videoeditor API and impl (or remote on distributed systems) to be installed.

The input parameters are:

 - source-flavors: the subtype of all media files in the best available quality and in a codec that can be processed by the videoeditor modules. The *-should usually not be changed, as tracks can be excluded in the editor UI too, only the subtype is important. All needed videos should be available within this flavor.
 - preview-flavours: the subtype of the media files that should be used for the preview player. This is an HTML5 player so the coded can be H.264 or WebM based on the browser. The main flavor should be the same as in source-flavors.
 - smil-flavors: the smil file(s) that should be used as a proposal within the editor UI. If * is used presenter/smil will be favored, if this is not available the first in the list will be used.
 - skipped-flavors: the flavor of the files that should be used if this workflow-operation is skipped.

The output parameters are:
 - target-smil-flavor: only a unique flavor is allowed here, as this is the file that the editor UI writes and that will be taken for processing the edited files afterwards.
 - target-flavor-subtype: the flavor-subtype that will be used for all media files created in this operation.

**Editor Operation Template**

    <operation
      id="editor"
      if="${trimHold}"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Waiting for user to review / video edit recording">
      <configurations>
        <configuration key="source-flavors">*/work</configuration>
        <configuration key="preview-flavors">*/preview</configuration>
        <configuration key="skipped-flavors">*/preview</configuration>
        <configuration key="smil-flavors">*/smil</configuration>
        <configuration key="target-smil-flavor">episode/smil</configuration>
        <configuration key="target-flavor-subtype">trimmed</configuration>
      </configurations>
    </operation>
 
## Including The Video Editor To The Workflow Definition File

Including the Video Editor with the silence detection into the needs some changes in the default workflow. Several of the steps here are inherited from the trim-operations and the workflow it was included too. We assume that you set ${trimHold} variable like in the current workflow definitions with trimming.

 1. The prepare-av operations has to be adopted. Gstreamer/gnonlin is kind of picky on the codec that it supports. So the media file has to be re-encoded in the beginning of the workflow. The prepare-av encoding profiles (av.work and mux-av.work) have been updated in the Video Editor branch for this. Within the prepare-av operation in the workflow-definition XML-file rewriting the file should be forced:

  **Changes in the workflow definition**
    
  `<configuration key="rewrite">true</configuration>`

  `<configuration key="promiscuous-audio-muxing">true</configuration>`

 2. The preview videos have to be created. These can be in H.264 (for Safari, IE, Chrome) or WebM (for Firefox, Opera or Chrome) codec. Encoding profiles for WebM are provided in the video editor branch and are used in the examples. This operation should be after the prepare-av operation.

  **Workflow operation to create WebM preview videos**

    <operation
      id="compose"
      if="${trimHold}"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Encoding presenter (camera) video for videoeditor preview">
      <configurations>
        <configuration key="source-flavor">*/work</configuration>
        <configuration key="target-flavor">*/preview</configuration>
        <configuration key="encoding-profile">webm-preview.http</configuration>
      </configurations>
    </operation>

 3. An audio-only file has to be composed for the waveform and silence operation. This operation should be after the prepare-av operation.
Workflow operation to compose the audio-only file(s)

    <operation
      id="compose"
      if="${trimHold}"
      fail-on-error="false"
      description="Extracting audio for waveform generation">
      <configurations>
        <configuration key="source-flavor">*/work</configuration>
        <configuration key="target-flavor">*/audio</configuration>
        <configuration key="encoding-profile">audio.wav</configuration>
      </configurations>
    </operation>

 4. The waveform operation should be included. See above for the XML-code for this operation. The audio-only file should already be available.
 5. The silence detection should be done. See above for the XML-code for this operation. The audio-only file should already be available.
 6. After all previous operations have been done the editor can be included. See above for the XML-code for this operation. 
 7. You may consider to tag the trimmed files for archiving. Then you should include this operation after the editor:

  **Tagging trimmed files for the archive**

    <operation
      id="tag"
      description="Tagging media for archival">
      <configurations>
        <configuration key="source-flavors">*/trimmed</configuration>
        <configuration key="target-tags">+archive</configuration>
      </configurations>
    </operation>

  You could check, if you want to archive the source media too, or remove the source-flavors from the previous tagging operations.

  8. The rest of the workflow definition can be kept as it is, the input flavor subtype for the trimmed files in other operations is "/trimmed" if you follow the naming in this example. 

The default *compose-distribute-publish.xml* workflow definition within the Video Editor branch has already been updated to include the editor instead of the trim-hold state. The trim operation is not overwritten with the video editor but could still be used.
