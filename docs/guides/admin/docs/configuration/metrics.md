Metrics (OpenMetrics, Prometheus)
=================================

Opencast comes with a metrics endpoint that supports the [OpenMetrics format](https://openmetrics.io) and can be used by
tools like [Prometheus](https://prometheus.io). The endpoint is available at `/metrics`.


Available Metrics
-----------------

Opencast related metrics describe the whole cluster and will be identical on all nodes
while JVM metrics are specific to each node.

These available metrics allow monitoring the current cluster state when it comes to processing
and should allow for good alerting rules:

- How many workflows are being processed
- How many jobs are being processed
- Are there any services in a warning or error state
- How many events are in the asset manager

Here is a complete list of the available Opencast related metrics:

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
# HELP opencast_asset_manager_events Events in Asset Manager
# TYPE opencast_asset_manager_events gauge
opencast_asset_manager_events{organization="mh_default_org",} 1.0
```

Additionally, standard JVM metrics are exported providing information about e.g. memory and CPU usage, threads,
classloading, etc. Here is a complete list of the available JVM metrics with exemplary values:

```
# HELP process_cpu_seconds_total Total user and system CPU time spent in seconds.
# TYPE process_cpu_seconds_total counter
process_cpu_seconds_total 117.251697
# HELP process_start_time_seconds Start time of the process since unix epoch in seconds.
# TYPE process_start_time_seconds gauge
process_start_time_seconds 1.623394602475E9
# HELP process_open_fds Number of open file descriptors.
# TYPE process_open_fds gauge
process_open_fds 627.0
# HELP process_max_fds Maximum number of open file descriptors.
# TYPE process_max_fds gauge
process_max_fds 10240.0

# HELP jvm_memory_objects_pending_finalization The number of objects waiting in the finalizer queue.
# TYPE jvm_memory_objects_pending_finalization gauge
jvm_memory_objects_pending_finalization 0.0
# HELP jvm_memory_bytes_used Used bytes of a given JVM memory area.
# TYPE jvm_memory_bytes_used gauge
jvm_memory_bytes_used{area="heap",} 2.74153448E8
jvm_memory_bytes_used{area="nonheap",} 2.04918736E8
# HELP jvm_memory_bytes_committed Committed (bytes) of a given JVM memory area.
# TYPE jvm_memory_bytes_committed gauge
jvm_memory_bytes_committed{area="heap",} 4.37256192E8
jvm_memory_bytes_committed{area="nonheap",} 2.25353728E8
# HELP jvm_memory_bytes_max Max (bytes) of a given JVM memory area.
# TYPE jvm_memory_bytes_max gauge
jvm_memory_bytes_max{area="heap",} 1.073741824E9
jvm_memory_bytes_max{area="nonheap",} -1.0
# HELP jvm_memory_bytes_init Initial bytes of a given JVM memory area.
# TYPE jvm_memory_bytes_init gauge
jvm_memory_bytes_init{area="heap",} 1.34217728E8
jvm_memory_bytes_init{area="nonheap",} 7667712.0
# HELP jvm_memory_pool_bytes_used Used bytes of a given JVM memory pool.
# TYPE jvm_memory_pool_bytes_used gauge
jvm_memory_pool_bytes_used{pool="CodeHeap 'non-nmethods'",} 1361536.0
jvm_memory_pool_bytes_used{pool="Metaspace",} 1.36619544E8
jvm_memory_pool_bytes_used{pool="CodeHeap 'profiled nmethods'",} 3.937024E7
jvm_memory_pool_bytes_used{pool="Compressed Class Space",} 1.5748792E7
jvm_memory_pool_bytes_used{pool="G1 Eden Space",} 1.24780544E8
jvm_memory_pool_bytes_used{pool="G1 Old Gen",} 1.31547112E8
jvm_memory_pool_bytes_used{pool="G1 Survivor Space",} 1.7825792E7
jvm_memory_pool_bytes_used{pool="CodeHeap 'non-profiled nmethods'",} 1.1818624E7
# HELP jvm_memory_pool_bytes_committed Committed bytes of a given JVM memory pool.
# TYPE jvm_memory_pool_bytes_committed gauge
jvm_memory_pool_bytes_committed{pool="CodeHeap 'non-nmethods'",} 2555904.0
jvm_memory_pool_bytes_committed{pool="Metaspace",} 1.51691264E8
jvm_memory_pool_bytes_committed{pool="CodeHeap 'profiled nmethods'",} 3.9452672E7
jvm_memory_pool_bytes_committed{pool="Compressed Class Space",} 1.9791872E7
jvm_memory_pool_bytes_committed{pool="G1 Eden Space",} 2.33832448E8
jvm_memory_pool_bytes_committed{pool="G1 Old Gen",} 1.85597952E8
jvm_memory_pool_bytes_committed{pool="G1 Survivor Space",} 1.7825792E7
jvm_memory_pool_bytes_committed{pool="CodeHeap 'non-profiled nmethods'",} 1.1862016E7
# HELP jvm_memory_pool_bytes_max Max bytes of a given JVM memory pool.
# TYPE jvm_memory_pool_bytes_max gauge
jvm_memory_pool_bytes_max{pool="CodeHeap 'non-nmethods'",} 5836800.0
jvm_memory_pool_bytes_max{pool="Metaspace",} -1.0
jvm_memory_pool_bytes_max{pool="CodeHeap 'profiled nmethods'",} 1.22908672E8
jvm_memory_pool_bytes_max{pool="Compressed Class Space",} 1.073741824E9
jvm_memory_pool_bytes_max{pool="G1 Eden Space",} -1.0
jvm_memory_pool_bytes_max{pool="G1 Old Gen",} 1.073741824E9
jvm_memory_pool_bytes_max{pool="G1 Survivor Space",} -1.0
jvm_memory_pool_bytes_max{pool="CodeHeap 'non-profiled nmethods'",} 1.22912768E8
# HELP jvm_memory_pool_bytes_init Initial bytes of a given JVM memory pool.
# TYPE jvm_memory_pool_bytes_init gauge
jvm_memory_pool_bytes_init{pool="CodeHeap 'non-nmethods'",} 2555904.0
jvm_memory_pool_bytes_init{pool="Metaspace",} 0.0
jvm_memory_pool_bytes_init{pool="CodeHeap 'profiled nmethods'",} 2555904.0
jvm_memory_pool_bytes_init{pool="Compressed Class Space",} 0.0
jvm_memory_pool_bytes_init{pool="G1 Eden Space",} 2.7262976E7
jvm_memory_pool_bytes_init{pool="G1 Old Gen",} 1.06954752E8
jvm_memory_pool_bytes_init{pool="G1 Survivor Space",} 0.0
jvm_memory_pool_bytes_init{pool="CodeHeap 'non-profiled nmethods'",} 2555904.0
# HELP jvm_memory_pool_collection_used_bytes Used bytes after last collection of a given JVM memory pool.
# TYPE jvm_memory_pool_collection_used_bytes gauge
jvm_memory_pool_collection_used_bytes{pool="G1 Eden Space",} 0.0
jvm_memory_pool_collection_used_bytes{pool="G1 Old Gen",} 1.29009136E8
jvm_memory_pool_collection_used_bytes{pool="G1 Survivor Space",} 1.7825792E7
# HELP jvm_memory_pool_collection_committed_bytes Committed after last collection bytes of a given JVM memory pool.
# TYPE jvm_memory_pool_collection_committed_bytes gauge
jvm_memory_pool_collection_committed_bytes{pool="G1 Eden Space",} 2.33832448E8
jvm_memory_pool_collection_committed_bytes{pool="G1 Old Gen",} 2.21249536E8
jvm_memory_pool_collection_committed_bytes{pool="G1 Survivor Space",} 1.7825792E7
# HELP jvm_memory_pool_collection_max_bytes Max bytes after last collection of a given JVM memory pool.
# TYPE jvm_memory_pool_collection_max_bytes gauge
jvm_memory_pool_collection_max_bytes{pool="G1 Eden Space",} -1.0
jvm_memory_pool_collection_max_bytes{pool="G1 Old Gen",} 1.073741824E9
jvm_memory_pool_collection_max_bytes{pool="G1 Survivor Space",} -1.0
# HELP jvm_memory_pool_collection_init_bytes Initial after last collection bytes of a given JVM memory pool.
# TYPE jvm_memory_pool_collection_init_bytes gauge
jvm_memory_pool_collection_init_bytes{pool="G1 Eden Space",} 2.7262976E7
jvm_memory_pool_collection_init_bytes{pool="G1 Old Gen",} 1.06954752E8
jvm_memory_pool_collection_init_bytes{pool="G1 Survivor Space",} 0.0

# HELP jvm_memory_pool_allocated_bytes_created Total bytes allocated in a given JVM memory pool. Only updated after GC, not continuously.
# TYPE jvm_memory_pool_allocated_bytes_created gauge
jvm_memory_pool_allocated_bytes_created{pool="CodeHeap 'profiled nmethods'",} 1.623394629741E9
jvm_memory_pool_allocated_bytes_created{pool="G1 Old Gen",} 1.623394629746E9
jvm_memory_pool_allocated_bytes_created{pool="G1 Eden Space",} 1.623394629746E9
jvm_memory_pool_allocated_bytes_created{pool="CodeHeap 'non-profiled nmethods'",} 1.623394629746E9
jvm_memory_pool_allocated_bytes_created{pool="G1 Survivor Space",} 1.623394629746E9
jvm_memory_pool_allocated_bytes_created{pool="Compressed Class Space",} 1.623394629746E9
jvm_memory_pool_allocated_bytes_created{pool="Metaspace",} 1.623394629746E9
jvm_memory_pool_allocated_bytes_created{pool="CodeHeap 'non-nmethods'",} 1.623394629746E9

# HELP jvm_memory_pool_allocated_bytes_total Total bytes allocated in a given JVM memory pool. Only updated after GC, not continuously.
# TYPE jvm_memory_pool_allocated_bytes_total counter
jvm_memory_pool_allocated_bytes_total{pool="CodeHeap 'profiled nmethods'",} 3.7028864E7
jvm_memory_pool_allocated_bytes_total{pool="G1 Old Gen",} 1.3262048E8
jvm_memory_pool_allocated_bytes_total{pool="G1 Eden Space",} 8.74512384E8
jvm_memory_pool_allocated_bytes_total{pool="CodeHeap 'non-profiled nmethods'",} 1.145216E7
jvm_memory_pool_allocated_bytes_total{pool="G1 Survivor Space",} 1.8874368E7
jvm_memory_pool_allocated_bytes_total{pool="Compressed Class Space",} 1.4760208E7
jvm_memory_pool_allocated_bytes_total{pool="Metaspace",} 1.28033584E8
jvm_memory_pool_allocated_bytes_total{pool="CodeHeap 'non-nmethods'",} 1340416.0

# HELP jvm_buffer_pool_used_bytes Used bytes of a given JVM buffer pool.
# TYPE jvm_buffer_pool_used_bytes gauge
jvm_buffer_pool_used_bytes{pool="mapped",} 0.0
jvm_buffer_pool_used_bytes{pool="direct",} 290150.0
# HELP jvm_buffer_pool_capacity_bytes Bytes capacity of a given JVM buffer pool.
# TYPE jvm_buffer_pool_capacity_bytes gauge
jvm_buffer_pool_capacity_bytes{pool="mapped",} 0.0
jvm_buffer_pool_capacity_bytes{pool="direct",} 290150.0
# HELP jvm_buffer_pool_used_buffers Used buffers of a given JVM buffer pool.
# TYPE jvm_buffer_pool_used_buffers gauge
jvm_buffer_pool_used_buffers{pool="mapped",} 0.0
jvm_buffer_pool_used_buffers{pool="direct",} 32.0

# HELP jvm_gc_collection_seconds Time spent in a given JVM garbage collector in seconds.
# TYPE jvm_gc_collection_seconds summary
jvm_gc_collection_seconds_count{gc="G1 Young Generation",} 41.0
jvm_gc_collection_seconds_sum{gc="G1 Young Generation",} 0.447
jvm_gc_collection_seconds_count{gc="G1 Old Generation",} 0.0
jvm_gc_collection_seconds_sum{gc="G1 Old Generation",} 0.0

# HELP jvm_threads_current Current thread count of a JVM
# TYPE jvm_threads_current gauge
jvm_threads_current 188.0
# HELP jvm_threads_daemon Daemon thread count of a JVM
# TYPE jvm_threads_daemon gauge
jvm_threads_daemon 73.0
# HELP jvm_threads_peak Peak thread count of a JVM
# TYPE jvm_threads_peak gauge
jvm_threads_peak 188.0
# HELP jvm_threads_started_total Started thread count of a JVM
# TYPE jvm_threads_started_total counter
jvm_threads_started_total 211.0
# HELP jvm_threads_deadlocked Cycles of JVM-threads that are in deadlock waiting to acquire object monitors or ownable synchronizers
# TYPE jvm_threads_deadlocked gauge
jvm_threads_deadlocked 0.0
# HELP jvm_threads_deadlocked_monitor Cycles of JVM-threads that are in deadlock waiting to acquire object monitors
# TYPE jvm_threads_deadlocked_monitor gauge
jvm_threads_deadlocked_monitor 0.0
# HELP jvm_threads_state Current count of threads by state
# TYPE jvm_threads_state gauge
jvm_threads_state{state="BLOCKED",} 0.0
jvm_threads_state{state="TERMINATED",} 0.0
jvm_threads_state{state="WAITING",} 54.0
jvm_threads_state{state="TIMED_WAITING",} 78.0
jvm_threads_state{state="NEW",} 0.0
jvm_threads_state{state="RUNNABLE",} 56.0

# HELP jvm_classes_loaded The number of classes that are currently loaded in the JVM
# TYPE jvm_classes_loaded gauge
jvm_classes_loaded 21570.0
# HELP jvm_classes_loaded_total The total number of classes that have been loaded since the JVM has started execution
# TYPE jvm_classes_loaded_total counter
jvm_classes_loaded_total 21570.0
# HELP jvm_classes_unloaded_total The total number of classes that have been unloaded since the JVM has started execution
# TYPE jvm_classes_unloaded_total counter
jvm_classes_unloaded_total 0.0

# HELP jvm_info VM version info
# TYPE jvm_info gauge
jvm_info{runtime="OpenJDK Runtime Environment",vendor="Oracle Corporation",version="11.0.10+9",} 1.0
```

A corresponding monitoring mixin for JVM metrics with dashboards and alerting rules can be found at
[github.com/grafana/jsonnet-libs](https://github.com/grafana/jsonnet-libs/tree/master/jvm-mixin).

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
