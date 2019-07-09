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


API changes
-----------

- Removed REST endpoints for modifying workflow definitions
    - DELETE /workflow/definition/{id}
    - PUT /workflow/definition


Additional Notes About 7.1
--------------------------

Opencast 7.1 is the first maintenance release for Opencast 7. It fixes a bug with the scheduler migration which may have
caused minor issues for old, process events which were missing some meta-data. If you have already migrated to Opencast
7.0 and experience this problem, simply re-start the scheduler migration and re-build the index once more.

Release Schedule
----------------

TBD
