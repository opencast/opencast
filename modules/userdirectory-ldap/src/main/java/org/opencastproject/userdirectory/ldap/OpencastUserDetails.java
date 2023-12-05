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

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.naming.Name;

public class OpencastUserDetails implements UserDetails {

  private String dn;

  private String password;

  private String username;

  private String name;

  private String mail;

  private Collection<GrantedAuthority> authorities = AuthorityUtils.NO_AUTHORITIES;
  private boolean accountNonExpired = true;
  private boolean accountNonLocked = true;
  private boolean credentialsNonExpired = true;
  private boolean enabled = true;

  protected OpencastUserDetails() {
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof OpencastUserDetails) {
      return dn.equals(((OpencastUserDetails) obj).dn);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return dn.hashCode();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(super.toString()).append(": ");
    sb.append("Dn: ").append(dn).append("; ");
    sb.append("Username: ").append(this.username).append("; ");
    sb.append("Password: [PROTECTED]; ");
    sb.append("Name: ").append(this.name);
    sb.append("Mail: ").append(this.mail);
    sb.append("Enabled: ").append(this.enabled).append("; ");
    sb.append("AccountNonExpired: ").append(this.accountNonExpired).append("; ");
    sb.append("CredentialsNonExpired: ").append(this.credentialsNonExpired).append("; ");
    sb.append("AccountNonLocked: ").append(this.accountNonLocked).append("; ");

    if (this.getAuthorities() != null) {
      sb.append("Granted Authorities: ");
      boolean first = true;

      for (Object authority : this.getAuthorities()) {
        if (first) {
          first = false;
        } else {
          sb.append(", ");
        }

        sb.append(authority.toString());
      }
    } else {
      sb.append("Not granted any authorities");
    }

    return sb.toString();
  }

  public String getDn() {
    return dn;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  public String getMail() {
    return mail;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean isAccountNonExpired() {
    return accountNonExpired;
  }

  @Override
  public boolean isAccountNonLocked() {
    return accountNonLocked;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return credentialsNonExpired;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public static class Essence {
    protected OpencastUserDetails instance = createTarget();
    private List<GrantedAuthority> mutableAuthorities = new ArrayList<>();

    public Essence() {
    }

    protected OpencastUserDetails createTarget() {
      return new OpencastUserDetails();
    }

    /**
     * Adds the authority to the list, unless it is already there, in which case it is ignored
     */
    public void addAuthority(GrantedAuthority a) {
      if (!hasAuthority(a)) {
        mutableAuthorities.add(a);
      }
    }

    private boolean hasAuthority(GrantedAuthority a) {
      for (GrantedAuthority authority : mutableAuthorities) {
        if (authority.equals(a)) {
          return true;
        }
      }
      return false;
    }

    public OpencastUserDetails createUserDetails() {
      Objects.requireNonNull(instance,"Essence can only be used to create a single instance");
      Objects.requireNonNull(instance, "Essence can only be used to create a single instance");
      Objects.requireNonNull(instance.username, "username must not be null");
      Objects.requireNonNull(instance.getDn(), "Distinguished name must not be null");

      instance.authorities = Collections.unmodifiableList(mutableAuthorities);

      OpencastUserDetails newInstance = instance;

      instance = null;

      return newInstance;
    }

    public Collection<GrantedAuthority> getGrantedAuthorities() {
      return mutableAuthorities;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
      instance.accountNonExpired = accountNonExpired;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
      instance.accountNonLocked = accountNonLocked;
    }

    public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
      mutableAuthorities = new ArrayList<>();
      mutableAuthorities.addAll(authorities);
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
      instance.credentialsNonExpired = credentialsNonExpired;
    }

    public void setDn(String dn) {
      instance.dn = dn;
    }

    public void setDn(Name dn) {
      instance.dn = dn.toString();
    }

    public void setEnabled(boolean enabled) {
      instance.enabled = enabled;
    }

    public void setPassword(String password) {
      instance.password = password;
    }

    public void setUsername(String username) {
      instance.username = username;
    }

    public void setName(String name) {
      instance.name = name;
    }

    public void setMail(String mail) {
      instance.mail = mail;
    }

  }
}
