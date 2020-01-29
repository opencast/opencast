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

### Fixed Security Issues

- CVE-2020-5231 – [Users with ROLE\_COURSE\_ADMIN can create new users
  ](https://github.com/opencast/opencast/security/advisories/GHSA-94qw-r73x-j7hg)
- CVE-2020-5206 – [Authentication Bypass For Endpoints With Anonymous Access
  ](https://github.com/opencast/opencast/security/advisories/GHSA-vmm6-w4cf-7f3x)
- CVE-2020-5222 – [Hard-Coded Key Used For Remember-me Token
  ](https://github.com/opencast/opencast/security/advisories/GHSA-mh8g-hprg-8363)
- CVE-2020-5230 – [Unsafe Identifiers
  ](https://github.com/opencast/opencast/security/advisories/GHSA-w29m-fjp4-qhmq)
- CVE-2020-5228 – [Public Access Via OAI-PMH
  ](https://github.com/opencast/opencast/security/advisories/GHSA-6f54-3qr9-pjgj)


Release Schedule
----------------

*To be determined*
