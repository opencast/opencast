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
import org.opencastproject.systems.MatterhornConstans;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;

import java.net.MalformedURLException;
import java.net.URL;

/** OSGi bound implementation. */
public final class ServiceRegistryRemoteImpl extends ServiceRegistryRemoteBase {
  /** The http client to use when connecting to remote servers */
  private TrustedHttpClient client;

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
