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

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.systems.MatterhornConstans;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;

import java.net.MalformedURLException;
import java.net.URL;

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

  /** The incident service */
  private IncidentService incidentService;

  /** Url of the actual service implementation */
  private String serviceUrl;

  /** The base URL of this server */
  private String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  @Override
  public TrustedHttpClient getHttpClient() {
    return client;
  }

  @Override
  public IncidentService getIncidentService() {
    return incidentService;
  }

  @Override
  public String getServiceUrl() {
    return serviceUrl;
  }

  @Override
  public String getServerUrl() {
    return serverUrl;
  }

  /**
   * Callback for the OSGi environment that is called upon service activation.
   *
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty(MatterhornConstans.SERVER_URL_PROPERTY))) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      serverUrl = cc.getBundleContext().getProperty(MatterhornConstans.SERVER_URL_PROPERTY);
    }

    if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty(OPT_SERVICE_REGISTRY_URL))) {
      try {
        serviceUrl = new URL(serverUrl + "/services").toExternalForm();
      } catch (MalformedURLException e) {
        throw new ServiceException(OPT_SERVICE_REGISTRY_URL + " is missing, and fallback localhost url is malformed: "
                + serverUrl + "/services");
      }
    } else {
      try {
        serviceUrl = new URL(cc.getBundleContext().getProperty(OPT_SERVICE_REGISTRY_URL)).toExternalForm();
      } catch (MalformedURLException e) {
        throw new ServiceException(OPT_SERVICE_REGISTRY_URL + " is malformed: "
                + StringUtils.trimToNull(cc.getBundleContext().getProperty(OPT_SERVICE_REGISTRY_URL)));
      }
    }
  }

  /** OSGi DI. */
  public void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }

  /** OSGi DI. */
  public void setIncidentService(IncidentService incidentService) {
    this.incidentService = incidentService;
  }

}
