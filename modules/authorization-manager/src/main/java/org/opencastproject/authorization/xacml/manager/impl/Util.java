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

package org.opencastproject.authorization.xacml.manager.impl;

import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

/**
 * General helper functions.
 */
public final class Util {
  private Util() {
  }

  public static final Function<ManagedAcl, AccessControlList> toAcl = new Function<ManagedAcl, AccessControlList>() {
    @Override
    public AccessControlList apply(ManagedAcl managedAcl) {
      return managedAcl.getAcl();
    }
  };

  public static Function<Long, Option<ManagedAcl>> getManagedAcl(final AclService aclService) {
    return new Function<Long, Option<ManagedAcl>>() {
      @Override
      public Option<ManagedAcl> apply(Long aclId) {
        return aclService.getAcl(aclId);
      }
    };
  }
}
