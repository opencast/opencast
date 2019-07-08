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


Release Schedule
----------------

TBD
