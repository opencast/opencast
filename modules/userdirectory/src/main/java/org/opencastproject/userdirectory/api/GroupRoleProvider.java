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

import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.GroupProvider;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.impl.jpa.JpaGroup;
import org.opencastproject.util.NotFoundException;

import java.util.Iterator;
import java.util.List;

public interface GroupRoleProvider extends GroupProvider, RoleProvider {


    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
     */
    List<Role> getRolesForUser(String userName);

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
     */
    List<Role> getRolesForGroup(String groupName);

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.RoleProvider#getOrganization()
     */
    String getOrganization();

    /**
     * Updates a user's group membership
     *
     * @param userName
     *          the username
     * @param orgId
     *          the user's organization
     * @param roleList
     *          the list of group role names
     */
    void updateGroupMembershipFromRoles(String userName,
            String orgId, List<String> roleList);

    /**
     * Adds or updates a group to the persistence.
     *
     * @param group
     *          the group to add
     */
    void addGroup(JpaGroup group) throws UnauthorizedException;

    /**
     * Getting all groups
     *
     * @return Iterator&lt;Group&gt; persisted groups
     */
    Iterator<Group> getGroups();

    /**
     * Update a group
     *
     * @param groupId
     *          the id of the group to update
     * @param name
     *          the name to update
     * @param description
     *          the description to update
     * @param roles
     *          the roles to update
     * @param users
     *          the users to update
     * @throws NotFoundException
     *           if the group is not found
     * @throws UnauthorizedException
     *           if the user does not have rights to update the group
     */
    void updateGroup(String groupId, String name,
            String description, String roles, String users)
            throws NotFoundException, UnauthorizedException;

}
