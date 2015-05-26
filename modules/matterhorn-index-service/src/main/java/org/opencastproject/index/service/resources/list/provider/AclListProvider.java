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
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.util.ListProviderUtil;
import org.opencastproject.security.api.Organization;

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

  private static final String[] NAMES = { "ACL", NAME, ID };
  private static final Logger logger = LoggerFactory.getLogger(AclListProvider.class);

  private AclServiceFactory aclServiceFactory;

  protected void activate(BundleContext bundleContext) {
    logger.info("ACL list provider activated!");
  }

  /** OSGi callback for acl services. */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization) {
    Map<String, Object> aclsList = new HashMap<String, Object>();

    List<ManagedAcl> acls = aclServiceFactory.serviceFor(organization).getAcls();
    for (ManagedAcl a : acls) {
      if (ID.equals(listName)) {
        aclsList.put(a.getId().toString(), a.getId().toString());
      } else {
        aclsList.put(a.getId().toString(), a.getName());
      }
    }

    return ListProviderUtil.filterMap(aclsList, query);
  }
}
