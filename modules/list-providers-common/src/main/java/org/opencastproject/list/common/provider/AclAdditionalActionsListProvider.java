/*
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

package org.opencastproject.list.common.provider;

import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.list.util.ListProviderUtil;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(
    property = {
        "opencast.service.type=org.opencastproject.list.provider.AclAdditionalActionsListProvider"
    }
)
@ServiceDescription("ACL additional actions list provider")
public class AclAdditionalActionsListProvider implements ResourceListProvider {

  private static final String PROVIDER_PREFIX = "ACL.ACTIONS";
  private static final String[] NAMES = { PROVIDER_PREFIX };
  private static final Logger logger = LoggerFactory.getLogger(AclAdditionalActionsListProvider.class);

  private SecurityService securityService;

  @Activate
  protected void activate() {
    logger.info("ACL.ACTIONS list provider activated!");
  }

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query) {
    try {
      Organization org = securityService.getOrganization();
      if (org == null) {
        return new HashMap<>();
      }
      Map<String, String> actions = SecurityUtil.additionalAclActions(org);
      return ListProviderUtil.filterMap(actions, query);
    } catch (Exception e) {
      logger.warn("Unable to read additional ACL actions from organization configuration", e);
      return new HashMap<>();
    }
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
