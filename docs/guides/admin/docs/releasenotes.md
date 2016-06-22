Opencast 2.2: Release Notes
===========================

*Making good things better*

By addressing more than 200 issues, this release focused on further improving the quality of Opencast.

Despite Opencast 2.2 being mainly a bugfix release, it also introduces some interesting features and improvements
that leverage the value provided to users.

New Features and Improvements
-----------------------------

- **Support for Job Weights** - A major shortcoming of the  job processing machinery of Opencast was that all jobs were
  treated equally by the job dispatcher. That made it hard to impossible to configure Opencast in a way to take full
  advantage of available processing resources. By the introduction of job weights, the job dispatcher can now more
  effectively dispatch jobs taking into account the load they will cause (job weight) so that a lot of light-weighted
  jobs may be dispatched without overloading your servers with heavy-weighted jobs
* **Stream Security** - So far, the distribution artefacts themselves (“files on your servers”) were not protected.
  Malicious users could compile and publish lists of URLs that would allow anybody to directly download the distribution
  artefacts from your servers. Stream Security now provides additional security by protecting the actual distribution
  artefacts themselves  using URL signing. This ensures that knowing the URLs of distribution artefacts won’t be not
  enough to download them from your servers. Besides, Opencast being capable of creating and verifying signed URLs,
  stream security plugins for both [Apache and Wowza are available](http://www.opencast.org/tools).
* **Zoom for Video Player** - The player now supports zooming. This feature is particularly useful when having high
  resolution videos of blackboard presentations as students may now choose the regions of interest to them and focus on
  them.
* **Side-by-side Previews for the Video Editor** - While still using a single-stream video player in the video editor,
  Opencast 2.2 now produces side-by-side previews for dual-stream videos so that users can finally see both streams in
  the Video Editor again
* **Improved Search Capabilities** - Search now supports wildcards and logical operations which enable power users to
  take advantage of the powerful search capabilities of Opencast
* **Improved Systems Tables** - The Systems tables have been vastly improved by means of fixing sorting, pagination,
  searching and filtering for all those tables. Additionally, the performance of these tables and the accompanying
  back-end mechanisms have been improved
* **Ingest of Partial Tracks** - The ingest service has been extended to support partial tracks which allow capture
  agents to ingest tracks as sets of logically related files and let the Opencast backend post-process them into
  fully-fledged tracks. This relieves implementers of capture agent software from needing capture agent side processing
  capabilities in case of incomplete tracks
* **Published Column** - The new column Published has been added to the events table providing a better overview of the
  publication status of events and a shortcut to publication channel URLs
* **Retractions Enforced** - Deletion of events is now enforcing the retraction of published events to ensure that you
  do not end up with orphaned distribution artefacts
* **Additional ACL Actions** - In additional to the built-in ACL actions read and write, administrators can now
  configure additional ACL actions that allow Opencast to provide third-party specific authorization information to
  third-party applications
* **Standard Metadata Configuration** - While Opencast 2.0 has introduced support for extended metadata that allow
  adopters to map almost any metadata to Opencast, the standard metadata have not been configurable so far. With
  Opencast 2.2, the standard metadata can be configured. For example, it is now just a matter of configuration to make
  fields mandatory
* **New Workflow Operation Handlers** - Several new workflow operation handlers have been added:
    * *WOH partial-import* is a powerful post-processing workflow operation for ingested partial tracks
    * *WOH publish-configure* replaces publish-internal
    * *WOH retract-configure* can retract publications of publish-configure (video editor previews can now be retracted)
    * *WOH analyze-tracks* sets workflow variables based on media package tracks that allow better dynamic control of
      workflow operation execution
* **Improved Workflow Operation Handlers** - Some existing workflow operation handlers have been improved resulting in
  more flexible and powerful processing capabilities
    * *WOH image* can now extract multiple images at multiple positions in a single pass as well as encode to multiple
      output formats. Both absolute and relative times are supported.
    * *WOH prepare-av* can be configured more flexible considering its audio-muxing facilities
    * *WOH publish-engage and WOH publish-configure* can now retract existing publications to avoid multiple publication
      elements in publication channels
    * *WOH composite* can now be used with single tracks recordings
* **New Languages Supported** - Support for six new languages has been added: Chinese Simplified, Dutch, Galician,
  Greek, Polish, Swedish
* **New Distributions** - A combined admin-worker distribution has been added to make it possible to have a two-server
  installation as documented in our old docs.

Release Process
---------------

**Opencast 2.2 Release Managers**

* Lars Kiesow (Elan e.V.)
* Sven Stauber (SWITCH)

**Opencast 2.2 Release Schedule**

|Date                              |Phase
|----------------------------------|---------------------------------------------
|April <del>*4th*</del> 6th        |Feature Freeze  *(Cutting of release branch)*
|April <del>*4th*</del> 6th - 24th |Internal QA and bug fixing phase
|&nbsp; *April 11th - 17th*        |Review Test Cases *(Dedicated team)*
|&nbsp; *April 18th - 24th*        |Documentation Review
|April 25th - May 15th             |Public QA phase
|May 15th - June 1st               |Additional bug fixing phase
|&nbsp; *May 25th - June 1st*      |Translation week
|June 2nd - June 12th              |Final QA phase *(Check release readiness)*
|June 15th                         |Release of Opencast 2.2
