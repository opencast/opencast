Opencast 2.3: Release Notes
===========================

*Enabling external interoperability.*

Opencast 2.3 introduces the new external application API to more easily connect to and integrate other tools like
learning management systems. It also continues the work of improving the administrative user interfaces and other parts
of Opencast by fixing over 100 issues.


New Features and Improvements
-----------------------------

- **External API** - While Opencast always was very strong when it came to the integration of third-party systems,
    the introduction of this brand new state-of-the-art API enables integrators to deal with even more complex
    integration scenarios. Its key feature is the support of versioning: Multiple different versions of the API can be
    offered the same time, enabling service providers to upgrade the API without breaking external dependencies. With
    fine-grained, easily customizable per-user access control, a comprehensive documentation and its ease of use, this
    new API builds a solid basis for the future of Opencast - Integration has never been more effective and fun!

- **Event Statistics** - The statistic counters that display statistics of events and allow the user to quickly apply
    filters have been significantly improved by both using the screen estate more effectively and better fitting into
    the overall UI design. Besides this, support for role-based visibility and optimizations to improve performance in
    large-scale scenarios make this useful feature even more useful.

- **Capture Agent API** - The capture agent API now better supports ad-hoc recordings by allowing capture agents to
    start (create), stop and prolong immediate events. Besides this, the missing parts of the JSON interface have been
    completed.

- **Admin UI Style Guide** - A comprehensive style guide for the Admin UI has been written and made publicly available
    to help to maintain the high level of graphical design in the future.

- **Enhanced Themes** - In addition to intro and outro movies, themes now also support titles: Metadata can be
    rendered on top of an image that can be uploaded or extracted from the recording. This image is then converted to a
    movie and rendered into the final video so that Opencast can render self-describing videos. The default workflow
    has been extended to support this kind of enriching recordings with metadata out-of-the-box.

- **New Workflow Operations Handler** - Two new workflow operation handlers have been added
    - WOH export-wf-properties enables Adopters to make workflow settings persistent for later re-use
    - WOH import-wf-properties can load those persistent settings back into workflow instances to be used

- **Improved Workflow Operation Handlers** - Some existing workflow operation handlers have been improved resulting in
    more flexible and powerful processing capabilities
    - *WOH segment-video* has been significantly optimized to provide better video segmentation results. In particular,
      it handles (accidental) processing of non-segmentable content vastly better
    - *WOH publish-engage, retract-engage, publish-configure and retract-configure* have been optmized to perform
      significantly better when it comes to publish/retract a large  number of files
    - *WOH theme* supports more settings of themes that can be used to control the flow of workflow operations

- **New Language Supported** - British-English has been added to improve the localization for non-US English speaking
    countries, in particular when it comes to non-US date formats.

- **Maintainability** - By removing obsolete legacy code throughout the Opencast code basis, updating various
    libraries, improving the documentation, design and performance, we continuously ensure Opencast's maintainability

For a completed list of changes, have a look at the [changelog](changelog.md).


Additional Notes About 2.3.1
----------------------------

Opencast 2.3.1 fixes a critical issue with publication on distributed systems as well as two security issues and several
minor issues.


Additional Notes About 2.3.2
----------------------------

Opencast 2.3.2 is a bug fix release that fixes some minor issues of Opencast 2.3.1. For more details, please have a look
at the [changelog](changelog.md).


Additional Notes About 2.3.3
----------------------------

Opencast 2.3.3 is a bug fix release that fixes some issues of Opencast 2.3.2, including an important security issue.
For more details, please have a look at the [changelog](changelog.md).


Opencast 2.3 Release Schedule
-----------------------------

|Date                               |Phase
|-----------------------------------|---------------------------------
|October <del>*1st*</del> 4th       |Feature Freeze
|October <del>*1st*</del> 4th - 23th|Internal QA and bug fixing phase
|October 10th - 16th                |Review Test Cases
|October 17th - 23th                |Documentation Review
|October 24th - November 14th       |Public QA phase
|November 15th - 28th               |Additional bug fixing phase
|November 21th - 27th               |Translation week
|November 28th - December 11th      |Final QA phase
|December 13th                      |Release of Opencast 2.3
