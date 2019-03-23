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

package org.opencastproject.authorization.xacml.manager.api;

import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import java.util.List;

/**
 * ACL service API for managing ACLs.
 */
public interface AclService {

  /**
   * Immediate ACL application to an episode.
   *
   * @param episodeId
   *          the episode id
   * @param managedAcl
   *          the ACL to apply, <code>none</code> to delete the episode ACL from the media package to cause a fallback
   *          to the series ACL
   * @return true if the episode exists
   * @throws AclServiceException
   *           in case of any error
   */
  boolean applyAclToEpisode(String episodeId, Option<ManagedAcl> managedAcl) throws AclServiceException;

  /**
   * Immediate ACL application to an episode.
   *
   * @param episodeId
   *          the episode id
   * @param acl
   *          the ACL to apply, <code>null</code> to delete the episode ACL from the media package to cause a fallback
   *          to the series ACL
   * @return true if the episode exists
   * @throws AclServiceException
   *           in case of any error
   */
  boolean applyAclToEpisode(String episodeId, AccessControlList acl) throws AclServiceException;

  /**
   * Immediate ACL application to a series.
   *
   * @param seriesId
   *          the series id
   * @param managedAcl
   *          the ACL to apply
   * @param override
   *          if true it will force the use the series ACL for all the episodes in the series. Otherwise, only episodes
   *          that don't have a custom access control list defined will be adjusted to the new series ACL.
   * @return false if the series doesn't exists
   * @throws AclServiceException
   *           in case of any error
   */
  boolean applyAclToSeries(String seriesId, ManagedAcl managedAcl, boolean override) throws AclServiceException;

  /**
   * Immediate ACL application to a series.
   *
   * @param seriesId
   *          the series id
   * @param acl
   *          the ACL to apply
   * @param override
   *          if true it will force the use the series ACL for all the episodes in the series. Otherwise, only episodes
   *          that don't have a custom access control list defined will be adjusted to the new series ACL.
   * @return false if the series doesn't exists
   * @throws AclServiceException
   *           in case of any error
   */
  boolean applyAclToSeries(String seriesId, AccessControlList acl, boolean override) throws AclServiceException;

  /**
   * Return all ACLs of this organization.
   */
  List<ManagedAcl> getAcls();

  /**
   * Return an ACL of an organization by its ID.
   *
   * @return <code>some</code> if the ACL could be found, <code>none</code> if the ACL with the given ID does not exist.
   */
  Option<ManagedAcl> getAcl(long id);

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
  Option<ManagedAcl> createAcl(AccessControlList acl, String name);

  /**
   * Delete an ACL by its ID.
   *
   * @return <code>true</code> if the ACL existed and could be deleted successfully, <code>false</code> if the ACL can't
   *         be deleted because it still has active references on it.
   * @throws NotFoundException
   *           if the managed acl could not be found
   * @throws AclServiceException
   *           if exception occurred
   */
  boolean deleteAcl(long id) throws AclServiceException, NotFoundException;
}
