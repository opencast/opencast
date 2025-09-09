# Microsoft Azure Transcription Engine
## Overview
Microsoft Azure Opencast transcription service uses the Microsoft Azure Speech Service API to create a transcript from an audio track. The transcription is done asynchronously to speed up processing. When the result is generated, an attach-transcript workflow will be started to archive and publish the transcript. To find out more about the Microsoft Azure Speech Service API, read [documentation here](https://learn.microsoft.com/en-us/azure/ai-services/speech-service/batch-transcription).

Note: You must have an active subscription to use Microsoft Azure Speech Services.

## Configuration

### Step 1: Get Azure subscription credentials
* [Create an Azure subscription](https://azure.microsoft.com/en-US/free/cognitive-services/)
* [Create a storage account](https://learn.microsoft.com/en-us/azure/storage/common/storage-account-create)
* Get the storage account access key. Go to [Azure Portal](https://portal.azure.com) > `Storage accounts`, select your storage account, choose `Security + networking` > `Access keys.` Copy the key.
* [Create a speech resource](https://portal.azure.com/#create/Microsoft.CognitiveServicesSpeechServices)
* Get the subscription key and region. After your Speech resource is deployed, select `Go to resource` to view and
  manage keys. For more information about Cognitive Services resources, see
  [here](https://docs.microsoft.com/en-us/azure/cognitive-services/cognitive-services-apis-create-account?tabs=multiservice%2Clinux#get-the-keys-for-your-resource)

### Step 2: Configure the Microsoft Azure Transcription Service
Edit `etc/org.opencastproject.transcription.microsoft.azure.MicrosoftAzureTranscriptionService.cfg`:

* Set `enabled`=`true`
* Set `azure_storage_account_name` to your storage account name
* Set `azure_account_access_key` to the storage account access key
* Set `azure_container_name` to a container name you want to use
* Set `azure_speech_services_endpoint` to your speech services endpoint
* Set `azure_cognitive_services_subscription_key` to the speech services subscription key
* Review the other configuration options in this file and edit as needed

### Step 3: Add a workflow operations or create new workflow to start transcription

Edit a workflow to start a transcription, e.g. `etc/workflows/partial-publish.xml`. You have to add the `microsoft-azure-start-transcription` operation right after the creation of the final cut of the media files. This operation may look like

```yaml
  # This is a typical operation to generate final cut
  # of the media files.
  - id: editor
   ...
  # This operation will start the transcription job
  - id: microsoft-azure-start-transcription
    fail-on-error: true
    exception-handler-workflow: partial-error
    description: Start Microsoft Azure transcription job
    configurations:
      - source-flavors: '*/trimmed'
      # Skip this operation if flavor already exists.
      # Used for cases when mediapackage already has captions.
      - skip-if-flavor-exists: captions/*
      - audio-extraction-encoding-profile: transcription-azure.audio

```

For more options please consult the [documentation](../../workflowoperationhandlers/microsoft-azure-start-transcription-woh.md).

### Step 4: Add a workflow to attach transcriptions

A sample attach transcript workflow that is preconfigured in the configuration from Step 2. Attaches the generated transcription to the mediapackage, archives and republishes it. Copy it into a new file under `etc/workflows/microsoft-azure-attach-transcription.xml` in your Opencast installation.

```yaml
id: microsoft-azure-attach-transcription
title: Attach Transcription from Microsoft Azure
description: |-
  Publish and archive transcription from Microsoft Azure Speech Services.
operations:
  - id: microsoft-azure-attach-transcription
    fail-on-error: true
    exception-handler-workflow: partial-error
    description: Attach transcription from Microsoft Azure
    configurations:
      # This is filled out by the transcription service when starting this workflow
      - transcription-job-id: ${transcriptionJobId}
      # Set the flavor to something the Paella player will parse
      - target-flavor: captions/source
      - target-tags: archive, ${transcriptionLocaleTag!}

  - id: snapshot
    fail-on-error: true
    exception-handler-workflow: partial-error
    description: Archive transcription
    configurations:
      - source-tags: archive

  - id: tag
    description: Tagging captions for publishing
    configurations:
      - source-flavors: captions/source
      - target-flavor: captions/delivery
      - target-tags: -archive
      - copy: true

  - id: publish-engage
    fail-on-error: true
    exception-handler-workflow: partial-error
    description: Distribute and publish to engage server
    configurations:
      - download-source-flavors: captions/delivery
      - strategy: merge
      - check-availability: false

  - id: cleanup
    fail-on-error: false
    description: Cleaning up
    configurations:
      - preserve-flavors: security/*
      - delete-external: false
```

All available options of the  `microsoft-azure-attach-transcription` operation are documented [here](../../workflowoperationhandlers/microsoft-azure-attach-transcription-woh.md).

### Step 5: Add audio extraction encoding profile

The audio track to transcript must be extracted from the media file and converted to a specific format for processing. This is done with encoding engine of Opencast. Put the encoding profile listed below into the file `etc/encoding/custom.properties`.

```cfg
# Microsoft Azure Speech Services accept limited audio formats
# See https://learn.microsoft.com/en-us/azure/cognitive-services/speech-service/batch-transcription-audio-data#supported-audio-formats
profile.transcription-azure.audio.name = extract audio stream for transcription
profile.transcription-azure.audio.input = visual
profile.transcription-azure.audio.output = audio
profile.transcription-azure.audio.jobload = 0.5
profile.transcription-azure.audio.suffix = .ogg
profile.transcription-azure.audio.mimetype = audio/ogg
profile.transcription-azure.audio.ffmpeg.command = -i #{in.video.path} \
    -vn -dn -sn -map_metadata -1 \
    -c:a libopus -b:a 24k -ac 1 -ar 16k \
    #{out.dir}/#{out.name}#{out.suffix}
```

### Step 6: Enable the transcription plugin

Transcription plugins are disabled by default. Enable it in the `etc/org.opencastproject.plugin.impl.PluginManagerImpl.cfg` configuration file by setting the `opencast-plugin-transcription-services` to `true`.
