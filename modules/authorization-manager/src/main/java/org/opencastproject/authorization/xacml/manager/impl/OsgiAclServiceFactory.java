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

import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/** OSGi implementation of {@link org.opencastproject.authorization.xacml.manager.api.AclServiceFactory}. */
@Component(
        property = {
                "service.description=Factory to create ACL services"
        },
        immediate = true,
        service = { AclServiceFactory.class }
)
public class OsgiAclServiceFactory implements AclServiceFactory {
  private AclDb aclDb;
  private SecurityService securityService;
  protected ElasticsearchIndex index;

  @Override
  public AclService serviceFor(Organization org) {
    return new AclServiceImpl(org, aclDb, index, securityService);
  }

  @Reference
  public void setAclDb(AclDb aclDb) {
    this.aclDb = aclDb;
  }

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Reference
  public void setIndex(ElasticsearchIndex index) {
    this.index = index;
  }
}
