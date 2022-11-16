Transcripts (Automated by Microsoft Azure)
========================================

Overview
--------

The Microsoft Azure Transcription Service invokes the Azure Speech-to-Text service via its Java SDK to transcribe audio
 to text.

During the execution of an Opencast workflow, a file containing the audio to be transcribed is streamed to the
Microsoft Azure Speech-to-Text service. Results are converted to WebVTT or SRT format 
and attached to the media package.

**Note that because Speech-to-Text services require a significant amount of time to process a recording, 
we do not wait for it to finish before proceeding with the rest of Opencast's normal processing.
This means that the transcription process is run asynchronously.**

* Workflow 1 runs:
    * Speech-to-Text job is started
    * Workflow finishes

Speech-to-Text job finishes, workflow 2 is started.

* Workflow 2 runs:
    * File is attached to media package
    * Media package is republished with captions/transcripts

Microsoft Azure Speech-to-Text service documentation, including which languages are currently supported, can be found
 [here](https://docs.microsoft.com/en-us/azure/cognitive-services/speech-service/language-support?tabs=speechtotext#speech-to-text).


Configuration
-------------

### Step 1: Get Azure subscription credentials

* [Create an Azure subscription](https://azure.microsoft.com/en-US/free/cognitive-services/)
* [Create a speech resource](https://portal.azure.com/#create/Microsoft.CognitiveServicesSpeechServices)
* Get the subscription key and region. After your Speech resource is deployed, select 'Go to resource' to view and 
manage keys. For more information about Cognitive Services resources, see 
[here](https://docs.microsoft.com/en-us/azure/cognitive-services/cognitive-services-apis-create-account?tabs=multiservice%2Clinux#get-the-keys-for-your-resource)

### Step 2: Install GStreamer

The Microsoft speech-to-text client uses GStreamer to transcode audio stream.
Therefore, GStreamer must be installed on your Opencast admin node.
On Debian based systems, you can do that by running:

```shell
sudo apt-get install libgstreamer1.0-0 gstreamer1.0-plugins-base gstreamer1.0-plugins-good gstreamer1.0-plugins-bad gstreamer1.0-plugins-ugly
```

On Red Hat based systems, run:

```shell
sudo yum install gstreamer1 gstreamer1-plugins-base gstreamer1-plugins-good gstreamer1-plugins-bad-free gstreamer1-plugins-ugly-free
```


### Step 3: Configure the Microsoft Azure Transcription Service

Edit  _etc/org.opencastproject.transcription.microsoftazure.MicrosoftAzureTranscriptionServiec.cfg_:

* Set _enabled_=true
* Use service credentials obtained above to set _subscription.key_ and _region_
* All other settings are optional and explained in the config file


### Step 4: Add a workflow operations or create new workflow to start transcription

The workflow below is a minimal working example for an event that has a video file in the `presenter/source` flavor.
You do not necessarily need an extra workflow, and instead you can integrate the parts you need into your own. The
relevant operations are `microsoft-azure-start-transcription` to start the transcription and `snapshot`, so a second
workflow can retrieve the mediapackage to attach captions/transcriptions

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<definition xmlns="http://workflow.opencastproject.org">

  <id>microsoft-azure-transcription</id>
  <title>Microsoft Azure Transcription Testing Workflow</title>
  <tags>
    <tag>upload</tag>
    <tag>schedule</tag>
    <tag>archive</tag>
  </tags>
  <displayOrder>100</displayOrder>
  <description>
    Microsoft Azure Transcription Testing Workflow
  </description>

  <operations>

    <operation
        id="defaults"
        description="Applying default configuration values">
      <configurations>
        <!-- The language code for the language spoken in the source file -->
        <configuration key="language-code">de-DE</configuration>
      </configurations>
    </operation>

    <!-- Start Microsoft Azure transcription job -->
    <operation
        id="microsoft-azure-start-transcription"
        fail-on-error="true"
        exception-handler-workflow="partial-error"
        description="Start Microsoft Azure transcription job">
      <configurations>
        <!--  Skip this operation if flavor already exists. Used for cases when mediapackage already has captions. -->
        <configuration key="skip-if-flavor-exists">captions/vtt+${language-code}</configuration>
        <configuration key="language-code">${language-code}</configuration>
        <configuration key="source-flavor">presenter/source</configuration>
      </configurations>
    </operation>

    <!-- Publish preview internal -->
    <operation
        id="publish-configure"
        exception-handler-workflow="partial-error"
        description="Publish to preview publication channel">
      <configurations>
        <configuration key="download-source-flavors">*/source</configuration>
        <configuration key="channel-id">internal</configuration>
        <configuration key="url-pattern">http://localhost:8080/admin-ng/index.html#/events/events/${event_id}/tools/playback</configuration>
        <configuration key="check-availability">false</configuration>
      </configurations>
    </operation>

    <!-- Publish to engage player -->
    <operation
        id="publish-engage"
        max-attempts="2"
        fail-on-error="true"
        exception-handler-workflow="partial-error"
        description="Publishing to Engage">
      <configurations>
        <configuration key="download-source-flavors">dublincore/*,security/*,*/source</configuration>
        <configuration key="download-source-tags">engage-download</configuration>
        <configuration key="streaming-source-tags">engage-streaming</configuration>
        <configuration key="check-availability">false</configuration>
      </configurations>
    </operation>


    <!-- Save the language-code to the mediapackage, so the attach workflow knows how to flavor the transcription file -->
    <operation
        id="export-wf-properties"
        fail-on-error="false"
        description="Export workflow settings to Java properties file">
      <configurations>
        <configuration key="target-flavor">wf-properties/language</configuration>
        <configuration key="target-tags">archive</configuration>
        <configuration key="keys">language-code</configuration>
      </configurations>
    </operation>

    <!-- Archive the current state of the media package -->
    <operation
        id="snapshot"
        fail-on-error="true"
        exception-handler-workflow="partial-error"
        description="Archiving">
      <configurations>
        <configuration key="source-flavors">*/source,dublincore/*,security/*</configuration>
        <configuration key="source-tags">archive</configuration>
      </configurations>
    </operation>

    <!-- Clean up the working file repository -->
    <operation
        id="cleanup"
        fail-on-error="false"
        description="Cleaning up">
      <configurations>
        <configuration key="delete-external">true</configuration>
        <!-- FixMe Don't clean up ACLs until workflow service no longer looks for them in the WFR. -->
        <configuration key="preserve-flavors">security/*</configuration>
      </configurations>
    </operation>

  </operations>

</definition>

```

### Step 5: Add a workflow to attach transcriptions

A sample attach workflow that works together with the workflow from Step 3. Attaches the generated transcription
to the mediapackages and republishes it. Copy it into a new file under
`etc/workflows/microsoft-azure-attach-transcripts.xml` in your Opencast installation.

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<definition xmlns="http://workflow.opencastproject.org">
  <id>microsoft-azure-attach-transcripts</id>
  <title>Attach caption/transcripts generated by Microsoft Azure</title>
  <description>Attach automated transcription generated by the Microsoft Azure service. This is an internal workflow, started by the Transcription Service.
  </description>
  <configuration_panel />

  <operations>

    <!-- Import the language code used in the start workflow -->
    <operation
        id="import-wf-properties"
        fail-on-error="true"
        exception-handler-workflow="partial-error"
        description="Load language setting">
      <configurations>
        <configuration key="source-flavor">wf-properties/language</configuration>
      </configurations>
    </operation>

    <!-- Attach caption/transcript -->
    <operation id="microsoft-azure-attach-transcription"
               fail-on-error="true"
               exception-handler-workflow="partial-error"
               description="Attach captions/transcription">
      <configurations>
        <!-- This is filled out by the transcription service when starting this workflow -->
        <configuration key="transcription-job-id">${transcriptionJobId}</configuration>
        <!-- Set the flavor to something the Paella player will parse -->
        <!-- Using the language code here is not necessary, but nice to have -->
        <configuration key="target-flavor">captions/vtt+${language-code}</configuration>
        <configuration key="target-tag">archive</configuration>
      </configurations>
    </operation>

    <!-- Publish to engage player -->
    <operation id="publish-engage"
               fail-on-error="true"
               exception-handler-workflow="partial-error"
               description="Distribute and publish to engage server">
      <configurations>
        <configuration key="download-source-flavors">dublincore/*,security/*,captions/*</configuration>
        <configuration key="strategy">merge</configuration>
        <configuration key="check-availability">false</configuration>
      </configurations>
    </operation>

    <!-- Archive media package -->
    <operation id="snapshot"
               fail-on-error="true"
               exception-handler-workflow="partial-error"
               description="Archive media package">
      <configurations>
        <configuration key="source-flavors">*/*</configuration>
      </configurations>
    </operation>

    <!-- Clean up work artifacts -->
    <operation
        id="cleanup"
        fail-on-error="false"
        description="Remove temporary processing artifacts">
      <configurations>
        <configuration key="delete-external">true</configuration>
        <!-- FixMe Don't clean up ACLs until workflow service no longer looks for them in the WFR. -->
        <configuration key="preserve-flavors">security/*</configuration>
      </configurations>
    </operation>

  </operations>

</definition>

```

Workflow Operations
-------------------

* [microsoft-azure-attach-transcription](../workflowoperationhandlers/microsoft-azure-attach-transcription-woh.md)
* [microsoft-azure-start-transcription](../workflowoperationhandlers/microsoft-azure-start-transcription-woh.md)