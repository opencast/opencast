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

package org.opencastproject.security.api;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.util.data.Tuple;


/**
 * Provides generation and interpretation of policy documents in media packages
 */
public interface AuthorizationService {

  /**
   * {@code user} defaults to the current user returned by the security service.
   * {@code org} defaults to the current organization returned by the security service.
   *
   * @see AuthorizationService#hasPermission(MediaPackage, User, Organization, String)
   */
  boolean hasPermission(MediaPackage mp, String action);

  /**
   * {@code user} defaults to the current user returned by the security service.
   * {@code org} defaults to the current organization returned by the security service.
   *
   * @see AuthorizationService#hasPermission(AccessControlList, User, Organization, String)
   */
  boolean hasPermission(AccessControlList acl, String action);

  /**
   * {@code entityBelongsToOrg} defaults to true.
   *
   * @see AuthorizationService#hasPermission(MediaPackage, User, Organization, String)
   */
  boolean hasPermission(MediaPackage mp, User user, Organization org, String action);

  /**
   * {@code entityBelongsToOrg} defaults to true.
   *
   * @see AuthorizationService#hasPermission(AccessControlList, User, Organization, String)
   */
  boolean hasPermission(AccessControlList acl, User user, Organization org, String action);

  /**
   * {@code entityBelongsToOrg} defaults to true.
   * If you pass a mediapackage, the authorization service has to get the active ACL of the mediapackage. If you
   * already have that ACL, calling this method might be slightly more performant.
   *
   * @see AuthorizationService#hasPermission(AccessControlList, User, Organization, String, String)
   */
  boolean hasPermissionPerformance(AccessControlList acl, User user, Organization org, String action,
      String mediaPackageId);

  /**
   * Determines whether the user can take the specified action on the media package.
   *
   * @param mp
   *          the media package
   * @param user
   *          the current user
   * @param org
   *          the current organization
   * @param action
   *          the action (e.g. read, modify, delete)
   * @param entityOrgId
   *          organization id of the mediapackage. If not the same organization as "org", permission will be denied
   * @return whether the current user has the correct privileges to take this action
   */
  boolean hasPermission(MediaPackage mp, User user, Organization org, String action, String entityOrgId);

  /**
   * Determines whether the current user can take the specified action given the access control list.
   * This is not restricted to access control lists in media packages, but works regardless of which entity the
   * access control list belongs to.
   *
   * @param acl
   *          the access control list
   * @param user
   *          the current user
   * @param org
   *          the current organization
   * @param action
   *          the action (e.g. read, modify, delete)
   * @param entityOrgId
   *          organization id of the entity. If not the same organization as "org", permission will be denied
   * @return whether the current user has the correct privileges to take this action
   */
  boolean hasPermission(AccessControlList acl, User user, Organization org, String action, String entityOrgId);

  /**
   * Gets the active access control list associated with the given media package, as specified by its XACML
   * attachments. XACML attachments are evaluated in the following order:
   *
   * <ol>
   *   <li>Use episode XACML attachment if present</li>
   *   <li>Use series XACML attachment if present</li>
   *   <li>Use non-specific XACML attachment if present. Note that the usage of this is deprecated!</li>
   *   <li>Use the global default ACL</li>
   * </ol>
   *
   * Note that this is identical to calling {@link #getAcl(MediaPackage, AclScope)} with scope set to
   * {@link AclScope#Series}.
   *
   * @param mp
   *          the media package
   * @return the active access control list as well as the scope identifying the source of the access rules (episode,
   *          series, …).
   */
  Tuple<AccessControlList, AclScope> getActiveAcl(MediaPackage mp);

  /**
   * Gets the access control list for a given scope associated with the given media package, as specified by its XACML
   * attachments. XACML attachments are evaluated in the following order:
   *
   * <ol>
   *   <li>Use episode XACML attachment if present. This applies only if scope is set to {@link AclScope#Episode}</li>
   *   <li>Use series XACML attachment if present. This applies only if scope is set to {@link AclScope#Episode} or
   *      {@link AclScope#Series}</li>
   *   <li>Use non-specific XACML attachment if present. Note that the usage of this is deprecated!</li>
   *   <li>Use the global default ACL</li>
   * </ol>
   *
   * @param mp
   *          the media package
   * @param scope
   *          the acl scope
   * @return the access control list as well as the scope identifying the source of the access rules (episode,
   *          series, …) for the given media package and scope.
   */
  Tuple<AccessControlList, AclScope> getAcl(MediaPackage mp, AclScope scope);

  /**
   * Attaches the provided policies to a media package as a XACML attachment, replacing any previous policy element of
   * the same scope.
   *
   * @param mp
   *          the media package
   * @param scope
   *          scope of the ACL
   * @param acl
   *          the tuples of roles to actions
   * @return the mutated (!) media package with attached XACML policy and the XACML attachment
   */
  Tuple<MediaPackage, Attachment> setAcl(MediaPackage mp, AclScope scope, AccessControlList acl)
          throws MediaPackageException;

  /**
   * Remove the XACML of the given scope.
   *
   * @param mp
   *          the media package
   * @param scope
   *          scope of the ACL
   * @return the mutated (!) media package with removed XACML policy
   */
  MediaPackage removeAcl(MediaPackage mp, AclScope scope);
}
