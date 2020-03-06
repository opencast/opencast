Opencast 9: Release Notes
=========================

Important Changes
-----------------

- External Elasticsearch
- MariaDB JDBC driver
- Dropping compose operation in favor of encode


Features
--------

- todo:
- list
- of
- listed

Configuration changes
---------------------
- `etc/org.opencastproject.organization-mh_default_org` and by extension all tenant configuration files can now have
  new options named `prop.org.opencastproject.host.<server url>` to map the internal server urls to the tenant-specific
  ones so that tenant-specific capture agent users get the correct host address when asking the endpoint
  services/available.


Other Changes
-------------

- OAI-PMH is not enabled by default any longer but need to be added to the workflow is it is to be used.
  This prevents users from accidentally and unknowingly publishing events to a publication channel which is public by
  default regardless of any set permissions for events.


Release Schedule
----------------

|Date                         |Phase
|-----------------------------|------------------------------------------
|March 31st 2020              |Feature freeze
|May 4th - May 10th 2020      |Translation week
|May 11th - May 24th 2019     |Public QA phase
|June 15th 2020               |Release of Opencast 9.0

Release Managers
----------------

- Julian Kniephoff (ELAN e.V.)
- Carlos Turro Ribalta (Universitat Politècnica de València)
