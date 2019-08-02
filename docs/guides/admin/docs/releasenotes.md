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


Additional Notes About 7.2
--------------------------

Opencast 7.2 fixes a bug in the video editor configuration present in Opencast 7.0 to 7.1 which will cause Opencast to
always silently skip the video editor and publish the whole video. The problem was introduced by [a fix in the default
workflows](https://github.com/opencast/opencast/pull/944) and later fixed again by a [configuration change therein
](https://github.com/opencast/opencast/pull/1013). If you use the default workflows, please make sure to update to the
latest state of the workflows.

If you use your own workflow and did not adapt the first patch, you should not be affected by this problem at all. If
you are, just make sure that source and target smil flavor for the editor workflow operation are identical like it is
ensured [by the official fix](https://github.com/opencast/opencast/pull/1013). A proper solution not relying on specific
configurations and less error prone is in work and will be added to the upcoming major Opencast release.


Release Schedule
----------------

TBD
