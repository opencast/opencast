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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.assetmanager.impl.AssetManagerWithSecurity.READ_ACTION;
import static org.opencastproject.assetmanager.impl.AssetManagerWithSecurity.WRITE_ACTION;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.impl.util.TestOrganization;
import org.opencastproject.assetmanager.impl.util.TestUser;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.util.data.Tuple;

import com.entwinemedia.fn.P1;
import com.entwinemedia.fn.P1Lazy;
import com.entwinemedia.fn.Prelude;
import com.entwinemedia.fn.ProductBuilder;
import com.entwinemedia.fn.Unit;
import com.entwinemedia.fn.data.Opt;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class AssetManagerWithSecurityTest extends AbstractTieredStorageAssetManagerTest<AssetManagerWithSecurity> {
  public static final String ROLE_TEACHER = "ROLE_TEACHER";
  public static final String ROLE_USER = "ROLE_USER";
  public static final String ROLE_STUDENT = "ROLE_STUDENT";
  public static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";
  public static final String ROLE_ORG_ADMIN = "ROLE_ORG_ADMIN";

  private static ProductBuilder p = com.entwinemedia.fn.Products.E;

  private SecurityService secSvc;

  // Yikes, mutable state! Watch out...
  private User currentUser = TestUser.mk(TestOrganization.mkDefault(), new HashSet<Role>());
  private AccessControlList currentMediaPackageAcl = acl();

  @Before
  public void setUp() throws Exception {
    setUp(mkTestEnvironment());
  }

  /**
   * Setup the test environment.
   */
  public AssetManagerWithSecurity mkTestEnvironment() throws Exception {
    final AuthorizationService authSvc = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authSvc.getActiveAcl(EasyMock.<MediaPackage>anyObject())).andAnswer(new IAnswer<Tuple<AccessControlList, AclScope>>() {
      @Override
      public Tuple<AccessControlList, AclScope> answer() throws Throwable {
        return tuple(currentMediaPackageAcl, AclScope.Episode);
      }
    }).anyTimes();
    EasyMock.replay(authSvc);
    //
    secSvc = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(secSvc.getUser()).andAnswer(new IAnswer<User>() {
      @Override public User answer() throws Throwable {
        return currentUser;
      }
    }).anyTimes();
    EasyMock.expect(secSvc.getOrganization()).andAnswer(new IAnswer<Organization>() {
      @Override public Organization answer() throws Throwable {
        return currentUser.getOrganization();
      }
    }).anyTimes();
    EasyMock.replay(secSvc);
    //
    return new AssetManagerWithSecurity(mkTieredStorageAM(), authSvc, secSvc);
  }

  @Override public AbstractAssetManager getAbstractAssetManager() {
    return (AbstractAssetManager) am.delegate;
  }

  @Override public String getCurrentOrgId() {
    return secSvc.getOrganization().getId();
  }

  /**
   * Evaluate product <code>p</code> in a given security context.
   *
   * @param user
   *     the user under whose roles and organization <code>p</code> shall be evaluated
   * @param assertAccess
   *     if true assert that the current user is authorized to evaluate <code>p</code>,
   *     if false assert that the current user is prohibited to evaluate <code>p</code>
   * @param p
   *     the product that contains the calls to the asset manager
   * @return the result of the evaluation of <code>p</code>
   */
  private <A> A runWith(User user, boolean assertAccess, P1<A> p) {
    final User stashedUser = currentUser;
    currentUser = user;
    A result = null;
    try {
      result = p.get1();
      if (!assertAccess) {
        fail("Access should be prohibited");
      }
    } catch (Exception e) {
      if ((e instanceof UnauthorizedException) && assertAccess) {
        fail("Access should be granted");
      } else if (!(e instanceof UnauthorizedException)) {
        Prelude.chuck(e);
      }
    }
    currentUser = stashedUser;
    return result;
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  @Test
  @Parameters
  public void testTakeSnapshot(final AccessControlList acl, User user,
                               final boolean assertAccess) throws Exception {
    createSnapshot(acl, user, assertAccess);
  }

  private Object parametersForTestTakeSnapshot() {
    final Organization org = TestOrganization.mkDefault();
    return $a($a(acl(ace(ROLE_TEACHER, WRITE_ACTION)),
                 TestUser.mk(org, ROLE_USER),
                 false),
              $a(acl(ace(ROLE_TEACHER, WRITE_ACTION)),
                 TestUser.mk(org, ROLE_USER, ROLE_TEACHER),
                 true),
              $a(acl(ace(ROLE_TEACHER, WRITE_ACTION)),
                 TestUser.mk(org),
                 false),
              $a(acl(),
                 TestUser.mk(org, SecurityConstants.GLOBAL_ADMIN_ROLE),
                 true));
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /**
   * Media package is created under the admin of the default organization.
   */
  @Test
  @Parameters
  public void testSetAvailabilityAndSetProperty(
      final AccessControlList acl,
      User user,
      boolean assertGrant) throws Exception {
    // create a snapshot
    final Snapshot snapshot = createSnapshot(acl);
    runWith(user, assertGrant, new P1Lazy<Unit>() {
      @Override public Unit get1() {
        // set availability
        am.setAvailability(
            snapshot.getVersion(),
            snapshot.getMediaPackage().getIdentifier().toString(),
            Availability.OFFLINE);
        // set a property
        assertTrue(am.setProperty(Property.mk(
            PropertyId.mk(
                snapshot.getMediaPackage().getIdentifier().toString(),
                "namespace",
                "property-name"),
            Value.mk("value"))));
        return Unit.unit;
      }
    });
  }

  private Object parametersForTestSetAvailabilityAndSetProperty() {
    final Organization org = TestOrganization.mkDefault();
    return $a($a(acl(ace(ROLE_TEACHER, WRITE_ACTION)),
                 TestUser.mk(org, ROLE_USER),
                 false),
              $a(acl(ace(ROLE_TEACHER, WRITE_ACTION)),
                 TestUser.mk(org, ROLE_USER, ROLE_STUDENT),
                 false),
              $a(acl(ace(ROLE_TEACHER, WRITE_ACTION)),
                 TestUser.mk(org, ROLE_USER, ROLE_TEACHER),
                 true),
              $a(acl(ace(ROLE_TEACHER, WRITE_ACTION)),
                 TestUser.mk(org, ROLE_TEACHER),
                 true),
              $a(acl(ace(ROLE_TEACHER, READ_ACTION)),
                 TestUser.mk(org, ROLE_TEACHER),
                 false),
              $a(acl(),
                 TestUser.mk(org, SecurityConstants.GLOBAL_ADMIN_ROLE),
                 true),
              $a(acl(),
                 TestUser.mk(org, org.getAdminRole()),
                 true));
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /**
   * Media package is created under the admin of the default organization.
   */
  @Test
  @Parameters
  public void testGetAsset(final AccessControlList acl, User user,
                           boolean assertAccess) throws Exception {
    // create a snapshot
    final Snapshot snapshot = createSnapshot(acl);
    // get an asset of the snapshot
    runWith(user, assertAccess, new P1Lazy<Unit>() {
      @Override public Unit get1() {
        assertTrue(am.getAsset(
            snapshot.getVersion(),
            snapshot.getMediaPackage().getIdentifier().toString(),
            snapshot.getMediaPackage().getElements()[0].getIdentifier()).isSome());
        return Unit.unit;
      }
    });
  }

  private Object parametersForTestGetAsset() {
    final Organization org = TestOrganization.mkDefault();
    return $a($a(acl(ace(ROLE_TEACHER, WRITE_ACTION)),
                 TestUser.mk(org, ROLE_TEACHER),
                 false),
              $a(acl(ace(ROLE_TEACHER, READ_ACTION)),
                 TestUser.mk(org, ROLE_USER, ROLE_STUDENT),
                 false),
              $a(acl(ace(ROLE_TEACHER, READ_ACTION)),
                 TestUser.mk(org, ROLE_USER, ROLE_TEACHER),
                 true),
              $a(acl(ace(ROLE_TEACHER, READ_ACTION)),
                 TestUser.mk(org, ROLE_TEACHER),
                 true),
              $a(acl(ace(ROLE_TEACHER, READ_ACTION)),
                 mkGlobalAdmin(org),
                 true),
              $a(acl(),
                 mkOrgAdmin(org),
                 true),
              $a(acl(),
                 mkDefaultOrgGlobalAdmin(),
                 true));
  }



  /* ------------------------------------------------------------------------------------------------------------------ */

  @Test
  @Parameters
  public void testQuery(
      AccessControlList acl,
      User writeUser, User queryUser,
      final boolean assertReadAccess, final boolean assertWriteAccess)
      throws Exception {
    // create a snapshot -> should always work (set assertAccess to true)
    final Snapshot snapshot = createSnapshot(acl, writeUser, true);
    // Set assertAccess to true since querying does not yield a security exception.
    // Restricted records are simply filtered out.
    runWith(queryUser, true, new P1Lazy<Unit>() {
      @Override public Unit get1() {
        // if read access is granted the result contains one record
        assertEquals("Snapshot should be retrieved: " + assertReadAccess,
                     assertReadAccess,
                     q.select(q.snapshot()).run().getSize() == 1);
        return Unit.unit;
      }
    });
    runWith(queryUser, true, new P1Lazy<Unit>() {
      @Override public Unit get1() {
        // if write access is granted one snapshot should be deleted
        assertEquals("Snapshots should be deleted: " + assertWriteAccess,
                     assertWriteAccess,
                     q.delete(OWNER, q.snapshot()).run() == 1);
        return Unit.unit;
      }
    });
  }

  private Object parametersForTestQuery() {
    final Organization org1 = TestOrganization.mk("org1", ROLE_ANONYMOUS, ROLE_ORG_ADMIN);
    final Organization org2 = TestOrganization.mk("org2", ROLE_ANONYMOUS, ROLE_ORG_ADMIN);
    return $a(
        // make sure that a role with read rights can access its episodes
        $a(acl(ace(ROLE_TEACHER, READ_ACTION), ace(ROLE_TEACHER, WRITE_ACTION)),
           TestUser.mk(org1, ROLE_TEACHER),
           TestUser.mk(org1, ROLE_TEACHER),
           true,
           true),
        // make sure that roles without read rights cannot read
        $a(acl(ace(ROLE_TEACHER, WRITE_ACTION)),
           TestUser.mk(org1, ROLE_TEACHER),
           TestUser.mk(org1, ROLE_TEACHER),
           false,
           true),
        // make sure that a different role cannot read
        $a(acl(ace(ROLE_USER, READ_ACTION), ace(ROLE_USER, WRITE_ACTION)),
           TestUser.mk(org1, ROLE_USER),
           TestUser.mk(org1, ROLE_TEACHER),
           false,
           false),
        // make sure that the organization's admin can always read the episodes of her organization
        $a(acl(ace(ROLE_TEACHER, READ_ACTION), ace(ROLE_TEACHER, WRITE_ACTION)),
           TestUser.mk(org1, ROLE_TEACHER),
           TestUser.mk(org1, org1.getAdminRole()),
           true,
           true),
        // make sure that the global admin is always allowed to read
        $a(acl(ace(ROLE_TEACHER, READ_ACTION), ace(ROLE_TEACHER, WRITE_ACTION)),
           TestUser.mk(org1, ROLE_TEACHER),
           TestUser.mk(org1, SecurityConstants.GLOBAL_ADMIN_ROLE),
           true,
           true),
        // make sure that the global admin is always allowed to read, no matter what organization she is from
        $a(acl(ace(ROLE_TEACHER, READ_ACTION), ace(ROLE_TEACHER, WRITE_ACTION)),
           TestUser.mk(org1, ROLE_TEACHER),
           TestUser.mk(org2, SecurityConstants.GLOBAL_ADMIN_ROLE),
           true,
           true),
        // make sure that even if the admin roles are named the same, an admin from one organization
        // cannot read the episodes from a another one.
        $a(acl(ace(ROLE_TEACHER, READ_ACTION), ace(ROLE_TEACHER, WRITE_ACTION)),
           TestUser.mk(org1, ROLE_TEACHER),
           TestUser.mk(org2, org2.getAdminRole()),
           false,
           false)
    );
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /**
   * Create a test media package and take a snapshot under the rights of the admin of the default organization.
   *
   * @param acl
   *     ACL of the media package to snapshot
   */
  private Snapshot createSnapshot(final AccessControlList acl) {
    return createSnapshot(acl, mkDefaultOrgAdmin(), true);
  }

  private Snapshot createSnapshot(final AccessControlList acl, final User user,
                                  boolean assertAccess) {
    return runWith(user, assertAccess, new P1Lazy<Snapshot>() {
      @Override public Snapshot get1() {
        final AccessControlList stashedAcl = currentMediaPackageAcl;
        currentMediaPackageAcl = acl;
        final Snapshot[] snapshot = createAndAddMediaPackages(1, 1, 1, Opt.<String>none());
        currentMediaPackageAcl = stashedAcl;
        return snapshot[0];
      }
    });
  }

  /** Create a user with admin rights who belongs to the default organization. */
  private User mkDefaultOrgAdmin() {
    final Organization org = TestOrganization.mkDefault();
    return TestUser.mk(org, org.getAdminRole());
  }

  /** Create a user with admin rights who belongs to the given organization. */
  private User mkOrgAdmin(Organization org) {
    return TestUser.mk(org, org.getAdminRole());
  }

  /** Create a user with global admin rights who belongs to the default organization. */
  private User mkDefaultOrgGlobalAdmin() {
    final Organization org = TestOrganization.mkDefault();
    return TestUser.mk(org, SecurityConstants.GLOBAL_ADMIN_ROLE);
  }

  /** Create a user with global admin rights who belongs to the given organization. */
  private User mkGlobalAdmin(Organization org) {
    return TestUser.mk(org, SecurityConstants.GLOBAL_ADMIN_ROLE);
  }

  private AccessControlList acl(AccessControlEntry... ace) {
    return new AccessControlList(ace);
  }

  /**
   * Create an allow rule (Opencast only support allow!)
   */
  private AccessControlEntry ace(String role, String action) {
    return new AccessControlEntry(role, action, true);
  }
}
