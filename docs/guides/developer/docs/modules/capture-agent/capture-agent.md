Introduction
============

This guide describes the communication protocol between Opencast and any Capture Agent.  For the sake of simplicity, the
following variables are used throughout this guide:

- `$HOST` is your core's base URL
- `$AGENT_NAME` is the agent's name
- `$RECORDING_ID` is the recording's ID


Basic Rules
-----------

- The core *MUST NOT* attempt to connect to the agent, communication is always happening from  agent to core
- The agent *MUST* get the endpoint location from the service registry
- The agent *MUST* try to send its recording states to the core on a regular basis during recordings
- The agent *SHOULD* send its state to the core on a semi regular basis
- The agent *MAY* send its capabilities to the core on a semi regular basis
- The agent *MUST* attempt to update its calendaring data regularly
- The agent *MUST* must capture with all available inputs if no inputs are selected
- The agent *MAY* tell the core the address of its web interface


Quick Overview
--------------

The following list gives a short overview over the communication between agent and core. Remember, it is up to the agent
to initiate the connections. Also note that ideally some operations may run in parallel.

- request rest endpoints
- register agent and set status
- set the agents capabilities
- repeat:
    - while no recording
        - get schedule/calendar
    - set agent and recording state
    - start recording
    - set agent and recording state
    - update ingest endpoints
    - ingest recording
    - set agent and recording state


API Stability
-------------

The Opencast community takes care to avoid any disruptive modifications to the APIs described in this document to
prevent hardware integrations from breaking since they are notorious hard to fix. This means that you can assume the API
to be stable for a long time. The same is not true for other parts of the API and you should therefore avoid integrating
hardware with other parts of Opencast's API


Authentication
--------------

Opencast supports two types of authentication which can be used by capture agents:

- (Backend) HTTP Digest authentication which is historically used for machine-to-machine communication.
- (General) HTTP Basic (or session-based) authentication used for both front-end users and integrations.

HTTP Digest authentication is the legacy option for capture agents. It is still widely used today and will continue to
be supported. HTTP Digest is more complicated and has the disadvantage that users need to be specified separately in the
backend. There is a global HTTP Digest user with admin privileges but specific capture agent users with limited
privileges can be defined as well.

When using HTTP Digest authentication, you need to send the additional header `X-Requested-Auth: Digest`:

    curl --digest -u opencast_system_account:CHANGE_ME -H "X-Requested-Auth: Digest" \
      https://develop.opencast.org/info/me.json

HTTP Basic authentication can be used with users defined via web-interface or via any regular user provider. A request
using HTTP Basic does not need to specify any additional headers:

    curl -u admin:opencast https://develop.opencast.org/info/me.json

Generally, we recommend using HTTP Basic authentication since it's easier for adopters to manage capture agent users via
the admin interface and does not rely on hidden users while being less complicated at the same time.

Whatever authentication method you choose to implement – maybe even both, allowing users to choose for themselves –
please clearly specify what authentication you expect since users have to provide different types of users which can
easily lead to usability problems if not clearly marked.


Action Details
--------------

This is a detailed description of the steps described in the quick overview section.

For details about the REST endpoints, please *always* consult the Opencast REST documentation which can be found in the
top-right corner of the administrative user interface of each running Opencast instance. Note that most endpoints can
handle both JSON and XML although throughout this guide, for all examples, only JSON is used.


### Get Endpoint Locations From Service Registry

First, you need to get the locations of the REST endpoints to talk to. These information can be retrieved from the
central service registry. It is likely that Opencast is running as a distributed system which means you cannot expect
all endpoints on a single host.

Three endpoint locations need to be requested:

- The capture-admin endpoint to register the agent and set status and configuration: `org.opencastproject.capture.admin`
- The scheduler endpoint to get the current schedule for the agent from: `org.opencastproject.scheduler`
- The ingest endpoint to upload the recordings to once they are done: `org.opencastproject.ingest`


To ask for an endpoint you would send an HTTP GET request to

    ${HOST}/services/available.json?serviceType=<SERVICE>

A result would look like

    {
      "services" : {
        "service" : {
          "active" : true,
          "host" : "http://example.opencast.org:8080",
          "path" : "/capture-admin",
          ...
        }
      }
    }


These endpoints should be requested once when starting up the capture agent. While the capture-admin and scheduler
endpoints may then be assumed to never change during the runtime of the capture agent, the ingest endpoint may change
and the data should be re-requested every time before uploading (ingesting) data to the core.


### Register the Capture Agent and set Current Status

Once the endpoints to talk to are known, it is time to register the capture agent at the core so that scheduled events
can be added. This can be done by sending an HTTP POST request to:

    ${CAPTURE-ADMIN-ENDPOINT}/agents/<name>


…including the following data fields:

    state=idle
    address=http(s)://<ca-web-ui>


The name has to be a unique identifier of a single capture agent. Using the same name for several agents would mean
sharing the same schedule and status which in general should be avoided.

Sending this request will register the capture agent. After this, the capture agent should appear in the admin interface
and schedules can be added for this agent.

Setting `address` is optional. It can be used to link an administrative web
interface of a capture agent.


### Setting Agent Capabilities

Additional to registering, the agent may set its capabilities allowing users to select possible inputs in the user
interface of Opencast when scheduling events. The configuration may be set as XML or JSON representation of a Java
properties file and can be set via an HTTP POST request to:

    /capture-admin/agents/$AGENT_NAME/configuration


### Getting the Calendar/Schedule

The calendar can be retrieved by sending an HTTP GET request to

    ${SCHEDULER-ENDPOINT}/calendars?agentid=<name>

The format returned is iCal. The file contains all scheduled upcoming recordings the capture agent should handle.
The output will look like this (base64 encoded parts are shortened):

```ical
BEGIN:VCALENDAR
PRODID:Opencast Calendar File 0.5
VERSION:2.0
CALSCALE:GREGORIAN
BEGIN:VEVENT
DTSTAMP:20200616T124513Z
DTSTART:20200616T124200Z
DTEND:20200616T124400Z
SUMMARY:Demo event
UID:68b19d11-6aca-413e-b2b3-eda72eebc65a
LAST-MODIFIED:20200616T124119Z
DESCRIPTION:demo
LOCATION:my_capture_agent_name
ATTACH;FMTTYPE=application/xml;VALUE=BINARY;ENCODING=BASE64;X-APPLE-FILENAME=episode.xml:...
ATTACH;FMTTYPE=application/text;VALUE=BINARY;ENCODING=BASE64;X-APPLE-FILENAME=org.opencastproject.capture.agent.properties:...
END:VEVENT
END:VCALENDAR
```

The iCal event contains start and end dates, all meta data catalogs for the event, the UID which is the `%RECORDING_ID`
and which important when uploading media later on and additional capture agent properties which should be passed on.
Note that most meta data like the dublin core catalogs can in most cases be ignored.

In most cases, this means the data you are interested in per event are:

- `DTSTART`: Date to starte the recording at
- `DTEND`: Date at which to stop the recording
- `UID`: Recording identifier used for updating the recording status and associating uploads with a scheduled event
- `LOCATION`: This should always match your agent's name
- `ATTACH;...FILENAME=org.opencastproject.capture.agent.properties`: Agent properties to pass on in case of workflow
  properties

Depending on the amount of recordings scheduled for the particular capture agent, this file may become very large. That
is why there are two way of limiting the amount of necessary data to transfer and process:


1. Sending the optional parameter `cutoff` to limit the schedule to a particular time span in the future.

       ${SCHEDULER-ENDPOINT}/calendars?agentid=<name>&cutoff=<time>

   The value for cutoff is a Unix timestamp in milliseconds from now. Events beginning after this time will not be
   included in the returned schedule.

2. Use the HTTP ETag and If-Not-Modified header to have Opencast only sent schedules when they have actually changed.


### Set Agent and Recording State

Setting the agent state is identical to the registration of the capture agent and done by sending an HTTP POST request
to:

    ${CAPTURE-ADMIN-ENDPOINT}/agents/<name>

…including the following data fields:

    state=capturing
    address=http(s)://<ca-web-ui>

Additionally, set the recording state using the event's UID obtained from the schedule with an HTTP POST request to

    ${CAPTURE-ADMIN-ENDPOINT}/recordings/<recording_id>

…including the data field:

    state=capturing



### Recording

This task is device specific. Use whatever means necessary to get the recording
done.


### Set Agent and Recording State

This step is identical to the previous status update but for the state.

If the recording has failed, the recording state is updated with `capture_error` while the agent's state is set back to
`idle` if the error is non-permanent and to `error` if it is permanent and block further recordings.

If the recording was successful, both states are set to `uploading`.


### Get Ingest Endpoint Locations From Service Registry

This step is identical to first request to the service registry expect that it is sufficient to request the location for
the service `org.opencastproject.ingest`. If this request fails, assume the old data to be valid.


### Ingest (Upload) Recording

Use the ingest endpoint to upload the recording.

There are multiple different methods to ingest media. Please refer to the REST endpoint documentation for details of
these methods. The most commonly used are:

- Single request ingest using an HTTP POST request to
    - `${INGEST-ENDPOINT}/addMediaPackage`
- Multi request ingest using HTTP POST requests to
    - `${INGEST-ENDPOINT}/createMediaPackage`
    - `${INGEST-ENDPOINT}/addDCCatalog`
    - `${INGEST-ENDPOINT}/addTrack`
    - `${INGEST-ENDPOINT}/ingest`

Please make sure that the event's UID is passed on as `workfloeInstanceId` to the final call to `/ingest/ingest` to
match the scheduled event to the media being uploaded.

If possible, please follow these additional rules about recording files:

- Recordings may be deleted if the ingest was successful.
- Recordings should be stored in case of a failure.

#### Upload Metadata

The calendar (iCal) with the scheduled events retrieved in an earlier step also contains metadata catalogs as attached
files. To modify metadata, these catalogs can be modified and ingested as well. Opencast's default setting is to use
these for updating the existing metadata in the system.

If no metadata modifications are required (usual case), please do not modify these files and do not upload
them. In short: Ignore these attachments

Additional note for Opencast ≤ 3.x: Opencast only creates events in the database after ingesting the files. Scheduled
data are kept separately. That is why for these Opencast versions, all metadata files need to be ingested. Usually, that
means to take the metadata catalogs from the schedule and ingest them unmodified using for example the `/addDCCatalog`
endpoint.


### Set Agent and Recording State

Again, this step is identical to the previous status updates except for the state.

If the upload has failed, the recording state is updated with `upload_error` while the agents state is set back to
`idle` if the error is non-permanent or to `error` otherwise.

If the upload was successful, the recording status is set to `upload_finished` while the agents state is set back to
`idle`.


Agent State And Configuration
-----------------------------

This section describes some additional aspects of the communication between capture agent and the Opencast core.


### Creating An Agent On The Core

An agent record is created on the core the first time the agent communicates with the core. There is no special endpoint
or registration required, just send the state and the agent record will be created.


### Agent State

Additional to the required status updates outlined above, the agent should continue to send this status information on a
regular basis to allow Opencast to determine that the agent is still active. If the agent fails to do so, it may be
marked as offline in the Opencast user interface after a certain amount of time (The default is 120min).


### Agent Configuration

If a special configuration is required, the agent should send its configuration data in a regular interval to ensure
Opencast has the updated configuration even if the core is reset in the meantime.

It should also send the configuration when the agent's configuration changes to avoid conflicts between selected and
available options.

The format of this XML structure is the following:

```XML
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd\">
<properties>
  <comment>Capabilities for $AGENT_NAME</comment>
  <entry key="$KEY_NAME">$VALUE</entry>
  …
</properties>
```

If sent as JSON, the format is a simple JSON object:

```JSON
{
  'key':'value',
  'key2':'value2'
}
```

To specify inputs the user can select, the special key `capture.device.names` is used.  It is a comma separated list of
inputs which will be presented in the Opencast user interface.


### Recording State

If the agent is processing (recording) a previously scheduled event, it must send the recording's state to the core. It
may do this on a regular basis but at least should do this once the state of the recording changes since, for example,
the recording process has started.

Note that these status changes are used in the administrative user interface and failing to set a state may cause the
interface to display a warning such as “The agent may have failed to start this recording”.


To send the recording's state to the core, a valid state (as defined here) is sent via HTTP POST to:

    ${CAPTURE-ADMIN-ENDPOINT}/recordings/<recording_id>


### Calendaring Data

Agent's are expected to understand Opencast's iCalendar implementation. They should poll the calendar endpoint in a
regular interval to update their internal schedule.

Agent's should use a permanent cache (e.g. disk or database) for the cached schedule to be able to handle power and/or
network failures gracefully. This also allows an agent to be used in a network-less environment, for example for mobile
recordings: Merely cache the calendar data once after which the agent is brought to its destination where it will
capture and cache the pre-scheduled recordings.

To retrieve the calendar for an agent an HTTP GET is performed to

    ${SCHEDULER-ENDPOINT}/calendars?agentid=<name>

Note that the schedule has ETag support, which is very useful to speed up the processing of larger calendars.


### Capture Agent Configuration File

*TODO: Verify that this is still necessary with ≥ 4.0*

One file attached to each scheduled event is `org.opencastproject.capture.agent.properties`. This file contains the
capture agent configuration directives (e.g. turning inputs on and off) as well as workflow directives which are
important for the ingest process without which the core may misbehave.

All keys in this file contain prefixes identifying the type of property. For example, workflow directives are prefixed
by `org.opencastproject.workflow.config`.  When passing the configuration directive to the core using the file  ingest
REST endpoints, the agent must usually strip this prefix from the parameter. For example,
`org.opencastproject.workflow.config.trimHold=true` should be passed as `trimHold=true`.

The `org.opencastproject.workflow.definition directive` is important as well, this is the workflow definition identifier
and should be passed as a parameter during the ingest operation.

Example configuration file:

    #Capture Agent specific data
    #Tue May 22 17:34:22 CST 2012
    org.opencastproject.workflow.config.trimHold=true
    capture.device.names=MOCK_SCREEN,MOCK_PRESENTER,MOCK_MICROPHONE
    org.opencastproject.workflow.definition=full
    event.title=Test Capture
    event.location=demo_capture_agent


### Ingesting Media

Opencast provides several different methods to ingest media, with each having some advantages and disadvantages. The
following description will give a short overview of the different methods. For more details, again, have a look at the
REST endpoint documentation of the ingest service.


#### Multi Request Ingest

*This is the recommended way to use for most capture agents. It offers the most features to use and does not require any
pre- and post-processing of ingested material.*

Using this method, a number of successive HTTP calls are made during the ingestion of media. The result of a successful
call is the newly updated media package. This media package is created by one call, then amended by a number of other
calls, each adding additional elements like tracks, attachments or metadata catalohs. Finally, it is then passed to the
last endpoint to begin processing.

The advantage to this process is that in case of a network failure only one particular element needs to be repeated in
contrast to repeating the whole process required by all other ingest methods.

To begin, the agent must first generate a valid media package. This is done via an HTTP GET request to
`${INGEST-ENDPOINT}/createMediaPackage`. The resulting media package will contain the base skeleton used in later calls.
Each following call will require a media package as input and will modify and return it to be used for the next call.

The next step(s) vary. Essentially, each generated file for a recording must be added, one at a time, to the media
package. For this, an agent may use the following REST endpoints:

- `${INGEST-ENDPOINT}/addTrack` to add media files (video, audio, …) used for processing and/or publication
- `${INGEST-ENDPOINT}/addDCCatalog` to add the dublin core metadata catalogs like the basic episode metadata (title, …)
- `${INGEST-ENDPOINT}/addCatalog` to add all types of metadata catalogs.  This is a more general version of
  `addDCCatalog` and is seldom necessary.
- `${INGEST-ENDPOINT}/addAttachment` to add arbitrary attachments (cover images, access control catalogs, …) to the
  media package.

Finally, once you have added all files, it is time to ingest the media package and begin processing. After this, no
further files can be added.

To ingest a recording, an HTTP POST is sent to `${INGEST-SERVICE}/ingest`.


#### Single Request Ingest

The single request ingest will, as its name implies, handle the whole process as part of a single HTTP request. This
is a convenient way of adding smaller ingest since the implementation does not require to store any internal state. The
operation is atomic after all: Either it succeeds or fails.

The disadvantage to this is that the complexity of ingests is limited, e.g. no attachments can be added to the media
package this way, and a failure means that all files need to be re-transferred.

For this method, the agent posts all data to `${INGEST-ENDPOINT}/addMediaPackage`.


#### Zipped Media Ingest

In general, the use of this method is discouraged because of the additional load for packing and unpacking the material
compared to the negligible gain. For this method, the captured media, along with some metadata files is zipped and then
HTTP POSTed to the core. The core then unzips the media package and begins processing. This unzipping operation is quite
disk intensive, and the REST endpoint does not return until the unzipping is done. Thus, please beware of proxy timeouts
and additional disk utilization.

To ingest a zipped media package an HTTP POST is performed to `${INGEST-ENDPOINT}/addZippedMediaPackage`. The BODY of
the POST must contain the zipped media package.


Further Reading
---------------

The communication involve several REST endpoints. Additional documentation about these can be found in the REST docs of
the specific service. The REST documentation can be found at `/rest_docs.html` in every Opencast instance to reflect
that servers unique capabilities.

Services involved in the communication with the capture agent are:

- The capture admin service used to register the capture agent and set its current status.
- The scheduler service to get scheduled recordings for an agent.
- The ingest service to upload recording files and start processing.
