Transcripts by Nibity
=====================

Overview
--------

The NibityTranscriptionService invokes the Nibity transcription service via REST API to transcribe and caption
videos.

During the execution of an Opencast workflow, a video file is extracted from one of the presenter videos and sent to
the Nibity transcription service. When the results are received, they are attached to the media package.

Workflow 1 runs:

* Submission media file created (video)
* Nibity transcription job started
* Workflow finishes

Translation finishes, callback with results is received, and workflow 2 is started.

Workflow 2 runs:

* File with results is converted and attached to media package
* Media package is republished with captions/transcripts

For more information on the Nibity transcription and captioning service, see https://nibity.com/

Configuration
-------------

### Step 1: Get Nibity credentials

* Contact Nibity via https://nibity.com/contact-us/ or support@nibity.com for a client ID and key

### Step 2: Configure NibityTranscriptionService

Edit `etc/org.opencastproject.transcription.nibity.NibityTranscriptionService.cfg`:

* Set _enabled_=true
* Use service credentials obtained above to set _nibity.client.id_ and _nibity.client.key_
* Update any of the optional properties

```
# Change enabled to true to enable this service.
enabled=true

# Workflow to be executed when results are ready to be attached to media package.
workflow=attach-nibity-transcripts

# Nibity API Client ID and KEY supplied by nibity.com
nibity.client.id=ID
nibity.client.key=KEY

# Interval the workflow dispatcher runs to start workflows to attach transcripts to the media package
# after the transcription job is completed.
# (in seconds) Default is 1 minute.
#workflow.dispatch.interval=60

# How long to wait after a transcription is supposed to finish before marking the job as
# canceled in the database. Default is 48 hours.
# (in seconds)
#max.overdue.time=172800

# How long to keep result files in the working file repository in days.
# The default is 7 days.
#cleanup.results.days=7

# Remove submission media files immediately after a submission
# Default is true. If false, media files will be removed after cleanup.results.days
#cleanup.submission=true

# Email to send notifications of errors. If not entered, the value from
# org.opencastproject.admin.email in custom.properties will be used.
#notification.email=
```

### Step 3: Add workflow operations and create new workflow

Add the following operations to your workflow. We suggest adding them after the media package is
published so that users can watch videos without having to wait for the transcription to finish, but it
depends on your use case. The only requirement is to take a snapshot of the media package so that
the second workflow can retrieve it from the Asset Manager to attach the caption/transcripts.

``` xml
<!-- Start Nibity recognitions job -->

<operation
  id="start-nibity-transcription"
  fail-on-error="true"
  exception-handler-workflow="partial-error"
  description="Start Nibity transcription job">
  <configurations>
    <!--  Skip this operation if flavor already exists. Used for cases when mp already has captions. -->
    <configuration key="skip-if-flavor-exists">captions/vtt+en</configuration>
    <!-- Video to be captioned, produced in the previous compose operation -->
    <configuration key="source-tag">transcript</configuration>
  </configurations>
</operation>

```

A workflow to add the generated caption/transcript to the media package and republish it is provided
as `etc/workflows/nibity-attach-transcripts.xml`.

If re-submitting requests is desired in case of failures, create a workflow that will start a transcription job.
A sample one can be found in `etc/workflows/retry-nibity-transcripts.xml`

Workflow Operations
-------------------

* [nibity-start-transcription](../workflowoperationhandlers/nibity-start-transcription-woh.md)
* [nibity-attach-transcription](../workflowoperationhandlers/nibity-attach-transcription-woh.md)

