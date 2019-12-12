AmberScript Transcription Service
=================================

Overview
--------

The AmberScriptTranscriptionService uses the AmberScript Transcription API to transcribe audio files.
Audio will get extracted from an opencast recording video file and sent to the AmberScript server to be processed.
AmberScriptTranscriptionService will periodically check for a transcription result.
Depending on your audio length and jobtype chosen, transcribing will take some time.
When the transcription result is ready the service will transform it to VTT format and attach it to the recording.
The recording will be available prior when its workflow finishes.
As soon as the transcription gets attached, the Video will be able to be played back using transcriptions.


Configuration
-------------

### Step 1: Get AmberScript API key

* Contact AmberScript via https://amberscript.com

### Step 2: Configure AmberscriptTranscriptionService

Edit `opencast/etc/org.opencastproject.transcription.amberscript.AmberscriptTranscriptionService.cfg`:

* Set `enabled=true` to enable the service.
* Set API key `client.key=__YOU-API-KEY__`. This is mandatory.
* Change options to your liking.

### Step 3: Include workflow operations into your workflow

Integrate AmberScript workflow operations by including the provided workflow file `amberscript-start-transcription.xml`
into your existing workflow:

```
<operation
  id="include"
  description="Start AmberScript Transcription">
  <configurations>
    <configuration key="workflow-id">amberscript-start-transcription</configuration>
  </configurations>
</operation>
```

Workflow Operations
-------------------

* [amberscript-start-transcription](../workflowoperationhandlers/amberscript-start-transcription-woh.md)
* [amberscript-attach-transcription](../workflowoperationhandlers/amberscript-attach-transcription-woh.md)

