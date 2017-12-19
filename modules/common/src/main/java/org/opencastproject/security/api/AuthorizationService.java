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

package org.opencastproject.security.api;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;

import java.util.List;

/**
 * Provides generation and interpretation of policy documents in media packages
 */
public interface AuthorizationService {

  /**
   * Determines whether the current media package contains a security policy.
   *
   * @param mp
   *          the media package
   * @return whether the current media package contains a security policy
   */
  boolean hasPolicy(MediaPackage mp);

  /**
   * Determines whether the current user can take the specified action on the media package.
   *
   * @param mp
   *          the media package
   * @param action
   *          the action (e.g. read, modify, delete)
   * @return whether the current user has the correct privileges to take this action
   */
  boolean hasPermission(MediaPackage mp, String action);

  /**
   * Gets the active permissions associated with this media package, as specified by its XACML attachment. The following
   * rules are used to determine the access control in descending order:
   * <ol>
   * <li>If exactly zero {@link org.opencastproject.mediapackage.MediaPackageElements#XACML_POLICY_EPISODE} and
   * {@link org.opencastproject.mediapackage.MediaPackageElements#XACML_POLICY_SERIES} attachments are present, the
   * returned ACL will be empty.</li>
   * <li>If exactly one {@link org.opencastproject.mediapackage.MediaPackageElements#XACML_POLICY_EPISODE} is attached
   * to the media package, this is the source of authority</li>
   * <li>If exactly one {@link org.opencastproject.mediapackage.MediaPackageElements#XACML_POLICY_SERIES} is attached to
   * the media package, this is the source of authority</li>
   * <li>If more than one XACML attachments are present, and one of them has no reference (
   * {@link org.opencastproject.mediapackage.MediaPackageElement#getReference()} returns null), that attachment is
   * presumed to be the source of authority. Episode XACMLs are considered before series XACMLs.</li>
   * <li>If more than one XACML attachments are present, and more than one of them has no reference (
   * {@link org.opencastproject.mediapackage.MediaPackageElement#getReference()} returns null), the returned ACL will be
   * empty. Episode XACMLs are considered before series XACMLs.</li>
   * <li>If more than one XACML attachments are present, and all of them have references (
   * {@link org.opencastproject.mediapackage.MediaPackageElement#getReference()} returns a non-null reference), the
   * returned ACL will be empty. Episode XACMLs are considered before series XACMLs.</li>
   * </ol>
   *
   * @param mp
   *          the media package
   * @return the set of permissions and explicit denials
   */
  Tuple<AccessControlList, AclScope> getActiveAcl(MediaPackage mp);

  /**
   * Gets the permissions by its scope associated with this media package, as specified by its XACML attachment.
   *
   * @param mp
   *          the media package
   * @param scope
   *          the acl scope
   * @return the set of permissions and explicit denials
   */
  Option<AccessControlList> getAcl(MediaPackage mp, AclScope scope);

  /**
   * Return access control attachments of a certain scope or all.
   *
   * @param mp
   *          the media package
   * @param scope
   *          the scope or none to get all ACL attachments
   * @return a list of attachments that fit the given criteria
   */
  List<Attachment> getAclAttachments(MediaPackage mp, Option<AclScope> scope);

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
  Tuple<MediaPackage, Attachment> setAcl(MediaPackage mp, AclScope scope, AccessControlList acl);

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
