Opencast 4: Release Notes
=========================

New Features and Improvements
-----------------------------

- **Asset Manager** - The Archive service has been enhanced to support properties that can be attached to episodes.
    This allows services to store their data centrally and thus reducing the overall system complexity and the same
    time avoiding data being duplicated across multiple services (and many of the problems related with that).
    The Asset Manager provides a query language for easy manipulation of properties.
    To have a more appropriate name, the Archive service has then been renamed to Asset Manager service.
- **Scheduler** - The Scheduler service has been rewritten to take advantage of the Asset Manager service. The new
    Scheduler service provides full support for extended metadata for scheduled events and adds a transactional
    API for integrating external scheduling sources. A new tab in the event detail modal helps to more clearly
    separate bibliographic metadata from technical metadata
- **Theodul Player Improvements** - The Theodul player now supports MPEG-DASH and HLS. To improve the user experience
    when navigating in a video, preview images are shown when hovering over the timeline
- **Flexible Asset Upload** - This new facility allows Adopters to fully configure the upload dialog of Opencast as
    well as to configure arbitrary assets that can be uploaded and managed trough the user interface.
- **Monitoring Service** - The UI has been enhanced with a monitoring service that provides a visual indication
    of both the ActiveMQ status and the services status. In case of problems with Opencast services, a single click
    navigates the user to the Systems->Services page with an appropriate filter already set
- **IBM Watson Transcription Service** - The integration of the IBN Watson Speech-to-Text service allows Adopters
    to easily integrate speech-to-text into their existing workflows.
- **Wowza Adaptive Streaming** - The Wowza adaptive streaming distribution service is now included in the official
    Opencast release which relieves Adopters from the need to include this functionality from a separate code
    repository.
- **Manually retry failed operations** - It is now possible to make failing workflow operations pause the workflow,
    leaving the user the choice to manually retry or abort the failed operation.
- **User Interface Improvements** - Various improvements in the user interface further improve the user experiences
    of Opencast. Just to name a few:
    - A new datetimer picker makes entering start time more efficient
    - Cross page links allow the user to navigate to a different table with useful filters enabled by a single click
    - The video editor now opens much faster
    - The start date of an upload can be directly set in the upload dialog
    - The new view Location Details allows users to see the configuration and capabilities as reported
     by capture agents which simplifies the management of capture agents
- **OAI-PMH Improvements** - The addition of support for automatically publishing changes to the OAI-PMH server relieves
    users from the need to re-publish to OAI-PMH after changes to metadata. The metadata prefix *matterhorn-inlined*
    now provides support for extended metadata catalogs. Last but not least, the performance of the publication and
    retraction workflow operation handlers for OAI-PMH has been significantly improved by supporting bulk operations.
- **Workflow Operation Improvements**
    - *WOH series* can now apply series metadata to event metadata
    - *WOH timelinepreview* has been added. This workflow operation handler generates a single image that contains
      a large configurable number of preview images which allows players to implement highly efficient timeline
      previews. The Theodul player timeline preview features relies on this new workflow operation handler
    - *WOH execute-once* can now set workflow properties
- **Scalability Improvements** - Several problems considering the scalability of Opencast in large-scale
     scenarios have been addressed. In particular, Opencast 4.0 performs much better in the presence of thousands
     of series
- **Language Support** - Added support for Slovenian and Hebrew

Access Control Defaults
-----------------------

The new fallback access control list used by events for which no access control is specified has been changed to block
all access except for admin user. While this scenario should seldom happen, this change was made to prevent accidental
publication of non-public material. This option is certainly a safer choice than Opencast's previous default which was
public access.

Capture Agent API Metadata Handling
-----------------------------------

The new scheduler now creates a media package for each event which holds all assets as soon as the schedule is created.
This makes it unnecessary for any capture agent to download and re-ingest any media package elements unless they are
modified by the capture agent. Note that re-ingested media package elements will overwrite scheduled elements. Hence a
capture agent may modify these data.  If not required for the inner workings of the capture agent, we advise to not
download, modify and upload any media package elements to avoid errors.


Working File Repository Configuration
-------------------------------------

The Working File Repository URL is now set per tenant.  The Configuration key `org.opencastproject.file.repo.url` was
moved from `etc/custom.properties` to `etc/org.opencastproject.organization-mh_default_org.cfg` as
`prop.org.opencastproject.file.repo.url`.  The default value has not changed (`${org.opencastproject.server.url}`). On
a multiple server setup the value should be the same on all nodes.  For more information read the [Configure Opencast
](installation/multiple-servers/#step-5-configure-opencast) section.


Release Schedule
----------------

|Date                         |Phase
|-----------------------------|------------------------------------------
|Sep 22th 2017                |Feature Freeze
|Sep 25th - Oct 13th 2017     |Internal QA and bug fixing phase
|Oct 16th - Nov 3th 2017      |Public QA phase
|Nov 6th  - Nov 24th 2017     |Additional bug fixing phase
|Nov 20th - Nov 24th 2017     |Translation week
|Nov 27th - Nov 3rd 2017      |Final QA phase
|Dec 7th 2017                 |Release of Opencast 4

**Release Managers:**

- Christian Greweling (ELAN e.V.)
- Sven Stauber (SWTICH)
