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

import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.security.api.AccessControlList;

public final class ManagedAclImpl implements ManagedAcl {

  private Long id;

  private String organizationId;

  private String name;

  private AccessControlList acl;

  /**
   * Create a managed acl
   *
   * @param id
   *          an id
   * @param name
   *          display name
   * @param acl
   *          ACL
   */
  public ManagedAclImpl(Long id, String name, String organizationId, AccessControlList acl) {
    this.id = id;
    this.name = name;
    this.organizationId = organizationId;
    this.acl = acl;
  }

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public AccessControlList getAcl() {
    return acl;
  }

  @Override
  public String getOrganizationId() {
    return organizationId;
  }

  public static ManagedAclImpl fromManagedAcl(ManagedAcl acl) {
    if (acl instanceof ManagedAclImpl)
      return (ManagedAclImpl) acl;
    return new ManagedAclImpl(acl.getId(), acl.getName(), acl.getOrganizationId(), acl.getAcl());
  }
}
