Opencast 9: Release Notes
=========================

Important Changes
-----------------

- External Elasticsearch
- MariaDB JDBC driver


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

*To be determined*
