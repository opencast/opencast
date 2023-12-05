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

package org.opencastproject.authorization.xacml.manager.endpoint;

import static org.opencastproject.util.Jsons.Obj;
import static org.opencastproject.util.Jsons.Val;
import static org.opencastproject.util.Jsons.arr;
import static org.opencastproject.util.Jsons.obj;
import static org.opencastproject.util.Jsons.p;
import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.data.Function;

/** Converter functions from business objects to JSON structures. */
public final class JsonConv {

  public static final String KEY_ID = "id";
  public static final String KEY_NAME = "name";
  public static final String KEY_ORGANIZATION_ID = "organizationId";
  public static final String KEY_ACL = "acl";
  public static final String KEY_ACE = "ace";
  public static final String KEY_ROLE = "role";
  public static final String KEY_ACTION = "action";
  public static final String KEY_ALLOW = "allow";

  private JsonConv() {
  }

  public static Obj digest(ManagedAcl acl) {
    return obj(p(KEY_ID, acl.getId()),
               p(KEY_NAME, acl.getName()));
  }

  public static Obj full(ManagedAcl acl) {
    return obj(p(KEY_ID, acl.getId()),
               p(KEY_NAME, acl.getName()),
               p(KEY_ORGANIZATION_ID, acl.getOrganizationId()),
               p(KEY_ACL, full(acl.getAcl())));
  }

  public static final Function<ManagedAcl, Val> fullManagedAcl = new Function<ManagedAcl, Val>() {
    @Override public Val apply(ManagedAcl acl) {
      return full(acl);
    }
  };

  public static Obj full(AccessControlList acl) {
    return obj(p(KEY_ACE, arr(mlist(acl.getEntries()).map(fullAccessControlEntry))));
  }

  public static Obj full(AccessControlEntry ace) {
    return obj(p(KEY_ROLE, ace.getRole()),
               p(KEY_ACTION, ace.getAction()),
               p(KEY_ALLOW, ace.isAllow()));
  }

  public static final Function<AccessControlEntry, Val> fullAccessControlEntry
      = new Function<AccessControlEntry, Val>() {
        @Override
        public Val apply(AccessControlEntry ace) {
          return full(ace);
        }
      };
}
