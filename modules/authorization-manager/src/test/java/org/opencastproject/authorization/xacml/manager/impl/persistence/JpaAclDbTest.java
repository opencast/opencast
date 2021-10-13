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

package org.opencastproject.authorization.xacml.manager.impl.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.security.api.AccessControlUtil.acl;
import static org.opencastproject.security.api.AccessControlUtil.entries;
import static org.opencastproject.security.api.AccessControlUtil.entry;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.impl.ManagedAclImpl;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.persistence.PersistenceUtil;

import org.junit.Test;

/** Tests for {@link JpaAclDb}. */
public final class JpaAclDbTest {

  private JpaAclDb db;
  @Test
  public void testProvider() {

    db = new JpaAclDb();
    db.setEntityManagerFactory(PersistenceUtil.newTestEntityManagerFactory(
            "org.opencastproject.authorization.xacml.manager"));

    // add ACL to org1
    final AccessControlList publicAcl = acl(entry("anonymous", "read", true));
    final Option<ManagedAcl> acl = db.createAcl(org1, publicAcl, "public");
    assertTrue(acl.isSome());
    assertTrue(db.getAcl(org1, acl.get().getId()).isSome());
    // ACL should not be visible for org2
    assertTrue(db.getAcl(org2, acl.get().getId()).isNone());
    // create duplicate which should be denied
    assertTrue(db.createAcl(org1, publicAcl, "public").isNone());
    //
    // add another ACL to org1
    db.createAcl(org1, acl(entries("instructor", tuple("read", true), tuple("write", true))), "instructor");
    assertEquals(2, db.getAcls(org1).size());
    // org2 should still have no ACLs
    assertEquals(0, db.getAcls(org2).size());
    //
    // add same ACL to org2
    db.createAcl(org2, publicAcl, "public");
    assertEquals(1, db.getAcls(org2).size());
    assertEquals(2, db.getAcls(org1).size());
    //
    // update
    final ManagedAcl org1Acl = acl.get();
    // update with new ACL
    assertTrue(db.updateAcl(
        new ManagedAclImpl(org1Acl.getId(), org1Acl.getName(), org1Acl.getOrganizationId(),
            acl(entry("anonymous", "write", true)))
    ));
    assertEquals("write", db.getAcl(org1, org1Acl.getId()).get().getAcl().getEntries().get(0).getAction());
    // update with new name
    final ManagedAcl org1AclUpdated
        = new ManagedAclImpl(org1Acl.getId(), "public2", org1Acl.getOrganizationId(), org1Acl.getAcl());
    assertTrue(db.updateAcl(org1AclUpdated));
    assertEquals("public2", db.getAcl(org1, org1AclUpdated.getId()).get().getName());
    // try to update a non-existing ACL
    assertFalse(db.updateAcl(new ManagedAclImpl(27427492384723L, "public2", org1.getId(), org1Acl.getAcl())));
    assertEquals(2, db.getAcls(org1).size());
    // update without any update
    assertTrue(db.updateAcl(org1AclUpdated));
    assertEquals(2, db.getAcls(org1).size());
    // try to update an ACL of a different org
    assertFalse(db.updateAcl(new ManagedAclImpl(org1Acl.getId(), "bla", org2.getId(), org1Acl.getAcl())));
    //
    // delete
    assertTrue(db.deleteAcl(org1, org1Acl.getId()));
    assertEquals(1, db.getAcls(org1).size());
    // try to delete a non-existing ACL
    assertFalse(db.deleteAcl(org1, 894892374923L));
    // try to delete an ACL of a different org
    assertFalse(db.deleteAcl(org2, org1Acl.getId()));
    assertEquals(1, db.getAcls(org2).size());
  }

  private static final Organization org1 = new DefaultOrganization();
  private static final Organization org2 = new JaxbOrganization("Entwine");
}
