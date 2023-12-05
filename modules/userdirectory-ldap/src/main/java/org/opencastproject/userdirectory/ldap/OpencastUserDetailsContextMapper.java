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

package org.opencastproject.userdirectory.ldap;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class OpencastUserDetailsContextMapper implements UserDetailsContextMapper {

  private final String[] name;

  private final String mail;

  public OpencastUserDetailsContextMapper(String[] name, String mail) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(mail);
    this.name = name;
    this.mail = mail;
  }

  @Override
  public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
      Collection<? extends GrantedAuthority> authorities) {
    String dn = ctx.getNameInNamespace();

    OpencastUserDetails.Essence essence = new OpencastUserDetails.Essence();
    essence.setDn(dn);
    essence.setUsername(username);
    essence.setName(buildName(ctx));
    essence.setMail(ctx.getStringAttribute(mail));

    // Add the supplied authorities
    for (GrantedAuthority authority : authorities) {
      essence.addAuthority(authority);
    }

    return essence.createUserDetails();
  }
  private String buildName(DirContextOperations ctx) {
    StringJoiner joiner = new StringJoiner(" ");
    for (String attribute: name) {
      joiner.add(ctx.getStringAttribute(attribute));
    }
    return joiner.toString();
  }

  @Override
  public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
    throw new UnsupportedOperationException("OpencastUserContextMapper only supports reading from a context. Please"
        + "use a subclass if mapUserToContext() is required.");
  }

  public String[] getAttributes() {
    List<String> attributes = new ArrayList<>(Arrays.asList(name));
    attributes.add(mail);
    return attributes.toArray(new String[] {});
  }
}
