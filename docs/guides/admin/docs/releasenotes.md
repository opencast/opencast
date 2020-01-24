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


Other Changes
-------------

- OAI-PMH is not enabled by default any longer but need to be added to the workflow is it is to be used.
  This prevents users from accidentally and unknowingly publishing events to a publication channel which is public by
  default regardless of any set permissions for events.


Aditional Notes About 7.6
-------------------------
Opencast 7.6 fixes a number of security issues. Upgrading is strongly recommended.
Take a look at the [security advisories](https://github.com/opencast/opencast/security/advisories) for more details.

One change is that the OAI-PMH endpoint is no longer publically accessible by default.
If you need it to be, you can easily change that in the security configuration at `etc/security/mh_default_org.xml`.


Release Schedule
----------------

*To be determined*
