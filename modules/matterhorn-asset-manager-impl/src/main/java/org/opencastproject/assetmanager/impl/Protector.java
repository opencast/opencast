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
package org.opencastproject.assetmanager.impl;

import static com.entwinemedia.fn.Stream.$;

import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;

import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.P1;
import com.entwinemedia.fn.Pred;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class Protector {
  private final SecurityService secSvc;

  public Protector(SecurityService secSvc) {
    this.secSvc = secSvc;
  }

  /**
   * Evaluate a product if the current user is authorized to perform the given actions.
   */
  public <A> Protected<A> protect(final AccessControlList acl, List<String> actions, P1<A> p) {
    final User user = secSvc.getUser();
    final Organization org = secSvc.getOrganization();
    final Pred<String> isAuthorizedToDo = new Pred<String>() {
      @Override public Boolean apply(String action) {
        return AccessControlUtil.isAuthorized(acl, user, org, action);
      }
    };
    final boolean isAuthorized = $(actions).map(isAuthorizedToDo).foldl(false, or);
    return isAuthorized
            ? Protected.granted(p.get1())
            : Protected.<A>rejected(new UnauthorizedException(user, $(actions).mkString(",")));
  }

  public static final Fn2<Boolean, Boolean, Boolean> or = new Fn2<Boolean, Boolean, Boolean>() {
    @Override public Boolean apply(Boolean a, Boolean b) {
      return a || b;
    }
  };
}
