Opencast 10: Release Notes
=========================


Features
--------

- New *conditional-config* workflow operation handler, used to evaluate a list of conditions and set
  a workflow configuration variable accordingly.
- Opencast 10 [adds JVM metrics to the metrics endpoint](https://github.com/opencast/opencast/pull/2694)
  so you can monitor Opencast's memory and CPU behavior more closely using Prometheus.
- Updated Opencast Studio to 2021-06-11
- Update to Elasticsearch 7.10.2

Improvements
------------

- [#2019](https://github.com/opencast/opencast/pull/2019): The full list of Opencast users can now be reduced in the
  admin UI's filtering tool using a regular expression
- [#2619](https://github.com/opencast/opencast/pull/2619): Autoconfigure job dispatching so that non-admin nodes do not
  dispatch jobs by default
- [#2554](https://github.com/opencast/opencast/pull/2554): Retract publications before deleting events rather than
  leaving the publications hanging with no easy way to remove them
- Improvements to Elasticsearch indexing [#2211](https://github.com/opencast/opencast/pull/2211), etc
- [#2677](https://github.com/opencast/opencast/pull/2677): Add 'latest' as a target verison for moving between asset
  manager storages
- Library updates across the project

Behaviour changes
-----------------

- The default visibility of events and series in the admin interface has changed so that users will not only see
  entities they have write access to. This obviously has no effect on admin users since they have write access to
  everything anyway.
- Paella's internal user tracking is now disabled by default since it is seldom used and can put a lot of stress on the
  database.
- Opencast 10 now requires JDK 11

API changes
-----------

- Rename value for `sort` parameter in search API from `DATE_PUBLISHED` to `DATE_MODIFIED`.
- [[#2644](https://github.com/opencast/opencast/pull/2644)]: Use millisecond precision in Solr date range queries

Additional Notes about 10.5
---------------------------

This release fixes several bugs.  Notably, this should drastically speed up audio normalization.

Additional Notes about 10.4
---------------------------

This release fixes bugs and a startup issue.  Some adopters were experiencing startup issues due to the JWT
authentication module being installed by default.  This release removes that module for default configurations.

Additional Notes about 10.3
---------------------------

This release fixes a number of bugs.  This release also includes [a fix](https://github.com/opencast/opencast/pull/2964)
which prevents ActiveMQ from eventually blocking operations.  This fix must be manually deployed.  Please follow the
existing the [message broker](configuration/message-broker.md) configuration instructions with the updated file.

Additional Notes about 10.2
---------------------------

This release fixes a number of bugs including two very important fixes related Extron SMP capture agent compatibility
and problems during publication.

__Manual action required if upgrading from Opencast 10.1__:
Opencast 10.1 contains a bug which can cause problems with republishing events.
If you never ran 10.1 in production, e.g. upgraded from 9.7 to 10.2, no manual action is required.

If you have, to fix the problem, you will need to re-write the search service's Solr entries set in Opencast 10.1
by re-indexing those events. To re-index, stop your presentation/allinone node,
remove `<karaf-data>/solr-indexes/search` (often `/var/lib/opencast/solr-indexes/search`)
and restart Opencast. The re-index progress will be logged. Once Opencast is up again, everything should be fine.
All other indexes are not effected.

More details can be found at [pull request #2923](https://github.com/opencast/opencast/pull/2923).

Additional Notes about 10.1
---------------------------

This release contains many bugfixes.  In particular, the email libraries are now working properly! You might also
encounter issues with updating the Elasticsearch indices with 10.0, especially when deleting elements, that are fixed
with this minor release.

From this release onwards there's also no longer the need to run the Elasticsearch index rebuild separately for each
service to avoid concurrency issues. Instead, you can simply use the /admin-ng/index/recreateIndex or /api/recreateIndex
endpoints to trigger the complete rebuild and be sure that everything will happen in order, since there is no longer any
asynchronicity involved.


Release Schedule
----------------

| Date                        | Phase                    |
|-----------------------------|--------------------------|
| May 18, 2021                | Feature freeze           |
| May 24, 2021                | Translation week         |
| May 31, 2021                | Public QA                |
| June 15, 2021               | Release of Opencast 10.0 |



Release managers
----------------

- Greg Logan
- Per Pascal Seeland (University of Stuttgart)
