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
package org.opencastproject.security.api;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;

/**
 * Provides generation and interpretation of policy documents in media packages
 */
public interface AuthorizationService {

  /**
   * Determines whether the current mediapackage contains a security policy.
   * 
   * @param mediapackage
   *          the mediapackage
   * @return whether the current mediapackage contains a security policy
   * @throws MediaPackageException
   *           if the mediapackage can not be read
   */
  boolean hasPolicy(MediaPackage mediapackage) throws MediaPackageException;

  /**
   * Determines whether the current user can take the specified action on the mediapackage.
   * 
   * @param mediapackage
   *          the mediapackage
   * @param action
   *          the action (e.g. read, modify, delete)
   * @return whether the current user has the correct privileges to take this action
   * @throws MediaPackageException
   *           if the policy can not be read from the mediapackage
   */
  boolean hasPermission(MediaPackage mediapackage, String action) throws MediaPackageException;

  /**
   * Gets the permissions associated with this mediapackage, as specified by its XACML attachment. The following rules
   * are used to determine the access control:
   * <ol>
   * <li>If exactly zero {@link org.opencastproject.mediapackage.MediaPackageElements#XACML_POLICY} attachments are
   * present, the returned ACL will be empty.</li>
   * <li>If exactly one {@link org.opencastproject.mediapackage.MediaPackageElements#XACML_POLICY} is attached to the
   * mediapackage, this is the source of authority</li>
   * <li>If more than one {@link org.opencastproject.mediapackage.MediaPackageElements#XACML_POLICY} attachments are
   * present, and one of them has no reference (
   * {@link org.opencastproject.mediapackage.MediaPackageElement#getReference()} returns null), that attachment is
   * presumed to be the source of authority</li>
   * <li>If more than one {@link org.opencastproject.mediapackage.MediaPackageElements#XACML_POLICY} attachments are
   * present, and more than one of them has no reference (
   * {@link org.opencastproject.mediapackage.MediaPackageElement#getReference()} returns null), the returned ACL will be
   * empty.</li>
   * <li>If more than one {@link org.opencastproject.mediapackage.MediaPackageElements#XACML_POLICY} attachments are
   * present, and all of them have references (
   * {@link org.opencastproject.mediapackage.MediaPackageElement#getReference()} returns a non-null reference), the
   * returned ACL will be empty.</li>
   * </ol>
   * 
   * @param mediapackage
   *          the mediapackage
   * @return the set of permissions and explicit denials
   * @throws MediaPackageException
   *           if the policy can not be read from the mediapackage
   */
  AccessControlList getAccessControlList(MediaPackage mediapackage) throws MediaPackageException;

  /**
   * Attaches the provided policies to a mediapackage as a XACML attachment, replacing any previous policy element.
   * 
   * @param mediapackage
   *          the mediapackage
   * @param accessControlList
   *          the tuples of roles to actions
   * @return the mediapackage with attached XACML policy
   * @throws MediaPackageException
   *           if the policy can not be attached to the mediapackage
   */
  MediaPackage setAccessControl(MediaPackage mediapackage, AccessControlList accessControlList)
          throws MediaPackageException;

}
