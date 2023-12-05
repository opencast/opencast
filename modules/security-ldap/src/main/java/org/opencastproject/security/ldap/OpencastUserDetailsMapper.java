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
package org.opencastproject.security.ldap;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.Collection;


public class OpencastUserDetailsMapper implements UserDetailsContextMapper {

  private UserDetailsService userDetailsService;

  /**
   * Activate component
   *
   * @param userDetailsService Reference to the UserDetailsService.
   * 
   */
  public OpencastUserDetailsMapper(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
      Collection<? extends GrantedAuthority> authorities) {
    return userDetailsService.loadUserByUsername(username);
  }

  public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
    throw new UnsupportedOperationException("OpencastUserDetailsMapper only supports reading from a context. Please"
        + "use a subclass if mapUserToContext() is required.");
  }

}
