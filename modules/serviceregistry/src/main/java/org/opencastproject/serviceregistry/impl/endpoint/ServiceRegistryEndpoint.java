/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.serviceregistry.impl.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.JaxbJobList;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.JaxbHostRegistration;
import org.opencastproject.serviceregistry.api.JaxbHostRegistrationList;
import org.opencastproject.serviceregistry.api.JaxbServiceHealth;
import org.opencastproject.serviceregistry.api.JaxbServiceRegistration;
import org.opencastproject.serviceregistry.api.JaxbServiceRegistrationList;
import org.opencastproject.serviceregistry.api.JaxbServiceStatisticsList;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceState;
import org.opencastproject.serviceregistry.api.SystemLoad;
import org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.osgi.service.component.ComponentContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Displays hosts and the service IDs they provide.
 */
@Path("/")
@RestService(name = "serviceregistry", title = "Service Registry", notes = { "All paths above are relative to the REST endpoint base" }, abstractText = "Provides registration and management functions for servers and services in this Opencast instance or cluster.")
public class ServiceRegistryEndpoint {

  /** The remote service maanger */
  protected ServiceRegistry serviceRegistry = null;

  /** This server's URL */
  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  /** The service path for this endpoint */
  protected String servicePath = "/";

  /** Sets the service registry instance for delegation */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Callback from OSGi that is called when this service is activated.
   *
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
    serverUrl = cc.getBundleContext().getProperty(OpencastConstants.SERVER_URL_PROPERTY);
    servicePath = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
  }

  @GET
  @Path("statistics.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "statisticsasjson", description = "List the service registrations in the cluster, along with some simple statistics", returnDescription = "The service statistics.", reponses = { @RestResponse(responseCode = SC_OK, description = "A JSON representation of the service statistics") })
  public Response getStatisticsAsJson() {
    try {
      return Response.ok(new JaxbServiceStatisticsList(serviceRegistry.getServiceStatistics())).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("statistics.xml")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "statisticsasxml", description = "List the service registrations in the cluster, along with some simple statistics", returnDescription = "The service statistics.", reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the service statistics") })
  public Response getStatisticsAsXml() throws ServiceRegistryException {
    return getStatisticsAsJson();
  }

  @GET
  @Path("servicewarnings")
  @RestQuery(name = "servicewarnings", description = "Get the number of services currently in a non-NORMAL state", returnDescription = "The count of abnormal services.", reponses = { @RestResponse(responseCode = SC_OK, description = "A plain text representation of the number of abnormal services") })
  public long serviceWarnings() throws ServiceRegistryException {
    return serviceRegistry.countOfAbnormalServices();
  }

  @POST
  @Path("sanitize")
  @RestQuery(name = "sanitize", description = "Sets the given service to NORMAL state", returnDescription = "No content", restParameters = {
          @RestParameter(name = "serviceType", isRequired = true, description = "The service type identifier", type = Type.STRING, defaultValue = ""),
          @RestParameter(name = "host", isRequired = true, description = "The host providing the service, including the http(s) protocol", type = Type.STRING, defaultValue = "") }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The service was successfully sanitized"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No service of that type on that host is registered.") })
  public Response sanitize(@FormParam("serviceType") String serviceType, @FormParam("host") String host)
          throws NotFoundException {
    serviceRegistry.sanitize(serviceType, host);
    return Response.status(Status.NO_CONTENT).build();
  }

  @POST
  @Path("register")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "register", description = "Add a new service registration to the cluster.", returnDescription = "The service registration.", restParameters = {
          @RestParameter(name = "serviceType", isRequired = true, description = "The service type identifier", type = Type.STRING, defaultValue = ""),
          @RestParameter(name = "host", isRequired = true, description = "The host providing the service, including the http(s) protocol", type = Type.STRING, defaultValue = ""),
          @RestParameter(name = "path", isRequired = true, description = "The service path on the host", type = Type.STRING, defaultValue = ""),
          @RestParameter(name = "jobProducer", isRequired = true, description = "Whether this service is a producer of long running jobs requiring dispatch", type = Type.STRING, defaultValue = "false") }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the new service registration") })
  public JaxbServiceRegistration register(@FormParam("serviceType") String serviceType, @FormParam("host") String host,
          @FormParam("path") String path, @FormParam("jobProducer") boolean jobProducer) {
    try {
      return new JaxbServiceRegistration(serviceRegistry.registerService(serviceType, host, path, jobProducer));
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("unregister")
  @RestQuery(name = "unregister", description = "Removes a service registration.", returnDescription = "No content", restParameters = {
          @RestParameter(name = "serviceType", isRequired = true, description = "The service type identifier", type = Type.STRING),
          @RestParameter(name = "host", isRequired = true, description = "The host providing the service, including the http(s) protocol", type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_NO_CONTENT, description = "The service was unregistered successfully") })
  public Response unregister(@FormParam("serviceType") String serviceType, @FormParam("host") String host) {
    try {
      serviceRegistry.unRegisterService(serviceType, host);
      return Response.status(Status.NO_CONTENT).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("enablehost")
  @RestQuery(name = "enablehost", description = "Enable a server from the cluster.", returnDescription = "No content.", restParameters = { @RestParameter(name = "host", isRequired = true, description = "The host name, including the http(s) protocol", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The host was enabled successfully"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The host does not exist") })
  public Response enableHost(@FormParam("host") String host) throws NotFoundException {
    try {
      serviceRegistry.enableHost(host);
      return Response.status(Status.NO_CONTENT).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("disablehost")
  @RestQuery(name = "disablehost", description = "Disable a server from the cluster.", returnDescription = "No content.", restParameters = { @RestParameter(name = "host", isRequired = true, description = "The host name, including the http(s) protocol", type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The host was disabled successfully"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The host does not exist") })
  public Response disableHost(@FormParam("host") String host) throws NotFoundException {
    try {
      serviceRegistry.disableHost(host);
      return Response.status(Status.NO_CONTENT).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("registerhost")
  @RestQuery(name = "registerhost", description = "Add a new server to the cluster.", returnDescription = "No content.", restParameters = {
          @RestParameter(name = "host", isRequired = true, description = "The host name, including the http(s) protocol", type = Type.STRING),
          @RestParameter(name = "address", isRequired = true, description = "The IP address", type = Type.STRING),
          @RestParameter(name = "nodeName", isRequired = true, description = "Descriptive node name", type = Type.STRING),
          @RestParameter(name = "memory", isRequired = true, description = "The allocated memory", type = Type.STRING),
          @RestParameter(name = "cores", isRequired = true, description = "The available cores", type = Type.STRING),
          @RestParameter(name = "maxLoad", isRequired = true, description = "The maximum load this host support", type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_NO_CONTENT, description = "The host was registered successfully") })
  public void register(@FormParam("host") String host, @FormParam("address") String address, @FormParam("nodeName") String nodeName,
          @FormParam("memory") long memory, @FormParam("cores") int cores, @FormParam("maxLoad") float maxLoad) {
    try {
      serviceRegistry.registerHost(host, address, nodeName, memory, cores, maxLoad);
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("unregisterhost")
  @RestQuery(name = "unregisterhost", description = "Removes a server from the cluster.",
          returnDescription = "No content.",
          restParameters = {
          @RestParameter(name = "host", isRequired = true, description = "The host name, including the http(s) protocol", type = Type.STRING)
          }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The host was removed successfully") })
  public Response unregister(@FormParam("host") String host) {
    try {
      serviceRegistry.unregisterHost(host);
      return Response.status(Status.NO_CONTENT).build();
    } catch (ServiceRegistryException e) {
      if (e.getCause() instanceof IllegalArgumentException) {
        return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
      }
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("maintenance")
  @RestQuery(name = "maintenance", description = "Sets the maintenance status for a server in the cluster.", returnDescription = "No content.", restParameters = {
          @RestParameter(name = "host", isRequired = true, type = Type.STRING, description = "The host name, including the http(s) protocol"),
          @RestParameter(name = "maintenance", isRequired = true, type = Type.BOOLEAN, description = "Whether this host should be put into maintenance mode (true) or not") }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The host was registered successfully"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Host not found") })
  public Response setMaintenanceMode(@FormParam("host") String host, @FormParam("maintenance") boolean maintenance)
          throws NotFoundException {
    try {
      serviceRegistry.setMaintenanceStatus(host, maintenance);
      return Response.status(Status.NO_CONTENT).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("available.xml")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "availableasxml", description = "Lists available services by service type identifier, ordered by load.", returnDescription = "The services list as XML", restParameters = { @RestParameter(name = "serviceType", isRequired = false, type = Type.STRING, description = "The service type identifier") }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "Returned the available services."),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "No service type specified, bad request.") })
  public Response getAvailableServicesAsXml(@QueryParam("serviceType") String serviceType) {
    if (isBlank(serviceType))
      throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Service type must be specified")
              .build());
    JaxbServiceRegistrationList registrations = new JaxbServiceRegistrationList();
    try {
      for (ServiceRegistration reg : serviceRegistry.getServiceRegistrationsByLoad(serviceType)) {
        registrations.add(new JaxbServiceRegistration(reg));
      }
      return Response.ok(registrations).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("available.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "availableasjson", description = "Lists available services by service type identifier, ordered by load.", returnDescription = "The services list as JSON", restParameters = { @RestParameter(name = "serviceType", isRequired = false, type = Type.STRING, description = "The service type identifier") }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "Returned the available services."),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "No service type specified, bad request.") })
  public Response getAvailableServicesAsJson(@QueryParam("serviceType") String serviceType) {
    return getAvailableServicesAsXml(serviceType);
  }

  @GET
  @Path("health.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "health", description = "Checks the status of the registered services", returnDescription = "Returns NO_CONTENT if services are in a proper state", restParameters = {
          @RestParameter(name = "serviceType", isRequired = false, type = Type.STRING, description = "The service type identifier"),
          @RestParameter(name = "host", isRequired = false, type = Type.STRING, description = "The host, including the http(s) protocol") }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "Service states returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No service of that type on that host is registered."),
          @RestResponse(responseCode = SC_SERVICE_UNAVAILABLE, description = "An error has occurred during stats processing") })
  public Response getHealthStatusAsJson(@QueryParam("serviceType") String serviceType, @QueryParam("host") String host)
          throws NotFoundException {
    return getHealthStatus(serviceType, host);
  }

  @GET
  @Path("health.xml")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(name = "health", description = "Checks the status of the registered services", returnDescription = "Returns NO_CONTENT if services are in a proper state", restParameters = {
          @RestParameter(name = "serviceType", isRequired = false, type = Type.STRING, description = "The service type identifier"),
          @RestParameter(name = "host", isRequired = false, type = Type.STRING, description = "The host, including the http(s) protocol") }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "Service states returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No service of that type on that host is registered."),
          @RestResponse(responseCode = SC_SERVICE_UNAVAILABLE, description = "An error has occurred during stats processing") })
  public Response getHealthStatus(@QueryParam("serviceType") String serviceType, @QueryParam("host") String host)
          throws NotFoundException {
    try {
      List<ServiceRegistration> services = null;
      if (isNotBlank(serviceType) && isNotBlank(host)) {
        // This is a request for one specific service. Return it, or SC_NOT_FOUND if not found
        ServiceRegistration reg = serviceRegistry.getServiceRegistration(serviceType, host);
        if (reg == null)
          throw new NotFoundException();

        services = new LinkedList<ServiceRegistration>();
        services.add(reg);
      } else if (isBlank(serviceType) && isBlank(host)) {
        // This is a request for all service registrations
        services = serviceRegistry.getServiceRegistrations();
      } else if (isNotBlank(serviceType)) {
        // This is a request for all service registrations of a particular type
        services = serviceRegistry.getServiceRegistrationsByType(serviceType);
      } else if (isNotBlank(host)) {
        // This is a request for all service registrations of a particular host
        services = serviceRegistry.getServiceRegistrationsByHost(host);
      }

      int healthy = 0;
      int warning = 0;
      int error = 0;
      for (ServiceRegistration reg : services) {
        if (ServiceState.NORMAL == reg.getServiceState()) {
          healthy++;
        } else if (ServiceState.WARNING == reg.getServiceState()) {
          warning++;
        } else if (ServiceState.ERROR == reg.getServiceState()) {
          error++;
        } else {
           error++;
        }
      }
      JaxbServiceHealth stats = new JaxbServiceHealth(healthy, warning, error);
      return Response.ok(stats).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("services.xml")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "servicesasxml", description = "Returns a service registraton or list of available service registrations as XML.", returnDescription = "The services list as XML", restParameters = {
          @RestParameter(name = "serviceType", isRequired = false, type = Type.STRING, description = "The service type identifier"),
          @RestParameter(name = "host", isRequired = false, type = Type.STRING, description = "The host, including the http(s) protocol") }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "Returned the available service."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No service of that type on that host is registered.") })
  public JaxbServiceRegistrationList getRegistrationsAsXml(@QueryParam("serviceType") String serviceType,
          @QueryParam("host") String host) throws NotFoundException {
    JaxbServiceRegistrationList registrations = new JaxbServiceRegistrationList();
    try {
      if (isNotBlank(serviceType) && isNotBlank(host)) {
        // This is a request for one specific service. Return it, or SC_NOT_FOUND if not found
        ServiceRegistration reg = serviceRegistry.getServiceRegistration(serviceType, host);
        if (reg == null) {
          throw new NotFoundException();
        } else {
          return new JaxbServiceRegistrationList(new JaxbServiceRegistration(reg));
        }
      } else if (isBlank(serviceType) && isBlank(host)) {
        // This is a request for all service registrations
        for (ServiceRegistration reg : serviceRegistry.getServiceRegistrations()) {
          registrations.add(new JaxbServiceRegistration(reg));
        }
      } else if (isNotBlank(serviceType)) {
        // This is a request for all service registrations of a particular type
        for (ServiceRegistration reg : serviceRegistry.getServiceRegistrationsByType(serviceType)) {
          registrations.add(new JaxbServiceRegistration(reg));
        }
      } else if (isNotBlank(host)) {
        // This is a request for all service registrations of a particular host
        for (ServiceRegistration reg : serviceRegistry.getServiceRegistrationsByHost(host)) {
          registrations.add(new JaxbServiceRegistration(reg));
        }
      }
      return registrations;
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("services.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "servicesasjson", description = "Returns a service registraton or list of available service registrations as JSON.", returnDescription = "The services list as XML", restParameters = {
          @RestParameter(name = "serviceType", isRequired = false, type = Type.STRING, description = "The service type identifier"),
          @RestParameter(name = "host", isRequired = false, type = Type.STRING, description = "The host, including the http(s) protocol") }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "Returned the available service."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No service of that type on that host is registered.") })
  public JaxbServiceRegistrationList getRegistrationsAsJson(@QueryParam("serviceType") String serviceType,
          @QueryParam("host") String host) throws NotFoundException {
    return getRegistrationsAsXml(serviceType, host);
  }

  @GET
  @Path("hosts.xml")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "hostsasxml", description = "Returns a host registraton or list of available host registrations as XML.", returnDescription = "The host list as XML", reponses = { @RestResponse(responseCode = SC_OK, description = "Returned the available hosts.") })
  public JaxbHostRegistrationList getHostsAsXml() throws NotFoundException {
    JaxbHostRegistrationList registrations = new JaxbHostRegistrationList();
    try {
      for (HostRegistration reg : serviceRegistry.getHostRegistrations())
        registrations.add(new JaxbHostRegistration(reg));
      return registrations;
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("hosts.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "hostsasjson", description = "Returns a host registraton or list of available host registrations as JSON.", returnDescription = "The host list as JSON", reponses = { @RestResponse(responseCode = SC_OK, description = "Returned the available hosts.") })
  public JaxbHostRegistrationList getHostsAsJson() throws NotFoundException {
    return getHostsAsXml();
  }

  @POST
  @Path("job")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "createjob", description = "Creates a new job.", returnDescription = "An XML representation of the job.", restParameters = {
          @RestParameter(name = "jobType", isRequired = true, type = Type.STRING, description = "The job type identifier"),
          @RestParameter(name = "host", isRequired = true, type = Type.STRING, description = "The creating host, including the http(s) protocol"),
          @RestParameter(name = "operation", isRequired = true, type = Type.STRING, description = "The operation this job should execute"),
          @RestParameter(name = "payload", isRequired = false, type = Type.TEXT, description = "The job type identifier"),
          @RestParameter(name = "start", isRequired = false, type = Type.BOOLEAN, description = "Whether the job should be queued for dispatch and execution"),
          @RestParameter(name = "jobLoad", isRequired = false, type = Type.STRING, description = "The load this job will incur on the system"),
          @RestParameter(name = "arg", isRequired = false, type = Type.TEXT, description = "An argument for the operation"),
          @RestParameter(name = "arg", isRequired = false, type = Type.TEXT, description = "An argument for the operation"),
          @RestParameter(name = "arg", isRequired = false, type = Type.TEXT, description = "An argument for the operation"),
          @RestParameter(name = "arg", isRequired = false, type = Type.TEXT, description = "An argument for the operation"),
          @RestParameter(name = "arg", isRequired = false, type = Type.TEXT, description = "An argument for the operation") }, reponses = {
          @RestResponse(responseCode = SC_CREATED, description = "Job created."),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required parameters were not supplied, bad request.") })
  public Response createJob(@Context HttpServletRequest request) {
    String[] argArray = request.getParameterValues("arg");
    List<String> arguments = null;
    if (argArray != null && argArray.length > 0) {
      arguments = Arrays.asList(argArray);
    }
    String jobType = request.getParameter("jobType");
    String operation = request.getParameter("operation");
    String host = request.getParameter("host");
    String payload = request.getParameter("payload");
    boolean start = StringUtils.isBlank(request.getParameter("start"))
            || Boolean.TRUE.toString().equalsIgnoreCase(request.getParameter("start"));
    try {
      Job job = null;
      if (StringUtils.isNotBlank(request.getParameter("jobLoad"))) {
        Float jobLoad = Float.parseFloat(request.getParameter("jobLoad"));
        job = ((ServiceRegistryJpaImpl) serviceRegistry).createJob(host, jobType, operation, arguments, payload,
              start, serviceRegistry.getCurrentJob(), jobLoad);
      } else {
        job = ((ServiceRegistryJpaImpl) serviceRegistry).createJob(host, jobType, operation, arguments, payload,
                start, serviceRegistry.getCurrentJob());
      }
      return Response.created(job.getUri()).entity(new JaxbJob(job)).build();
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(Status.BAD_REQUEST);
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @PUT
  @Path("job/{id}.xml")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "updatejob", description = "Updates an existing job", returnDescription = "No content", pathParameters = { @RestParameter(name = "id", isRequired = true, type = Type.STRING, description = "The job identifier") }, restParameters = { @RestParameter(name = "job", isRequired = true, type = Type.TEXT, description = "The updated job as XML") }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "Job updated."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Job not found.") })
  public Response updateJob(@PathParam("id") String id, @FormParam("job") String jobXml) throws NotFoundException {
    try {
      Job job = JobParser.parseJob(jobXml);
      serviceRegistry.updateJob(job);
      return Response.status(Status.NO_CONTENT).build();
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("job/{id}.xml")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "jobasxml", description = "Returns a job as XML.", returnDescription = "The job as XML", pathParameters = { @RestParameter(name = "id", isRequired = true, type = Type.STRING, description = "The job identifier") }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "Job found."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No job with that identifier exists.") })
  public JaxbJob getJobAsXml(@PathParam("id") long id) throws NotFoundException {
    return getJobAsJson(id);
  }

  @GET
  @Path("job/{id}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "jobasjson", description = "Returns a job as JSON.", returnDescription = "The job as JSON", pathParameters = { @RestParameter(name = "id", isRequired = true, type = Type.STRING, description = "The job identifier") }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "Job found."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No job with that identifier exists.") })
  public JaxbJob getJobAsJson(@PathParam("id") long id) throws NotFoundException {
    try {
      return new JaxbJob(serviceRegistry.getJob(id));
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("job/{id}/children.xml")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "childrenjobsasxml", description = "Returns all children from a job as XML.", returnDescription = "A list of children jobs as XML", pathParameters = { @RestParameter(name = "id", isRequired = true, type = Type.STRING, description = "The parent job identifier") }, reponses = { @RestResponse(responseCode = SC_OK, description = "Jobs found.") })
  public JaxbJobList getChildrenJobsAsXml(@PathParam("id") long id) {
    return getChildrenJobsAsJson(id);
  }

  @GET
  @Path("job/{id}/children.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "childrenjobsasjson", description = "Returns all children from a job as JSON.", returnDescription = "A list of children jobs as JSON", pathParameters = { @RestParameter(name = "id", isRequired = true, type = Type.STRING, description = "The parent job identifier") }, reponses = { @RestResponse(responseCode = SC_OK, description = "Jobs found.") })
  public JaxbJobList getChildrenJobsAsJson(@PathParam("id") long id) {
    try {
      return new JaxbJobList(serviceRegistry.getChildJobs(id));
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("jobs.xml")
  @Produces(MediaType.TEXT_XML)
  public JaxbJobList getJobsAsXml(@QueryParam("serviceType") String serviceType, @QueryParam("status") Job.Status status) {
    try {
      return new JaxbJobList(serviceRegistry.getJobs(serviceType, status));
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }

  }

  @GET
  @Path("activeJobs.xml")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "activejobsasxml",
          description = "Returns all active jobs as XML.",
          returnDescription = "A list of active jobs as XML",
          reponses = { @RestResponse(responseCode = SC_OK, description = "Active jobs found.") })
  public JaxbJobList getActiveJobsAsXml() {
    try {
      return new JaxbJobList(serviceRegistry.getActiveJobs());
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("activeJobs.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "activejobsasjson",
          description = "Returns all active jobs as JSON.",
          returnDescription = "A list of active jobs as JSON",
          reponses = { @RestResponse(responseCode = SC_OK, description = "Active jobs found.") })
  public JaxbJobList getActiveJobsAsJson() {
    try {
      return new JaxbJobList(serviceRegistry.getActiveJobs());
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("count")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "count", description = "Returns the number of jobs matching the query parameters as plain text.", returnDescription = "The number of matching jobs", restParameters = {
          @RestParameter(name = "serviceType", isRequired = false, type = Type.STRING, description = "The service type identifier"),
          @RestParameter(name = "status", isRequired = false, type = Type.STRING, description = "The job status"),
          @RestParameter(name = "host", isRequired = false, type = Type.STRING, description = "The host executing the job"),
          @RestParameter(name = "operation", isRequired = false, type = Type.STRING, description = "The job's operation") }, reponses = { @RestResponse(responseCode = SC_OK, description = "Job count returned.") })
  public long count(@QueryParam("serviceType") String serviceType, @QueryParam("status") Job.Status status,
          @QueryParam("host") String host, @QueryParam("operation") String operation) {
    try {
      if (isNotBlank(host) && isNotBlank(operation)) {
        if (isBlank(serviceType))
          throw new WebApplicationException(Response.serverError().entity("Service type must not be null").build());
        return serviceRegistry.count(serviceType, host, operation, status);
      } else if (isNotBlank(host)) {
        if (isBlank(serviceType))
          throw new WebApplicationException(Response.serverError().entity("Service type must not be null").build());
        return serviceRegistry.countByHost(serviceType, host, status);
      } else if (isNotBlank(operation)) {
        if (isBlank(serviceType))
          throw new WebApplicationException(Response.serverError().entity("Service type must not be null").build());
        return serviceRegistry.countByOperation(serviceType, operation, status);
      } else {
        return serviceRegistry.count(serviceType, status);
      }
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("maxconcurrentjobs")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "maxconcurrentjobs", description = "Returns the number of jobs that the servers in this service registry can execute concurrently. If there is only one server in this service registry this will be the number of jobs that one server is able to do at one time. If it is a distributed install across many servers then this number will be the total number of jobs the cluster can process concurrently.", returnDescription = "The maximum number of concurrent jobs", reponses = { @RestResponse(responseCode = SC_OK, description = "Maximum number of concurrent jobs returned.") })
  @Deprecated
  public Response getMaximumConcurrentWorkflows() {
    return Response.status(Status.MOVED_PERMANENTLY).type(MediaType.TEXT_PLAIN)
            .header("Location", servicePath + "/maxload").build();
  }

  @GET
  @Path("maxload")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "maxload", description = "Returns the maximum load that servers in this service registry can execute concurrently.  "
          + "If there is only one server in this service registry this will be the maximum load that one server is able to handle at one time.  "
          + "If it is a distributed install across many servers then this number will be the maximum load the cluster can process concurrently.",
          returnDescription = "The maximum load of the cluster or server", restParameters = {
              @RestParameter(name = "host", isRequired = false, type = Type.STRING, description = "The host you want to know the maximum load for.")
          }, reponses = { @RestResponse(responseCode = SC_OK, description = "Maximum load for the cluster.") })
  public Response getMaxLoadOnNode(@QueryParam("host") String host) throws NotFoundException {
    try {
      if (StringUtils.isEmpty(host)) {
        return Response.ok(serviceRegistry.getMaxLoads()).build();
      } else {
        SystemLoad systemLoad = new SystemLoad();
        systemLoad.addNodeLoad(serviceRegistry.getMaxLoadOnNode(host));
        return Response.ok(systemLoad).build();
      }
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("currentload")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "currentload", description = "Returns the current load on the servers in this service registry.  "
          + "If there is only one server in this service registry this will be the the load that one server.  "
          + "If it is a distributed install across many servers then this number will be a dictionary of the load on all nodes in the cluster.",
          returnDescription = "The current load across the cluster", restParameters = {},
          reponses = { @RestResponse(responseCode = SC_OK, description = "Current load for the cluster.") })
  public Response getCurrentLoad() {
    try {
      return Response.ok(serviceRegistry.getCurrentHostLoads()).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("ownload")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "ownload", description = "Returns the current load on this service registry's node.",
          returnDescription = "The current load across the cluster", restParameters = {},
          reponses = { @RestResponse(responseCode = SC_OK, description = "Current load for the cluster.") })
  public Response getOwnLoad() {
    try {
      return Response.ok(serviceRegistry.getOwnLoad()).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }


  @DELETE
  @Path("job/{id}")
  @RestQuery(name = "deletejob", description = "Deletes a job from the service registry", returnDescription = "No data is returned, just the HTTP status code", pathParameters = { @RestParameter(isRequired = true, name = "id", type = Type.INTEGER, description = "ID of the job to delete") }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "Job successfully deleted"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Job with given id could not be found") })
  public Response deleteJob(@PathParam("id") long id) throws NotFoundException {
    try {
      serviceRegistry.removeJobs(Collections.singletonList(id));
      return Response.noContent().build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }


  @POST
  @Path("removejobs")
  @RestQuery(name = "removejobs", description = "Removes all given jobs and their child jobs", returnDescription = "No data is returned, just the HTTP status code", restParameters = { @RestParameter(name = "jobIds", isRequired = true, description = "The IDs of the jobs to delete", type = Type.TEXT), }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "Jobs successfully removed"),
          @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Error while removing jobs") })
  public Response removeParentlessJobs(@FormParam("jobIds") String jobIds) throws NotFoundException {
    try {
      final JSONArray array = (JSONArray) JSONValue.parse(jobIds);
      final List<Long> jobIdList = Arrays.asList((Long[]) array.toArray(new Long[0]));
      serviceRegistry.removeJobs(jobIdList);
      return Response.noContent().build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }


  @POST
  @Path("removeparentlessjobs")
  @RestQuery(name = "removeparentlessjobs", description = "Removes all jobs without a parent job which have passed their lifetime", returnDescription = "No data is returned, just the HTTP status code", restParameters = { @RestParameter(name = "lifetime", isRequired = true, type = Type.INTEGER, description = "Lifetime of parentless jobs") }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "Parentless jobs successfully removed"),
          @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Error while removing parentless jobs") })
  public Response removeParentlessJobs(@FormParam("lifetime") int lifetime) {
    try {
      serviceRegistry.removeParentlessJobs(lifetime);
      return Response.noContent().build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

}
