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

package org.opencastproject.authorization.xacml.manager.endpoint;

import static org.opencastproject.util.RestUtil.getEndpointUrl;

import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;

/** REST endpoint for ACL manager. */
@Path("/")
@RestService(name = "aclmanager", title = "ACL Manager", abstractText = "This service creates, edits and retrieves and helps managing and scheduling ACL's.", notes = {})
public final class OsgiAclServiceRestEndpoint extends AbstractAclServiceRestEndpoint {
  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(OsgiAclServiceRestEndpoint.class);

  private AclServiceFactory aclServiceFactory;
  private SecurityService securityService;
  private AuthorizationService authorizationService;
  private String endpointBaseUrl;
  private Archive<?> archive;
  private SeriesService seriesService;
  private HttpMediaPackageElementProvider httpMediaPackageElementProvider;

  /** OSGi callback. */
  public void activate(ComponentContext cc) {
    logger.info("Start ACL manager rest service");
    final Tuple<String, String> endpointUrl = getEndpointUrl(cc);
    endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
  }

  /** OSGi callback. */
  public void deactivate() {
    logger.info("Stop ACL manager rest service");
  }

  /** OSGi callback for setting persistence. */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  /** OSGi callback for setting security service. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi DI callback. */
  public void setHttpMediaPackageElementProvider(HttpMediaPackageElementProvider httpMediaPackageElementProvider) {
    this.httpMediaPackageElementProvider = httpMediaPackageElementProvider;
  }

  /** OSGi DI callback. */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /** OSGi DI callback. */
  public void setArchive(Archive<?> archive) {
    this.archive = archive;
  }

  /** OSGi DI callback. */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  @Override
  protected AclServiceFactory getAclServiceFactory() {
    return aclServiceFactory;
  }

  @Override
  protected String getEndpointBaseUrl() {
    return endpointBaseUrl;
  }

  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  @Override
  protected AuthorizationService getAuthorizationService() {
    return authorizationService;
  }

  @Override
  protected Archive<?> getArchive() {
    return archive;
  }

  @Override
  protected SeriesService getSeriesService() {
    return seriesService;
  }

  @Override
  protected HttpMediaPackageElementProvider getHttpMediaPackageElementProvider() {
    return httpMediaPackageElementProvider;
  }
}
