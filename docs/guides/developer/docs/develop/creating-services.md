# Creating a Service

Most of Opencast is made of small services that run inside the OSGi container (Karaf) and find each other through
OSGi Declarative Services (DS). This page explains how to write such a service from scratch, what the individual
annotations do and how to make a service a job producer.

The three `hello-world` modules in the repository are a working, minimal version of everything described here. They are
deliberately not part of the Opencast distributions, so you cannot use them as a feature, but they are built with the
rest of the code and are the best place to start copying from:

| Module                                  | Contents                                         |
|-----------------------------------------|--------------------------------------------------|
| `modules/hello-world-api`               | The service interface                            |
| `modules/hello-world-impl`              | The service implementation and its REST endpoint |
| `modules/hello-world-workflowoperation` | A workflow operation handler                     |

For how jobs and services fit into the bigger picture, see the [architecture overview](../architecture/overview.md) and
[job dispatch](../architecture/job-dispatch.md).


## A minimal service

### API and implementation

Services are split into an `-api` module with the interface, and an `-impl` module with the implementation. Other
modules depend only on the API, so the implementation can change or be replaced without touching them. The service is
then a plain Java class registered as an OSGi service using the `@Component` annotation of DS:

```java
@Component(
    property = {
        "service.description=Hello World Service"
    },
    immediate = true,
    service = HelloWorldService.class
)
public class HelloWorldServiceImpl implements HelloWorldService {
  // ...
}
```

(`HelloWorldServiceImpl`, abridged.)

- `service` lists the interface or interfaces under which the service is registered. Other components ask for the
  service by this type.
- `service.description` is a human readable name that shows up in the Karaf console.
- `immediate` is described in the next section.

Everything the service needs from other services is injected with `@Reference`, either on a setter method or on a
constructor parameter. Do not look services up manually.


### What `immediate` does

DS knows two kinds of components. A *delayed* component is registered as soon as its dependencies are available, but its
instance is only created, and `@Activate` only called, when some other component first asks for the service. An
*immediate* component is activated as soon as its dependencies are satisfied, whether or not anyone is using it.

A component that provides a service is delayed by default. In Opencast, the vast majority of components set
`immediate = true`, because they have to be running for the system to work even though nobody calls them directly:

- REST endpoints, which are picked up by the REST publisher and, for job producers, the service registry
- event handlers and schedulers that react to messages or run periodically
- services that load their configuration or start background work when they are activated

If you are unsure, use `immediate = true`. Only leave it out for components that are pure helpers and should not cost
anything unless someone uses them.


### Configuration

A component is configured from a file in `etc/` named after its PID. By default, the PID is the fully qualified class
name, so the hello-world service reads `etc/org.opencastproject.helloworld.impl.HelloWorldServiceImpl.cfg`. The values
are passed to the method annotated with `@Activate` as a map. Add `@Modified` to the same method to apply changes of
the file without restarting the component:

```java
@Activate
@Modified
void activate(Map<String, Object> properties) {
  message = Objects.toString(properties.get("message"), "Hello World");
}
```

This single method is called when the component starts and every time the configuration changes. Without a `@Modified`
method, DS stops and restarts the whole component on every configuration change instead.

For values that are looked up in the bundle context rather than in a `.cfg` file, such as entries from
`etc/custom.properties`, use the `BundleContext` or `ComponentContext` that can be injected into `@Activate`.


## Exposing a REST endpoint

Services are made available over HTTP by a separate component, the REST endpoint. Keeping it separate from the service
means that the service can be used and tested without any HTTP code. See `HelloWorldRestEndpoint`:

```java
@Component(
    property = {
        "service.description=Hello World REST Endpoint",
        "opencast.service.type=org.opencastproject.helloworld",
        "opencast.service.path=/helloworld",
        "opencast.service.jobproducer=false"
    },
    immediate = true,
    service = HelloWorldRestEndpoint.class
)
@Path("/helloworld")
@RestService(
    name = "HelloWorldServiceEndpoint",
    title = "Hello World Service Endpoint",
    abstractText = "This is a tutorial service.",
    notes = { /* ... */ }
)
@JaxrsResource
public class HelloWorldRestEndpoint {
  // ...
}
```

- `@Path` is the path below which the endpoint is served and has to match `opencast.service.path`.
- `opencast.service.type` is the type under which the endpoint is registered in the service registry. For job
  producers it must match the job type, see below.
- `@RestService` and the `@RestQuery` annotations on the methods are not only comments. They generate the REST
  documentation page, which is available under the `/docs` path of the endpoint, for example `/helloworld/docs`. You do
  not need to implement that path yourself.
- `@JaxrsResource` marks the component as a JAX-RS resource by setting the OSGi service property
  `osgi.jaxrs.resource=true`. The REST publisher uses that property to find the endpoints it has to serve, and the
  service registry uses it to find the endpoints it has to register. Without it the component still starts and looks
  fine in the Karaf console, but the endpoint is not served and, for a job producer, never receives any jobs. There is
  no error message.


## Job producers

Work that takes a while, such as encoding a video, is done in *jobs*, so that it can be spread over the nodes of a
cluster. A service that creates such jobs is a *job producer*. When it creates a job, the service registry stores it.
The job dispatcher then picks a node that offers the same service and has capacity, and the job producer on that node
processes the job. This can be a different node than the one that created the job. See
[job dispatch](../architecture/job-dispatch.md) for how the dispatcher decides.

Making a service a job producer takes two things. The examples below come from the media inspection service
(`MediaInspectionServiceImpl` and `MediaInspectionRestEndpoint` in `modules/inspection-service-ffmpeg`), which is a
typical job producer.

**1. The service extends `AbstractJobProducer`** and implements `process()`. The constructor takes the *job type*, a
string that identifies the service in the service registry. The job type is usually a constant in the API interface:
`MediaInspectionService.JOB_TYPE` is `org.opencastproject.inspection`.

Creating a job does not do any work. It stores the job in the service registry, together with the name of an operation,
its arguments and its load. The dispatcher later assigns the job to a node, where `process()` is called to do the work.
The string it returns is stored as the payload of the job.

```java
public class MediaInspectionServiceImpl extends AbstractJobProducer implements MediaInspectionService {

  private enum Operation {
    Inspect, Enrich
  }

  public MediaInspectionServiceImpl() {
    super(JOB_TYPE);
  }

  @Override
  public Job inspect(URI uri, Map<String, String> options) throws MediaInspectionException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Inspect.toString(),
          Arrays.asList(uri.toString(), Options.toJson(options)), inspectJobLoad);
    } catch (ServiceRegistryException e) {
      throw new MediaInspectionException(e);
    }
  }

  @Override
  protected String process(Job job) throws Exception {
    Operation op = Operation.valueOf(job.getOperation());
    List<String> arguments = job.getArguments();
    switch (op) {
      case Inspect:
        // ... inspect the file, return the result as the payload of the job
      // ...
    }
  }
}
```

(Abridged.) A job producer has to provide the service registry, the security service, the user directory service and the
organization directory service to the base class. The base class declares an abstract getter for each of them, so the
service has to implement the getters, and it receives the services through `@Reference` setters, as
`MediaInspectionServiceImpl` does.

**2. The REST endpoint extends `AbstractJobProducerEndpoint`**, sets `opencast.service.jobproducer=true` and uses the
job type of the service as `opencast.service.type`. The base class implements the `/dispatch` path through which the
dispatcher hands a job to the node. The endpoint only has to tell it which job producer and which service registry to
use.

```java
@Component(
    property = {
        "service.description=Media Inspection REST Endpoint",
        "opencast.service.type=org.opencastproject.inspection",
        "opencast.service.path=/inspection",
        "opencast.service.jobproducer=true"
    },
    immediate = true,
    service = { MediaInspectionRestEndpoint.class }
)
@JaxrsResource
public class MediaInspectionRestEndpoint extends AbstractJobProducerEndpoint {

  // ... the @Reference setters for the service and the service registry

  @Override
  public JobProducer getService() {
    return (JobProducer) service;
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  // ... the actual REST methods
}
```

(Abridged. The real class checks whether the service is a `JobProducer` before returning it.)

The type has to match because the `RestServiceTracker` in the service registry module registers every published REST
endpoint with the type, host and path from its properties, and a job of a given type is only dispatched to hosts where
a job producer with that type is registered. In the example above, `MediaInspectionService.JOB_TYPE` and
`opencast.service.type` are both `org.opencastproject.inspection`.

Jobs also have a *load*, which tells the dispatcher how much of a node's capacity the job needs. As in the example
above, it is passed to `createJob`, and it should be configurable. `MediaInspectionServiceImpl` reads its load values
in a `@Modified` method, like any other configuration. What the load values mean and how administrators set them is
described in the "Job Load" page of the administration guide.


## `ManagedService` and why not to use it

Older code, and plenty of code that was copied from it, implements `org.osgi.service.cm.ManagedService` (or
`ManagedServiceFactory`) to receive configuration through a method called `updated`. Do not use this in new code. Use
`@Activate` and `@Modified` as shown above.

The reason is that DS already does the job of a `ManagedService` and the two interact badly. If a component does not
have a `@Modified` method, DS restarts it on every configuration change. A component that is also a `ManagedService`
therefore receives the new configuration in `updated`, and then receives it a second time after the restart. If the
configuration is broken, for example a number that is not a number, `updated` throws and logs the same exception twice,
and the first call is wasted because the component is shut down right after. With `@Activate` and `@Modified` there is
one code path for the initial and for every later configuration, and no second mechanism to reason about.

Many older services in the code base still implement `ManagedService`. Do not copy them for new code.

If you need one service instance per configuration file, for example one user directory provider per organization, use
the factory configuration of DS instead of a `ManagedServiceFactory`: set `configurationPid` and
`configurationPolicy = ConfigurationPolicy.REQUIRE` on the component and name the files `<pid>-<name>.cfg`. For example,
`StudipUserProviderInstance` is created once for each `org.opencastproject.userdirectory.studip-*.cfg` file in `etc/`.


## Checklist for a new service

- API interface in an `-api` module, implementation in an `-impl` module
- `@Component` with `service`, a `service.description` and, as a rule, `immediate = true`
- Dependencies injected with `@Reference`
- Configuration through `@Activate` and `@Modified`, never `ManagedService`
- An optional REST endpoint component with `@JaxrsResource`, `@RestService` and `@RestQuery` documentation
- Long-running work as a job producer, with matching job and endpoint type
- A unit test of the service that creates the class with `new` and sets its dependencies by hand, like
  `HelloWorldServiceTest`
