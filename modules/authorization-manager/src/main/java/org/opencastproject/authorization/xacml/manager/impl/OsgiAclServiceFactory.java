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

package org.opencastproject.authorization.xacml.manager.impl;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.elasticsearch.index.AbstractSearchIndex;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesService;

/** OSGi implementation of {@link org.opencastproject.authorization.xacml.manager.api.AclServiceFactory}. */
public class OsgiAclServiceFactory implements AclServiceFactory {
  private AclDb aclDb;
  private SeriesService seriesService;
  private AssetManager assetManager;
  private AuthorizationService authorizationService;
  private SecurityService securityService;
  protected AbstractSearchIndex adminUiIndex;
  protected AbstractSearchIndex externalApiIndex;

  @Override
  public AclService serviceFor(Organization org) {
    return new AclServiceImpl(org, aclDb, seriesService, assetManager,
            authorizationService, adminUiIndex, externalApiIndex, securityService);
  }

  /** OSGi DI callback. */
  public void setAclDb(AclDb aclDb) {
    this.aclDb = aclDb;
  }

  /** OSGi DI callback. */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /** OSGi DI callback. */
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /** OSGi DI callback. */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /** OSGi DI callback. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi DI callback. */
  public void setAdminUiIndex(AbstractSearchIndex index) {
    this.adminUiIndex = index;
  }

  /** OSGi DI callback. */
  public void setExternalApiIndex(AbstractSearchIndex index) {
    this.externalApiIndex = index;
  }
}
