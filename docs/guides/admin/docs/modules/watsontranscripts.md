Transcripts (Automated by IBM Watson)
=====================================

Overview
--------

The IBMWatsonTranscriptionService invokes the IBM Watson Speech-to-Text service via REST API to translate audio to
 text.

During the execution of an Opencast workflow, an audio file is extracted from one of the presenter videos and
sent to the IBM Watson Speech-to-Text service. When the results are received, they are converted to the desired caption format and attached to the media package.

Workflow 1 runs:

  - Audio file created
  - Watson Speech-to-Text job started
  - Workflow finishes

Translation finishes, callback with results is received, and workflow 2 is started.

Workflow 2 runs:

  - File with results is converted and attached to media package
  - Media package is republished with captions/transcripts

IBM Watson Speech-to-Text service documentation, including which languages are currently supported, can be found
 [here](https://www.ibm.com/watson/developercloud/doc/speech-to-text/index.html).

Configuration
-------------

### Step 1: Get IBM Watson credentials

* [Create a 30-day trial acoount in IBM Bluemix](https://www.ibm.com/cloud-computing/bluemix)
* [Get service credentials](https://www.ibm.com/watson/developercloud/doc/common/getting-started-credentials.html)

### Step 2: Configure IBMWatsonTranscriptionService

Edit  _etc/org.opencastproject.transcription.ibmwatson.IBMWatsonTranscriptionService.cfg_:

- Set _enabled_=true
- Use service credentials obtained above to set _ibm.watson.user_ and _ibm.watson.psw_
- Enter the appropriate language model in _ibm.watson.model_, if not using the default (_en-US_BroadbandModel_)
- In _workflow_, enter the workflow definition id of the workflow to be used to attach the generated 
transcripts/captions
- Enter a _notification.email_ to get job failure notifications. If not entered, the email in 
etc/custom.properties (org.opencastproject.admin.email) will be used. Configure the SmtpService.
If no email address specified in either _notification.email_ or _org.opencastproject.admin.email_,
email notifications will be disabled. 

```
# Change enabled to true to enable this service. 
enabled=true

# User obtained when registering with the IBM Watson Speech-to_text service 
ibm.watson.user=<SERVICE_USER>

# Password obtained when registering with the IBM Watson Speech-to_text service 
ibm.watson.password=<SERVICE_PSW>

# Language model to be used. See the IBM Watson Speech-to-Text service documentation
# for available models. If empty, the default will be used ("en-US_BroadbandModel").
#ibm.watson.model=

# Workflow to be executed when results are ready to be attached to media package.
#workflow=attach-watson-transcription
  
# Interval the workflow dispatcher runs to start workflows to attach transcripts to the media package
# after the transcription job is completed.
# (in seconds) Default is 1 minute.
#workflow.dispatch.interval=60
 
# How long it should wait to check jobs after their start date + track duration has passed.
# The default is 10 minutes. This is only used if we didn't get a callback from the
# ibm watson speech-to-text service.
# (in seconds)
#completion.check.buffer=600

# How long to wait after a transcription is supposed to finish before marking the job as 
# canceled in the database. Default is 2 hours.
# (in seconds)
#max.processing.time=7200

# How long to keep result files in the working file repository in days.
# The default is 7 days.
#cleanup.results.days=7

# Email to send notifications of errors. If not entered, the value from
# org.opencastproject.admin.email in custom.properties will be used.
#notification.email=
```

### Step 3: Add encoding profile for extracting audio

The IBM Watson Speech-to-Text service has limitations on audio file size. Try using the encoding profile suggested in 
etc/encoding/watson-audio.properties.

### Step 4: Add workflow operations and create new workflow

Add the following operations to your workflow. We suggest adding them after the media package is
published so that users can watch videos without having to wait for the transcription to finish, but it
depends on your use case. The only requirement is to take a snapshot of the media package so that
the second workflow can retrieve it from the Asset Manager to attach the caption/transcripts.  

``` xml
<!-- Extract audio from one of the presenter videos -->

<operation
  id="compose"
  fail-on-error="true"
  exception-handler-workflow="partial-error"
  description="Extract audio for transcript generation">
  <configurations>
    <configuration key="source-tags">engage-download</configuration>
    <configuration key="target-flavor">audio/ogg</configuration>
    <!-- The target tag 'transcript' will be used in the next 'start-watson-transcription' operation -->
    <configuration key="target-tags">transcript</configuration>
    <configuration key="encoding-profile">audio-opus</configuration>
    <!-- If there is more than one file that match the source-tags, use only the first one -->
    <configuration key="process-first-match-only">true</configuration>
  </configurations>
</operation>

<!-- Start IBM Watson recognitions job -->

<operation
  id="start-watson-transcription"
  fail-on-error="true"
  exception-handler-workflow="partial-error"
  description="Start IBM Watson transcription job">
  <configurations>
    <!--  Skip this operation if flavor already exists. Used for cases when mp already has captions. -->
    <configuration key="skip-if-flavor-exists">captions/timedtext</configuration>
    <!-- Audio to be translated, produced in the previous compose operation -->
    <configuration key="source-tag">transcript</configuration>
  </configurations>
</operation>

```

Create a workflow that will add the generated caption/transcript to the media package and republish it.
A sample one can be found in etc/workflows/attach-watson-transcripts.xml

Workflow Operations
-------------------

- [start-watson-transcription](../workflowoperationhandlers/start-watson-transcription-woh.md)
- [attach-watson-transcription](../workflowoperationhandlers/attach-watson-transcription-woh.md)
