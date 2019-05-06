Statistics
==========

Opencast provides an extensible mechanism to make statistics data available to the Opencast administrative user
interface and also to third-party applications using the External API.

The overall idea is that sources of statistics data (*StatisticsProvider*) are managed at a central service
(*StatisticsService*). The *StatisticsService* supports a minimal set of common attributes, in particular, the type
of the *StatisticsProvider* which implies a data format and available parametrization.

A client can use the *StatisticsService* to retrieve a list of all available *StatisticsProviders*. The data format
as well as the parameters supported by the *StatisticsProviders* are implied by the type of the provider. This
information is used by the client to decide whether it can visualize the statistics data and which component has
to be used for visualization.

Modules
-------

The StatisticsService consists of the following modules:

* `statistics-service-api`
An API module defining the core StatisticsService and StatisticsProvider functions.
* `statistics-service-impl`
The default implementation of the StatisticsService as an OSGi service.
* `statistics-service remote`
The remote implementation of the StatisticsService.
* `statistics-provider-influx`
An implementation of the StatisticsProvider for InfluxDB
* `statistics-provider-random`
An implementation of the StatisticsProvider for testing and demo purposes.

Interfaces & Classes
--------------------

The Opencast `StatisticsService` implements the two interfaces `StatisticsProviderRegistry` and
`StatisticsService.`

**StatisticsProviderRegistry**

This interface is used by `StatisticsProvider` implementations to register and unregister themselves at the
statistics service.

Method         | Description
---------------|------------
addProvider    | Register a statistics provider at the statistics service
removeProvider | Unregister a statistics provider from the statistics service

**StatisticsService**

This is the interface used by clients of the statistics service to retrieve a list of registered statistics
providers

Method         | Description
---------------|------------
getProviders   | Retrieve lists of statistics providers

**StatisticsProvider**

The `StatisticsProvider` interface provides access to common attributes of the statistics providers:

Method          | Description
----------------|------------
getId           | Returns the unique identifier of the statistics provider
getType         | Returns the type of the statistics provider
getResourceType | Returns the `ResourceType` of the statistics provider
getName         | Returns the displayable name of the statistics provider
getDescription  | Returns the displayable description of the statistics provider

whereas supported resource types are

ResourceType    | Description
----------------|------------
EPISODE         | The statistics data relates to an episode
SERIES          | The statistics data relates to a series
ORGANIZATION    | The statistics data does not relate to a particular object

Integration
-----------

The StatisticsService API is supposed to be an internal Opencast interface. External clients can use the External API
to access Opencast statistics and the Opencast Admin UI has access through the Admin UI facade.

External API
------------

The External API supports Opencast statistics by its [Statistics API endpoint](api/statistics-api.md).

Admin UI
--------

The Admin UI supports Opencast statistics at various levels:

File                                                                                        | Description
--------------------------------------------------------------------------------------------|------------
modules/admin-ui/src/main/java/org/opencastproject/adminui/endpoint/StatisticsEndpoint.java | Implementation of the Statistics endpoint for the Admin UI facade
modules/admin-ui/src/main/webapp/scripts/shared/resources/statisticsResource.js             | Abstracts StatisticsEndpoint for use in the web application



