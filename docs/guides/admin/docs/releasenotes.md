# Opencast 12: Release Notes


Features
--------

Improvements
------------

Behavior changes
-----------------

- Internally, workflows were stored as huge xml objects in the job table in the databaase. With Opencast 12,
workflows now get their own tables in the database. This makes it easier and quicker to query for workflows when
not using the Solr Index.


API changes
-----------
- [TBD] - Remove tasks.json Endpoint from Admin-UI JobEndpoint
- [TBD] - Add check for active workflows on a mediapackage to WorkflowRestService
- [TBD] - Add GET all workflows of a mediapackage to WorkflowRestService


Release Schedule
----------------

Release managers
----------------
