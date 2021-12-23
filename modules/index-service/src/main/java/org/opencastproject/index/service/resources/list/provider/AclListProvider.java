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

package org.opencastproject.index.service.resources.list.provider;

import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.list.util.ListProviderUtil;
import org.opencastproject.security.api.SecurityService;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AclListProvider implements ResourceListProvider {

  private static final String PROVIDER_PREFIX = "ACL";

  public static final String NAME = PROVIDER_PREFIX + ".NAME";
  public static final String ID = PROVIDER_PREFIX + ".ID";

  private static final String[] NAMES = { PROVIDER_PREFIX, NAME, ID };
  private static final Logger logger = LoggerFactory.getLogger(AclListProvider.class);

  private AclServiceFactory aclServiceFactory;
  private SecurityService securityService;

  protected void activate(BundleContext bundleContext) {
    logger.info("ACL list provider activated!");
  }

  /** OSGi callback for acl services. */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  /**
   * OSGI callback to get the security service
   *
   * @param securityService the security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query) {
    Map<String, String> aclsList = new HashMap<String, String>();

    List<ManagedAcl> acls = aclServiceFactory.serviceFor(securityService.getOrganization()).getAcls();
    for (ManagedAcl a : acls) {
      if (ID.equals(listName)) {
        aclsList.put(a.getId().toString(), a.getId().toString());
      } else if (NAME.equals(listName)) {
        aclsList.put(a.getName(), a.getName());
      } else {
        aclsList.put(a.getId().toString(), a.getName());
      }
    }

    return ListProviderUtil.filterMap(aclsList, query);
  }

  @Override
  public boolean isTranslatable(String listName) {
    return false;
  }

  @Override
  public String getDefault() {
    return null;
  }
}
