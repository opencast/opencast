Opencast 8: Release Notes
=========================

Features
--------

- The `adminworker` distribution is no longer available.


Improvements
------------

- Media package elements retrieved from the asset manager (e.g. using “start task”) now always get tagged `archive` even
  when they have not been tagged thus before.


Configuration changes
---------------------
- `etc/org.opencastproject.adminui.cfg` has a new option `retract.workflow.id` which contains the id of the workflow used
  to retract events when deleting.


API changes
-----------

- Removed REST endpoints for modifying workflow definitions
    - DELETE /workflow/definition/{id}
    - PUT /workflow/definition


Aditional Notes About 7.4
-------------------------
Opencast 7.4 brings a performance fix for some queries in the search API that can cause Opencast to run out of memory.

This release also gives back a patch that was in version 6.x that allows to filter capture agent roles for ACLs and
fixes the date cell of the events overview table in the admin UI.

Finally, Opencast 7.4 also brings an update to the CAS security example that was out of date.


Aditional Notes About 7.5
-------------------------
Opencast 7.5 fixes behaviour where the bibliographic date was not changed when changing the technical date via Rest.
Also an option was added to disable thumbnail generation in the video editor because it can lead to performance issues.


Release Schedule
----------------

TBD
