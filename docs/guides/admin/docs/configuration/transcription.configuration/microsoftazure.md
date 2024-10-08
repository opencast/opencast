# Microsoft Azure Transcription Engine
## Overview
Microsoft Azure Opencast transcription service uses the Microsoft Azure Speech Service API to create a transcript from audio track. The transcription is done asynchronously to speed up processing. When the result is generated, an attach-transcript workflow will be started to archive and publish transcript. To get more in touch with Microsoft Azure Speech Service API, read [documentation here](https://learn.microsoft.com/en-us/azure/ai-services/speech-service/batch-transcription).

Note: You must have an active subscription to use Microsoft Azure Speech Services.

## Configuration

### Step 1: Get Azure subscription credentials
* [Create an Azure subscription](https://azure.microsoft.com/en-US/free/cognitive-services/)
* [Create a storage account](https://learn.microsoft.com/en-us/azure/storage/common/storage-account-create)
* Get storage account access key. Go to [Azure Portal](https://portal.azure.com) > `Storage accounts`, select your storage account, choose `Security + networking` > `Access keys.` Copy the key.
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
* Review and edit all other configurations in this file

### Step 3: Add a workflow operations or create new workflow to start transcription

Edit workflow to start transcription, e.g. `etc/workflows/partial-publish.xml`. You have to add the `microsoft-azure-start-transcription` operation right after the step creating of the final cut of the media files. This operation may look like

```xml
<!-- This is a typical operation to generate final cut -->
<!-- of the media files. -->
<operation
  id="editor"
  …
</operation>

<!-- This operation will start the transcription job -->
<operation
  id="microsoft-azure-start-transcription"
  fail-on-error="true"
  exception-handler-workflow="partial-error"
  description="Start Microsoft Azure transcription job">
  <configurations>
    <configuration key="source-flavors">*/trimmed</configuration>
    <!-- Skip this operation if flavor already exists. -->
    <!-- Used for cases when mediapackage already has captions. -->
    <configuration key="skip-if-flavor-exists">captions/*</configuration>
    <configuration key="audio-extraction-encoding-profile">transcription-azure.audio</configuration>
  </configurations>
</operation>
```

For more options please consult the [documentation](../../workflowoperationhandlers/microsoft-azure-start-transcription-woh.md).

### Step 4: Add a workflow to attach transcriptions

A sample attach transcript workflow that is preconfigured in the configuration from Step 2. Attaches the generated transcription to the mediapackage, archives and republishes it. Copy it into a new file under `etc/workflows/microsoft-azure-attach-transcription.xml` in your Opencast installation.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definition xmlns="http://workflow.opencastproject.org">
  <id>microsoft-azure-attach-transcription</id>
  <title>Attach Transcription from Microsoft Azure</title>
  <description>Publish and archive transcription from Microsoft Azure Speech Services.</description>
  <operations>

    <operation
      id="microsoft-azure-attach-transcription"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Attach transcription from Microsoft Azure">
      <configurations>
        <!-- This is filled out by the transcription service when starting this workflow -->
        <configuration key="transcription-job-id">${transcriptionJobId}</configuration>
        <!-- Set the flavor to something the Paella player will parse -->
        <configuration key="target-flavor">captions/source</configuration>
        <configuration key="target-tags">archive, ${transcriptionLocaleTag!}</configuration>
      </configurations>
    </operation>

    <operation
      id="snapshot"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Archive transcription">
      <configurations>
        <configuration key="source-tags">archive</configuration>
      </configurations>
    </operation>

    <operation
      id="tag"
      description="Tagging captions for publishing">
      <configurations>
        <configuration key="source-flavors">captions/source</configuration>
        <configuration key="target-flavor">captions/delivery</configuration>
        <configuration key="target-tags">-archive</configuration>
        <configuration key="copy">true</configuration>
      </configurations>
    </operation>

    <operation
      id="publish-engage"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Distribute and publish to engage server">
      <configurations>
        <configuration key="download-source-flavors">captions/delivery</configuration>
        <configuration key="strategy">merge</configuration>
        <configuration key="check-availability">false</configuration>
      </configurations>
    </operation>

    <operation
      id="cleanup"
      fail-on-error="false"
      description="Cleaning up">
      <configurations>
        <configuration key="preserve-flavors">security/*</configuration>
        <configuration key="delete-external">false</configuration>
      </configurations>
    </operation>
  </operations>
</definition>
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

### Step 6: Enable transcription plugin

Transcription plugins are disabled by default. Enable it in the `etc/org.opencastproject.plugin.impl.PluginManagerImpl.cfg` configuration file setting the `opencast-plugin-transcription-services` to `true`.