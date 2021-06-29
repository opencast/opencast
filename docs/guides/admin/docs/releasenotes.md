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
