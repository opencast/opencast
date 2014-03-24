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
package org.opencastproject.authorization.xacml.manager.impl;

import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.data.Option;

import java.util.List;

/** The ACL DB stores ACLs on a per organization basis. */
public interface AclDb {
  /** Return all ACLs of this organization. */
  List<ManagedAcl> getAcls(Organization org);

  /**
   * Return an ACL of an organization by its ID.
   *
   * @return <code>some</code> if the ACL could be found, <code>none</code> if the ACL with the given ID does not exist.
   */
  Option<ManagedAcl> getAcl(Organization org, long id);

  /**
   * Update an existing ACL.
   *
   * @return true on a successful update, false if no ACL exists with the given ID.
   */
  boolean updateAcl(ManagedAcl acl);

  /**
   * Create a new ACL.
   *
   * @return <code>some</code> if the new ACL could be created successfully, <code>none</code> if an ACL with the same
   *         name already exists
   */
  Option<ManagedAcl> createAcl(Organization org, AccessControlList acl, String name);

  /**
   * Delete an ACL by its ID.
   *
   * @return true if the ACL existed and could be deleted successfully, false if there is no such ACL.
   */
  boolean deleteAcl(Organization org, long id);
}
