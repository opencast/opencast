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

package org.opencastproject.userdirectory.api;

import org.opencastproject.security.impl.jpa.JpaUserReference;

public interface UserReferenceProvider {

    /**
     * Add a user reference
     *
     * @param user
     *            the user reference to be added
     * @param mechanism
     *            the mechanism that adds the user reference
     */
    void addUserReference(JpaUserReference user,
            String mechanism);

    /**
     * Update an existing user reference
     * 
     * @param user
     *            the user reference to be updated
     */
    void updateUserReference(JpaUserReference user);

    /**
     * Returns the persisted user reference by the user name and organization id
     *
     * @param userName
     *            the user name
     * @param organizationId
     *            the organization id
     * @return the user reference or <code>null</code> if not found
     */
    JpaUserReference findUserReference(String userName, String organizationId);

}
