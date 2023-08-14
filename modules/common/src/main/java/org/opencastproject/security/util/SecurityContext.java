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

package org.opencastproject.security.util;

import static org.opencastproject.util.EqualsUtil.ne;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

import java.util.function.Supplier;

/**
 * This class handles all the boilerplate of setting up and tearing down a security context. It also makes it possible
 * to pass around contexts so that clients need not deal with services, users, passwords etc.
 */
public class SecurityContext {
  private final SecurityService sec;
  private final User user;
  private final Organization org;

  public SecurityContext(SecurityService sec, Organization org, User user) {
    if (ne(org, user.getOrganization())) {
      throw new IllegalArgumentException("User is not a member of organization " + org.getId());
    }
    this.sec = sec;
    this.user = user;
    this.org = org;
  }

  /** Run function <code>f</code> within the context. */
  public <A> A runInContext(Supplier<A> f) {
    final Organization prevOrg = sec.getOrganization();
    // workaround: if no organization is bound to the current thread sec.getUser() will throw a NPE
    final User prevUser = prevOrg != null ? sec.getUser() : null;
    sec.setOrganization(org);
    sec.setUser(user);
    try {
      return f.get();
    } finally {
      sec.setOrganization(prevOrg);
      sec.setUser(prevUser);
    }
  }

  /** Run function <code>f</code> within the context. */
  public void runInContext(Runnable f) {
    runInContext(() -> {
      f.run();
      return null;
    });
  }
}
