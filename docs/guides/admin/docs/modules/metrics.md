Metrics (OpenMetrics, Prometheus)
=================================

Opencast comes with a metrics endpoint that supports the [OpenMetrics format](https://openmetrics.io) and can be used by
tools like [Prometheus](https://prometheus.io). The endpoint is available at `/metrics`.


Available Metrics
-----------------

Metrics are retrieved directly from the service registry and describe the current work load of the Opencast cluster.
There should usually be no need for monitoring several serves of a cluster. All will produce identical metrics.

This available metrics allow monitoring the current cluster state when it comes to processing
and should allow for good alerting rules:

- How many workflows are being processed
- How many jobs are being processed
- Are there any services in a warning or error state

Here is a complete list of available metrics:

```
# HELP opencast_services_total Number of services in a cluster
# TYPE opencast_services_total gauge
opencast_services_total{state="ERROR",} 0.0
opencast_services_total{state="WARNING",} 4.0
opencast_services_total{state="NORMAL",} 83.0
# HELP opencast_job_load_current Maximum job load
# TYPE opencast_job_load_current gauge
opencast_job_load_current{host="https://example.opencast.org",} 3.0
# HELP opencast_workflow_active Active workflows
# TYPE opencast_workflow_active gauge
opencast_workflow_active{organization="mh_default_org",} 1.0
# HELP opencast_job_load_max Maximum job load
# TYPE opencast_job_load_max gauge
opencast_job_load_max{host="https://example.opencast.org",} 4.0
# HELP opencast_job_active Active jobs
# TYPE opencast_job_active gauge
opencast_job_active{host="https://example.opencast.org",organization="mh_default_org",} 4.0
# HELP opencast_version Version of Opencast (based on metrics module)
# TYPE opencast_version gauge
opencast_version{part="major",} 10.0
opencast_version{part="minor",} 0.0
# HELP requests_total Total requests.
# TYPE requests_total counter
requests_total 1.0
```


Access
------

By default, you need to be authenticated and have `ROLE_ADMIN` or `ROLE_METRICS` to access the endpoint.
You can configure this in the security configuration (e.g. `etc/security/mh_default.org.xml`).
For example, to allow anonymous access set:

```xml
<sec:intercept-url pattern="/metrics" access="ROLE_ANONYMOUS" />
```


Prometheus Configuration
------------------------

There is nothing special when it comes to the Prometheus configuration:

```yml
- job_name: opencast
  scheme: https
  basic_auth:
    username: <oc-user>
    password: <oc-password>
  static_configs:
    - targets:
      - example.opencast.org
```
