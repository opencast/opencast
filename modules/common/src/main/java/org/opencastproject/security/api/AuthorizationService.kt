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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.security.api

import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.util.data.Tuple

import java.io.IOException
import java.io.InputStream


/**
 * Provides generation and interpretation of policy documents in media packages
 */
interface AuthorizationService {

    /**
     * Determines whether the current user can take the specified action on the media package.
     *
     * @param mp
     * the media package
     * @param action
     * the action (e.g. read, modify, delete)
     * @return whether the current user has the correct privileges to take this action
     */
    fun hasPermission(mp: MediaPackage, action: String): Boolean

    /**
     * Gets the active access control list associated with the given media package, as specified by its XACML
     * attachments. XACML attachments are evaluated in the following order:
     *
     *
     *  1. Use episode XACML attachment if present
     *  1. Use series XACML attachment if present
     *  1. Use non-specific XACML attachment if present. Note that the usage of this is deprecated!
     *  1. Use the global default ACL
     *
     *
     * Note that this is identical to calling [.getAcl] with scope set to
     * [AclScope.Series].
     *
     * @param mp
     * the media package
     * @return the active access control list as well as the scope identifying the source of the access rules (episode,
     * series, …).
     */
    fun getActiveAcl(mp: MediaPackage): Tuple<AccessControlList, AclScope>

    /**
     * Gets the active permissions as specified by the given XACML.
     *
     * @param in
     * the XACML attachment used to determine a set of permissions and explicit denials
     * @return a set of permissions and explicit denials
     */
    @Throws(IOException::class)
    fun getAclFromInputStream(`in`: InputStream): AccessControlList

    /**
     * Gets the access control list for a given scope associated with the given media package, as specified by its XACML
     * attachments. XACML attachments are evaluated in the following order:
     *
     *
     *  1. Use episode XACML attachment if present. This applies only if scope is set to [AclScope.Episode]
     *  1. Use series XACML attachment if present. This applies only if scope is set to [AclScope.Episode] or
     * [AclScope.Series]
     *  1. Use non-specific XACML attachment if present. Note that the usage of this is deprecated!
     *  1. Use the global default ACL
     *
     *
     * @param mp
     * the media package
     * @param scope
     * the acl scope
     * @return the access control list as well as the scope identifying the source of the access rules (episode,
     * series, …) for the given media package and scope.
     */
    fun getAcl(mp: MediaPackage, scope: AclScope): Tuple<AccessControlList, AclScope>

    /**
     * Attaches the provided policies to a media package as a XACML attachment, replacing any previous policy element of
     * the same scope.
     *
     * @param mp
     * the media package
     * @param scope
     * scope of the ACL
     * @param acl
     * the tuples of roles to actions
     * @return the mutated (!) media package with attached XACML policy and the XACML attachment
     */
    @Throws(MediaPackageException::class)
    fun setAcl(mp: MediaPackage, scope: AclScope, acl: AccessControlList): Tuple<MediaPackage, Attachment>

    /**
     * Remove the XACML of the given scope.
     *
     * @param mp
     * the media package
     * @param scope
     * scope of the ACL
     * @return the mutated (!) media package with removed XACML policy
     */
    fun removeAcl(mp: MediaPackage, scope: AclScope): MediaPackage
}
