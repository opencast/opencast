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

package org.opencastproject.graphql.datafetcher.acl;

import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.graphql.acl.GqlManagedAclCatalogue;
import org.opencastproject.graphql.datafetcher.ParameterDataFetcher;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.security.api.SecurityService;

import java.util.List;

import graphql.schema.DataFetchingEnvironment;

public class ManagedAclOffsetDataFetcher extends ParameterDataFetcher<GqlManagedAclCatalogue> {

  @Override
  public GqlManagedAclCatalogue get(OpencastContext opencastContext, DataFetchingEnvironment dataFetchingEnvironment) {
    SecurityService securityService = opencastContext.getService(SecurityService.class);
    AclServiceFactory aclServiceFactory = opencastContext.getService(AclServiceFactory.class);

    AclService aclService = aclServiceFactory.serviceFor(securityService.getOrganization());

    List<ManagedAcl> acls = aclService.getAcls();

    return new GqlManagedAclCatalogue(acls);
  }

}
