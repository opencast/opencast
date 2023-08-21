
[1]: http://en.wikipedia.org/wiki/Basic_access_authentication
[2]: http://en.wikipedia.org/wiki/XML
[3]: http://en.wikipedia.org/wiki/JSON
[4]: http://semver.org
[5]: http://en.wikipedia.org/wiki/Representational_state_transfer

[figure_1]:media/img/figure_1.png "Figure 1: Architectural overview"


# External API

## Introduction

In order to allow for robust technical integration of applications like learning management systems or mobile
applications, Opencast offers the External API to allow those applications to provide access
to and management of resources exposed through the API.
The External API has been designed and implemented to support large numbers of clients, each with considerable
amounts of requests per time interval. In addition, security has been a focus to ensure protection of the
managed data and to support use cases promoting differing views on the managed data.


## Architectural Overview

The External API has been implemented as an abstraction layer to multiple internal APIs that the underlying
application (Opencast) offers for the manipulation of resources like series, events or users (see [Figure
1: Architectural overview](#figure_1)).

### Authentication and Authorization
The External API features a dedicated security layer that is in charge of providing support for a variety of
authentication and authorization mechanisms. Additionally, the security layer provides means for delegation of
authorization to the client application in cases where the API client needs to manage its own set of assets with
implicit access control. These concepts are documented in greater detail in the following
[Authentication](authentication.md) and [Authorization](authorization.md) chapters.


### Requests for data
The abstraction layer is backed by a dedicated index, which is kept up-to-date using Opencast’s message
broker. When a request to an API method is received (1), the data is compiled using the index and returned to
the client (2). Since the index is scalable and optimized for performance, a large number of requests can be
processed per time interval.
The corresponding requests along with the potential responses are defined later on in the [API](usage.md) chapter.

### Processing of updates
Whenever a client sends updated information to the External API, it will forward that information to the
corresponding Opencast services (3), which in turn will process the data and send messages to the
message bus accordingly (4). The messages are consumed by the External API’s data store and can be
served to its clients from then on.
The corresponding requests along with the data structures and potential responses are defined later on in
the [API](usage.md) chapter.


![Architectural overview][figure_1]

<a name="figure_1"></a>Figure 1: Architectural overview

Requests are authenticated and authorized (1), and corresponding responses are sent back to the client (2). Updates are
passed on to the backing application services and the modified data is then received through the application’s message
infrastructure (4), (5).


### Access
The External API has been implemented using the [Restful State Transfer][5] paradigm to expose resources of the
underlying system in the URL space that are then accessible using the HTTP protocol and verbs `GET`, `POST`, `PUT` and
`DELETE`.

Since as part of the communication, the External API is used to transfer potentially sensitive data between the client
and the server including the username and password as part of the Basic Authentication protocol, the API will usually
only be available over a secure HTTPS connection only.


### Url Space
The External API is located at the `/api` namespace on the Opencast admin node. This results in all requests to the
External API starting with `https://<hostname>/api`, where the hostname is depending on the installation and tenant
(see “Multi Tenancy”).


### Versioning
The External API is versioned so that applications developed against one version of the API won’t break with
enhancements or replacements of existing versions as long as they stay on the same major version. The set of
currently supported versions as well as the current version are exposed through REST methods as part of the meta API.


#### Version scheme
The External API is following the [semantic versioning standard][4], which is suggesting the use of versions of the
form `x.z.y` where `x` is the major version, `y` is the minor version and `z` is the patch level.

Part         | Comment
:----------- | :-------------
Major        | Changes are potentially backward incompatible and require changing client code.
Minor        | Functionality is added in a backwards-compatible manner.
Patch        | Bugfixes applied in a backwards-compatible manner.


#### Backwards Compatibility
As a consequence, the External API is expected to be backwards compatible between minor version upgrades, including the
patch level. This means that a client that has been developed against version 1.0.0 of the api will work with version
1.1.3 as well. This however may not be true going from version 1.1.0 to 2.0.0

### Multi tenancy
With Opencast being a multi tenant application, the External API reflects that characteristics as well. Requests are
mapped to individual tenants by matching the requests’s target hostname against the list of tenant hostnames.