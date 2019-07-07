Monitoring
==========

To assist in the operation of Opencast an application health-check is available. This will quickly
return the state of the specific node, whether it is running properly, has minor issues or is unavailable
for some reason e.g. in maintenance mode. Regular calls to the health-check from whatever monitoring
software you choose to use will give you confidence that the Opencast nodes are running correctly and alert
you when they are not.

For larger deployments the health-check can be used by load-balancers* and trigger fail-overs if one of the
nodes goes down.

\* The only nodes that make sense to load-balance are the externally facing ones, *ingest* and *presentation*.

Calling the Health-Check
------------------------

The Runtime module provides the health-check endpoint at ```/info/health``` and a simple HTTP GET request
will return a HTTP status code indicating the health of the node and response in JSON providing further details.

    curl "http://oc-admin.example.com/info/health"

The HTTP status code will just indicate whether the node is running or not, the response contains a *status* field
that indicates the actual health of the node. The status can have the values **pass**, **warn** and **fail**. The response
 implements the health-check format proposed here [https://inadarei.github.io/rfc-healthcheck](https://inadarei.github.io/rfc-healthcheck).

The table below shows the HTTP status codes the health-check status and the conditions for which they can occur.

_status_ | _notes_ | HTTP code | meaning
--- | --- | --- | ---
pass|n/a|200|All is OK
warn|service(s) in WARN state|200|Partially working service here
warn|services(s) in ERROR state|200|Look for service on another node
fail|maintenance|503|Node not available, try again later
fail|disabled|503|Node not available, try another node
fail|offline|503|Node not running, try another node

In all cases where the health-check status is not **pass** the JSON response provides more details.
A summary of the problem(s) are list in the *notes* field. In the case of services in non **NORMAL**
states these are listing the *checks* field. An example response for a **warn** status is shown below

    {
        "description" : "Opencast node's health status",
        "releaseId" : "8",
        "serviceId" : "http://oc-admin.example.com",
        "version" : "1",
        "status" : "warn",
        "notes" : [
           "service(s) in WARN state",
           "service(s) in ERROR state"
        ],
        "checks" : {
           "service:states" : [
              {
                 "changed" : "Tue Jun 04 11:10:12 BST 2019",
                 "links" : {
                    "path" : "service1"
                 },
                 "observedValue" : "WARNING",
                 "componentId" : "service1"
              },
              {
                 "changed" : "Tue Jun 04 11:15:27 BST 2019",
                 "links" : {
                    "path" : "service2"
                 },
                 "observedValue" : "ERROR",
                 "componentId" : "service2"
              }
           ]
        }
    }