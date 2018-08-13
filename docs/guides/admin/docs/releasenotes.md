Opencast 6: Release Notes
=========================

New Features and Improvements
-----------------------------

- **Admin UI** - Deleting Series now warns if series contains events. You can configure if the user is allowed to
  delete a series containing events in the series endpoint config file.


Configuration changes
---------------------

- The tracking options defaults have changed to be more aware of the European Union's General Data Protection
  Regulation. Note that you can [still use the old settings if you want to](configuration/user-statistics.and.privacy.md).

- The role ROLE_UI_EVENTS_DETAILS_GENERAL_VIEW for viewing the publications (previously general) tab in the event
  details modal has been renamed to ROLE_UI_EVENTS_DETAILS_PUBLICATIONS_VIEW for consistency.


Release Schedule
----------------

|Date                         |Phase
|-----------------------------|------------------------------------------
|Sep 25th 2018                |Feature Freeze
|Oct 29th - Nov 4th 2018      |Translation week
|Nov 5th - Nov 18th 2018      |Public QA phase
|Dec 10th 2018                |Release of Opencast 6.0


Release Managers
----------------

- Matthias Neugebauer (University of Münster)
- Lars Kiesow (ELAN e.V.)
