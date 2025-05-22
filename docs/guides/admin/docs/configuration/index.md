Opencast Configuration Guides
=============================

These guides will help you to configure Opencast. If you are a first-time user, please make sure to at lease have a look
at the [basic configuration guide](basic.md).


Essential Configuration
-----------------------

Required for Opencast to run properly.  
Arguably part of installing Opencast.

- [Basic Configuration](basic.md)
- [Database Configuration](database.md)
- [HTTPS Configuration](https/index.md)


Important Configuration
-----------------------

Not absolutely required, but should have thought about before going live.

- [Firewall Configuration](firewall.md)
- [Multi Tenancy Configuration](multi.tenancy.md)

Security Configuration
----------------------

Everything related to protecting your installation and data.

- [Authentication, Authorizations, and User Management](security.md)
    - [CAS Security Configuration](security.cas.md)
    - [LDAP Authentication and Authorization (without CAS)](security.ldap.md)
    - [Moodle User Provider](security.user.moodle.md)
    - [Sakai User Provider](security.user.sakai.md)
    - [Brightspace User Provider](security.user.brightspace.md)
    - [Canvas LMS User Provider](security.user.canvas.md)
    - [Authentication and Authorization Infrastructure (AAI)](security.aai.md)
    - [JWT-based Authentication and Authorization (e.g. for OIDC)](security.jwt.md)
    - [Access Control Lists](acl.md)
    - [Stream Security](stream-security.md)
    - [Role Based Event Access](episode-id-roles.md)
- [Serving Static Files](serving-static-files.md)
- [Stream Security Overview](stream-security/stream-security-overview.md)
    - [Configuration](stream-security/stream-security-config.md)

All Configuration
---------------------

A list of everything.

- Admin UI Configuration
    - [Event Filters](admin-ui/event-filters.md)
    - [Manual Asset Upload](admin-ui/asset-upload.md)
    - [Languages](admin-ui/languages.md)
    - [Statistics](admin-ui/statistics.md)
    - [Thumbnails](admin-ui/thumbnails.md)
- Amazon Services
    - [Amazon S3 Archive Storage](awss3archive.md)
    - [Amazon S3 Distribution](awss3distribution.md)
- [Authentication, Authorizations, and User Management](security.md)
    - [CAS Security Configuration](security.cas.md)
    - [LDAP Authentication and Authorization (without CAS)](security.ldap.md)
    - [Moodle User Provider](security.user.moodle.md)
    - [Sakai User Provider](security.user.sakai.md)
    - [Brightspace User Provider](security.user.brightspace.md)
    - [Canvas LMS User Provider](security.user.canvas.md)
    - [Authentication and Authorization Infrastructure (AAI)](security.aai.md)
    - [JWT-based Authentication and Authorization (e.g. for OIDC)](security.jwt.md)
    - [Access Control Lists](acl.md)
    - [Stream Security](stream-security/stream-security-config.md)
- [Basic Configuration](basic.md)
- [Database Configuration](database.md)
- [Encoding Profile Configuration](encoding.md)
- [Execute Service](execute.md)
- [External API Configuration](external-api.md)
- [External Monitoring](monitoring.md)
- [Firewall Configuration](firewall.md)
- [HTTPS Configuration](https/index.md)
- [Inbox](inbox.md)
- [LTI Module](ltimodule.md)
- [List Providers](listproviders.md)
- [Live Schedule](liveschedule.md)
- [Load Configuration](load.md)
- [Logging Configuration](log.md)
- [Media Module](mediamodule.configuration.md)
- [Metadata Configuration](metadata.md)
- [Metrics (OpenMetrics, Prometheus)](metrics.md)
- [Multi Tenancy Configuration](multi.tenancy.md)
- [OAI-PMH Configuration](oaipmh.md)
- [Player Configuration](player/player.overview.md)
    - [Paella player 7 Configuration](player/paella.player7/configuration.md)
- [Plugin Management](plugin-management.md)
- [Search Index Overview](searchindex/index.md)
    - [Elasticsearch](searchindex/elasticsearch.md)
- [Serving Static Files](serving-static-files.md)
- [Stream Security Overview](stream-security/stream-security-overview.md)
    -  [Configuration](stream-security/stream-security-config.md)
- [Studio](studio.md)
- [Subtitles](subtitles.md)
- Termination State:
    - [Basic](terminationstate.md)
      - [AWS AutoScaling](terminationstate.aws.autoscaling.md)
- [Text Extraction](textextraction.md)
- [Timeline Previews](timelinepreviews.md)
- Transcription configuration:
    - [AmberScript](transcription.configuration/amberscripttranscripts.md)
    - [Google Speech](transcription.configuration/googlespeechtranscripts.md)
    - [IBM Watson](transcription.configuration/watsontranscripts.md)
    - [Microsoft Azure](transcription.configuration/microsoftazure.md)
    - [Vosk](transcription.configuration/vosk.md)
    - [Whisper](transcription.configuration/whisper.md)
- [User Statistics and Privacy Configuration](user-statistics.and.privacy.md)
- [Video Editor Overview](videoeditor.overview.md)
    - [Architecture](videoeditor.architecture.md)
    - [Frontend](videoeditor.frontend.md)
    - [Workflow Operations](videoeditor.workflow-operation.md)
- [Video Segmentation](videosegmentation.md)
- [Workflow Configuration](workflow.md)
    - [Workflow Operation Handler](../workflowoperationhandlers/index.md)
- [Wowza Streaming Distribution Service](streaming-wowza.md)
- [YouTube Publication](youtubepublication.md)
