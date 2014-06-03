/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.serviceregistry.remote;

import static org.apache.commons.lang.StringUtils.isBlank;

import org.opencastproject.job.api.JaxbJobList;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.HostRegistrationParser;
import org.opencastproject.serviceregistry.api.JaxbHostRegistrationList;
import org.opencastproject.serviceregistry.api.JaxbServiceRegistrationList;
import org.opencastproject.serviceregistry.api.JaxbServiceStatistics;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistrationParser;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.serviceregistry.api.SystemLoad;
import org.opencastproject.systems.MatterhornConstans;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.QueryStringBuilder;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This implementation of the remote service registry is able to provide the functionality specified by the api over
 * <code>HTTP</code> rather than by directly connecting to the database that is backing the service.
 * <p>
 * This means that it is suited to run inside protected environments as long as there is an implementation of the
 * service running somewhere that provides the matching communication endpoint, which is the case with the default
 * implementation at {@link org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl}.
 * <p>
 * Other than with the other <code>-remote</code> implementations, this one needs to be configured to find it's
 * counterpart implementation. It may either point to a load balancer hiding a number of running instances or to one
 * specific instance.
 */
public class ServiceRegistryRemoteImpl implements ServiceRegistry {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryRemoteImpl.class);

  /** Current job used to process job in the service registry */
  private static final ThreadLocal<Job> currentJob = new ThreadLocal<Job>();

  /** Configuration key for the service registry */
  public static final String OPT_SERVICE_REGISTRY_URL = "org.opencastproject.serviceregistry.url";

  /** The http client to use when connecting to remote servers */
  protected TrustedHttpClient client = null;

  /** Url of the actual service implementation */
  protected String serviceURL = null;

  /** The base URL of this server */
  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  /**
   * Callback for the OSGi environment that is called upon service activation.
   *
   * @param context
   *          the component context
   */
  protected void activate(ComponentContext cc) {
    if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty(MatterhornConstans.SERVER_URL_PROPERTY))) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      serverUrl = cc.getBundleContext().getProperty(MatterhornConstans.SERVER_URL_PROPERTY);
    }

    if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty(OPT_SERVICE_REGISTRY_URL))) {
      try {
        serviceURL = new URL(serverUrl + "/services").toExternalForm();
      } catch (MalformedURLException e) {
        throw new ServiceException(OPT_SERVICE_REGISTRY_URL + " is missing, and fallback localhost url is malformed: "
                + serverUrl + "/services");
      }
    } else {
      try {
        serviceURL = new URL(cc.getBundleContext().getProperty(OPT_SERVICE_REGISTRY_URL)).toExternalForm();
      } catch (MalformedURLException e) {
        throw new ServiceException(OPT_SERVICE_REGISTRY_URL + " is malformed: "
                + StringUtils.trimToNull(cc.getBundleContext().getProperty(OPT_SERVICE_REGISTRY_URL)));
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String host, String path)
          throws ServiceRegistryException {
    return registerService(serviceType, host, path, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#enableHost(String)
   */
  @Override
  public void enableHost(String host) throws ServiceRegistryException, NotFoundException {
    HttpPost post = new HttpPost(UrlSupport.concat(serviceURL, "enablehost"));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("host", host));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new ServiceRegistryException("Can not url encode post parameters", e);
    }
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(post);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
        logger.info("Enabled host '" + host + "'.");
        return;
      } else if (responseStatusCode == HttpStatus.SC_NOT_FOUND) {
        throw new NotFoundException("Host not found: " + host);
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceRegistryException("Unable to enable '" + host + "'", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to enable '" + host + "'. HTTP status=" + responseStatusCode);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#disableHost(String)
   */
  @Override
  public void disableHost(String host) throws ServiceRegistryException, NotFoundException {
    HttpPost post = new HttpPost(UrlSupport.concat(serviceURL, "disablehost"));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("host", host));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new ServiceRegistryException("Can not url encode post parameters", e);
    }
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(post);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
        logger.info("Disabled host '" + host + "'.");
        return;
      } else if (responseStatusCode == HttpStatus.SC_NOT_FOUND) {
        throw new NotFoundException("Host not found: " + host);
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceRegistryException("Unable to disable '" + host + "'", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to disable '" + host + "'. HTTP status=" + responseStatusCode);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerHost(java.lang.String, int)
   */
  @Override
  public void registerHost(String host, int maxConcurrentJobs) throws ServiceRegistryException {
    HttpPost post = new HttpPost(UrlSupport.concat(serviceURL, "registerhost"));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("host", host));
      params.add(new BasicNameValuePair("maxJobs", Integer.toString(maxConcurrentJobs)));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new ServiceRegistryException("Can not url encode post parameters", e);
    }
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(post);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
        logger.info("Registered '" + host + "'.");
        return;
      }
    } catch (Exception e) {
      throw new ServiceRegistryException("Unable to register '" + host + "'", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to register '" + host + "'. HTTP status=" + responseStatusCode);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unregisterHost(java.lang.String)
   */
  @Override
  public void unregisterHost(String host) throws ServiceRegistryException {
    HttpPost post = new HttpPost(UrlSupport.concat(serviceURL, "unregisterhost"));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("host", host));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new ServiceRegistryException("Can not url encode post parameters", e);
    }
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(post);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
        logger.info("Unregistered '" + host + "'.");
        return;
      }
    } catch (Exception e) {
      throw new ServiceRegistryException("Unable to unregister '" + host + "'", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to unregister '" + host + "'. HTTP status=" + responseStatusCode);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String, boolean)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String host, String path, boolean jobProducer)
          throws ServiceRegistryException {
    HttpPost post = new HttpPost(UrlSupport.concat(serviceURL, "register"));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("serviceType", serviceType));
      params.add(new BasicNameValuePair("host", host));
      params.add(new BasicNameValuePair("path", path));
      params.add(new BasicNameValuePair("jobProducer", Boolean.toString(jobProducer)));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new ServiceRegistryException("Can not url encode post parameters", e);
    }
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(post);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        logger.info("Registered '" + serviceType + "' on host '" + host + "' with path '" + path + "'.");
        return ServiceRegistrationParser.parse(response.getEntity().getContent());
      }
    } catch (Exception e) {
      throw new ServiceRegistryException("Unable to register '" + serviceType + "' on host '" + host + "' with path '"
              + path + "'.", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to register '" + serviceType + "' on host '" + host + "' with path '"
            + path + "'. HTTP status=" + responseStatusCode);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unRegisterService(java.lang.String, java.lang.String)
   */
  @Override
  public void unRegisterService(String serviceType, String host) throws ServiceRegistryException {
    HttpPost post = new HttpPost(UrlSupport.concat(serviceURL, "unregister"));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("serviceType", serviceType));
      params.add(new BasicNameValuePair("host", host));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
    } catch (UnsupportedEncodingException e) {
      throw new ServiceRegistryException("Can not url encode post parameters", e);
    }
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(post);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
        logger.info("Unregistered '" + serviceType + "' on host '" + host + "'.");
        return;
      }
    } catch (Exception e) {
      throw new ServiceRegistryException("Unable to register " + serviceType + " from host " + host, e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to unregister '" + serviceType + "' on host '" + host
            + "'. HTTP status=" + responseStatusCode);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#setMaintenanceStatus(java.lang.String, boolean)
   */
  @Override
  public void setMaintenanceStatus(String host, boolean maintenance) throws NotFoundException, ServiceRegistryException {
    HttpPost post = new HttpPost(UrlSupport.concat(serviceURL, "maintenance"));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("host", host));
      params.add(new BasicNameValuePair("maintenance", Boolean.toString(maintenance)));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new ServiceRegistryException("Can not url encode post parameters", e);
    }
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(post);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
        logger.info("Set maintenance mode on '" + host + "' to '" + maintenance + "'.");
        return;
      } else if (responseStatusCode == HttpStatus.SC_NOT_FOUND) {
        throw new NotFoundException("Host not found: " + host);
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceRegistryException("Unable to set maintenance mode on " + host + " to '" + maintenance + "'", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to set maintenace mode on '" + host + "' to '" + maintenance
            + "'. HTTP status=" + responseStatusCode);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String)
   */
  @Override
  public Job createJob(String type, String operation) throws ServiceRegistryException {
    return createJob(type, operation, null, null, true);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments) throws ServiceRegistryException {
    return createJob(type, operation, arguments, null, true);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List, java.lang.String)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload)
          throws ServiceRegistryException {
    return createJob(type, operation, arguments, payload, true);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List, String, boolean)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean queueable)
          throws ServiceRegistryException {
    return createJob(type, operation, arguments, payload, queueable, getCurrentJob());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(String, String, List, String, boolean, Job)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean queueable,
          Job parentJob) throws ServiceRegistryException {
    HttpPost post = new HttpPost(UrlSupport.concat(serviceURL, "job"));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("jobType", type));
      params.add(new BasicNameValuePair("host", this.serverUrl));
      params.add(new BasicNameValuePair("operation", operation));
      if (arguments != null && !arguments.isEmpty()) {
        for (String argument : arguments) {
          params.add(new BasicNameValuePair("arg", argument));
        }
      }
      if (payload != null)
        params.add(new BasicNameValuePair("payload", payload));
      params.add(new BasicNameValuePair("start", Boolean.toString(queueable)));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new ServiceRegistryException("Can not url encode post parameters", e);
    }
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(post);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_CREATED) {
        Job job = JobParser.parseJob(response.getEntity().getContent());
        logger.debug("Created a new job '{}'", job);
        return job;
      }
    } catch (Exception e) {
      throw new ServiceRegistryException("Unable to create a job of type '" + type, e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to create a job of type '" + type + " (" + responseStatusCode + ")");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#updateJob(org.opencastproject.job.api.Job)
   */
  @Override
  public Job updateJob(Job job) throws ServiceRegistryException, NotFoundException {
    HttpPut put = new HttpPut(UrlSupport.concat(serviceURL, "job/" + job.getId() + ".xml"));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("job", JobParser.toXml(job)));
      put.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new ServiceRegistryException("Can not url encode post parameters", e);
    } catch (IOException e) {
      throw new ServiceRegistryException("Can not serialize job " + job, e);
    }
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(put);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
        logger.info("Updated job '{}'", job);
        return job;
      } else if (responseStatusCode == HttpStatus.SC_NOT_FOUND) {
        throw new NotFoundException("Job not found: " + job.getId());
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceRegistryException("Unable to update " + job, e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to update " + job + " (" + responseStatusCode + ")");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJob(long)
   */
  @Override
  public Job getJob(long id) throws NotFoundException, ServiceRegistryException {
    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, "job/" + id + ".xml"));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NOT_FOUND) {
        throw new NotFoundException("Unable to locate job " + id);
      }
      if (responseStatusCode == HttpStatus.SC_OK) {
        return JobParser.parseJob(response.getEntity().getContent());
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get job id=" + id, e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to retrieve job " + id + " (" + responseStatusCode + ")");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getChildJobs(long)
   */
  @Override
  public List<Job> getChildJobs(long id) throws NotFoundException, ServiceRegistryException {
    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, "job/" + id + "/children.xml"));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NOT_FOUND) {
        throw new NotFoundException("No children jobs found from parent job " + id);
      }
      if (responseStatusCode == HttpStatus.SC_OK) {
        JaxbJobList jaxbJobList = JobParser.parseJobList(response.getEntity().getContent());
        return new ArrayList<Job>(jaxbJobList.getJobs());
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get job id=" + id, e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to retrieve job " + id + " (" + responseStatusCode + ")");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJobs(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public List<Job> getJobs(String serviceType, Status status) throws ServiceRegistryException {
    QueryStringBuilder qsb = new QueryStringBuilder("jobs.xml").add("serviceType", serviceType);
    if (status != null)
      qsb.add("status", status.toString());

    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, qsb.toString()));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        JaxbJobList jaxbJobList = JobParser.parseJobList(response.getEntity().getContent());
        return new ArrayList<Job>(jaxbJobList.getJobs());
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get jobs", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to retrieve jobs via http:" + response.getStatusLine());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByLoad(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByLoad(String serviceType) throws ServiceRegistryException {
    String servicePath = new QueryStringBuilder("available.xml").add("serviceType", serviceType).toString();
    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, servicePath));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        JaxbServiceRegistrationList serviceList = ServiceRegistrationParser.parseRegistrations(response.getEntity()
                .getContent());
        return new ArrayList<ServiceRegistration>(serviceList.getRegistrations());
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get service registrations", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to get service registrations (" + responseStatusCode + ")");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByHost(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByHost(String host) throws ServiceRegistryException {
    String servicePath = new QueryStringBuilder("services.xml").add("host", host).toString();
    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, servicePath));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        JaxbServiceRegistrationList serviceList = ServiceRegistrationParser.parseRegistrations(response.getEntity()
                .getContent());
        return new ArrayList<ServiceRegistration>(serviceList.getRegistrations());
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get service registrations", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to get service registrations (" + responseStatusCode + ")");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByType(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByType(String serviceType) throws ServiceRegistryException {
    String servicePath = new QueryStringBuilder("services.xml").add("serviceType", serviceType).toString();
    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, servicePath));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        JaxbServiceRegistrationList serviceList = ServiceRegistrationParser.parseRegistrations(response.getEntity()
                .getContent());
        return new ArrayList<ServiceRegistration>(serviceList.getRegistrations());
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get service registrations", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to get service registrations (" + responseStatusCode + ")");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistration(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration getServiceRegistration(String serviceType, String host) throws ServiceRegistryException {
    if (isBlank(serviceType) || isBlank(host)) {
      throw new IllegalArgumentException("Service type and host must be provided to locate service registrations");
    }
    String servicePath = new QueryStringBuilder("services.xml").add("serviceType", serviceType).add("host", host)
            .toString();
    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, servicePath));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        return ServiceRegistrationParser.parse(response.getEntity().getContent());
      } else if (responseStatusCode == HttpStatus.SC_NOT_FOUND) {
        return null;
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get service registrations", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to get service registrations (" + responseStatusCode + ")");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrations()
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrations() throws ServiceRegistryException {
    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, "services.xml"));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        JaxbServiceRegistrationList serviceList = ServiceRegistrationParser.parseRegistrations(response.getEntity()
                .getContent());
        return new ArrayList<ServiceRegistration>(serviceList.getRegistrations());
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get service registrations", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to get service registrations (" + responseStatusCode + ")");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getHostRegistrations()
   */
  @Override
  public List<HostRegistration> getHostRegistrations() throws ServiceRegistryException {
    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, "hosts.xml"));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        JaxbHostRegistrationList serviceList = HostRegistrationParser.parseRegistrations(response.getEntity()
                .getContent());
        return new ArrayList<HostRegistration>(serviceList.getRegistrations());
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get host registrations", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to get host registrations (" + responseStatusCode + ")");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceStatistics()
   */
  @Override
  public List<ServiceStatistics> getServiceStatistics() throws ServiceRegistryException {
    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, "statistics.xml"));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        List<JaxbServiceStatistics> stats = ServiceRegistrationParser
                .parseStatistics(response.getEntity().getContent()).getStats();
        return new ArrayList<ServiceStatistics>(stats);
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get service statistics", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to get service statistics (" + responseStatusCode + ")");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long count(String serviceType, Status status) throws ServiceRegistryException {
    return count(serviceType, null, null, status);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#countByHost(java.lang.String, java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countByHost(String serviceType, String host, Status status) throws ServiceRegistryException {
    return count(serviceType, host, null, status);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#countByOperation(java.lang.String, java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countByOperation(String serviceType, String operation, Status status) throws ServiceRegistryException {
    return count(serviceType, null, operation, status);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String, java.lang.String,
   *      java.lang.String, org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long count(String serviceType, String host, String operation, Status status) throws ServiceRegistryException {
    if (isBlank(serviceType)) {
      throw new IllegalArgumentException("Service type must not be null");
    }
    QueryStringBuilder queryStringBuilder = new QueryStringBuilder("count").add("serviceType", serviceType);
    if (status != null)
      queryStringBuilder.add("status", status.toString());

    if (StringUtils.isNotBlank(host))
      queryStringBuilder.add("host", host);

    if (StringUtils.isNotBlank(operation))
      queryStringBuilder.add("operation", operation);

    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, queryStringBuilder.toString()));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        return Long.parseLong(EntityUtils.toString(response.getEntity()));
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get service statistics", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to get service statistics (" + responseStatusCode + ")");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getLoad()
   */
  @Override
  public SystemLoad getLoad() throws ServiceRegistryException {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the trusted http client.
   *
   * @param client
   *          the trusted http client
   */
  void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getMaxConcurrentJobs()
   */
  @Override
  public int getMaxConcurrentJobs() throws ServiceRegistryException {
    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, "maxconcurrentjobs"));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        return Integer.parseInt(EntityUtils.toString(response.getEntity()));
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get service statistics", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to get service statistics (" + responseStatusCode + ")");
  }

  @Override
  public void sanitize(String serviceType, String host) throws NotFoundException {
    HttpPost post = new HttpPost(UrlSupport.concat(serviceURL, "sanitize"));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("serviceType", serviceType));
      params.add(new BasicNameValuePair("host", host));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Can not url encode post parameters", e);
    }

    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(post);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
        return;
      } else if (responseStatusCode == HttpStatus.SC_NOT_FOUND) {
        throw new NotFoundException();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to get service statistics", e);
    } finally {
      client.close(response);
    }
    throw new IllegalStateException("Unable to get service statistics (" + responseStatusCode + ")");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getCurrentJob()
   */
  @Override
  public Job getCurrentJob() {
    return currentJob.get();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#setCurrentJob(Job)
   */
  @Override
  public void setCurrentJob(Job job) {
    currentJob.set(job);
  }

  @Override
  public void removeJob(long id) throws NotFoundException, ServiceRegistryException {
    HttpDelete delete = new HttpDelete(UrlSupport.concat(serverUrl, "job", String.valueOf(id)));

    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(delete);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
        return;
      } else if (responseStatusCode == HttpStatus.SC_NOT_FOUND) {
        throw new NotFoundException();
      }
    } catch (TrustedHttpClientException e) {
      throw new ServiceRegistryException(e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to remove job with ID " + id + " (" + responseStatusCode + ")");
  }

  @Override
  public void removeParentlessJobs(int lifetime) throws ServiceRegistryException {
    String postUrl = UrlSupport.concat(serverUrl, "removeparentlessjobs");
    HttpPost post = new HttpPost(postUrl);

    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("lifetime", String.valueOf(lifetime)));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }

    HttpResponse response = null;
    try {
      response = client.execute(post);
      int responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
        logger.info("Parentless jobs successfully removed");
        return;
      }
    } catch (TrustedHttpClientException e) {
      throw new ServiceRegistryException(e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to remove parentless jobs");
  }
}
