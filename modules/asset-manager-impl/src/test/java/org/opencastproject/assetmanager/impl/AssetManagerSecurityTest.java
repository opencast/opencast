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
package org.opencastproject.assetmanager.impl;

import static org.junit.Assert.fail;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.impl.util.TestOrganization;
import org.opencastproject.assetmanager.impl.util.TestUser;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;

import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Optional;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class AssetManagerSecurityTest extends AssetManagerTestBase {
  private SecurityService securityService;

  private User globalAdmin;
  private User orgAdmin;
  private User someUser;
  private User captureAgent;

  public void setupSecurityService(User currentUser) {
    securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andAnswer(() -> currentUser).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andAnswer(() -> currentUser.getOrganization()).anyTimes();
    EasyMock.replay(securityService);
    am.setSecurityService(securityService);
  }

  public void setupAuthService(MediaPackage mediaPackage, boolean allowed) {
    final AuthorizationService authorizationService = EasyMock.createMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.getActiveAcl(EasyMock.anyObject(MediaPackage.class))).andAnswer(
        () -> tuple(new AccessControlList(), AclScope.Episode)).anyTimes();
    EasyMock.expect(authorizationService.hasPermission(EasyMock.eq(mediaPackage), (String) EasyMock.anyObject()))
        .andReturn(allowed).anyTimes();
    EasyMock.replay(authorizationService);
    am.setAuthorizationService(authorizationService);
  }

  @Test
  @Parameters(method = "userAccessParameters")
  public void testGetMediaPackage(User currentUser, boolean allowed) throws Exception {
    final MediaPackage mp = mkMediaPackage(mkCatalog());
    am.takeSnapshot(OWNER, mp);

    setupSecurityService(currentUser);
    setupAuthService(mp, allowed);

    Optional<MediaPackage> optMp = am.getMediaPackage(mp.getIdentifier().toString());
    if (optMp.isPresent() && !allowed) {
      fail("Access should not be granted to " + currentUser.getUsername());
    }
    if (optMp.isEmpty() && allowed) {
      fail("Access should be granted " + currentUser.getUsername());
    }
  }

  @Test
  @Parameters(method = "userAccessParameters")
  public void testCreateSnapshot(User currentUser, boolean allowed) throws Exception {
    final MediaPackage mp = mkMediaPackage(mkCatalog());

    setupSecurityService(currentUser);
    setupAuthService(mp, allowed);

    try {
      am.takeSnapshot(mp);
    } catch (RuntimeException e) {
      if (e.getCause() instanceof UnauthorizedException && allowed) {
        fail("Access should be granted " + currentUser.getUsername());
      }
    }
  }

  @Test
  @Parameters(method = "userAccessParameters")
  public void testSetAvailability(User currentUser, boolean allowed) throws Exception {
    final MediaPackage mp = mkMediaPackage(mkCatalog());
    Snapshot snapshot = am.takeSnapshot(OWNER, mp);

    setupSecurityService(currentUser);
    setupAuthService(mp, allowed);

    try {
      am.setAvailability(
          snapshot.getVersion(),
          snapshot.getMediaPackage().getIdentifier().toString(),
          Availability.OFFLINE);
      if (!allowed) {
        fail("Access should not be granted " + currentUser.getUsername());
      }
    } catch (RuntimeException e) {
      if (e.getCause() instanceof UnauthorizedException && allowed) {
        fail("Access should be granted " + currentUser.getUsername());
      }
    }

    try {
      am.setProperty(Property.mk(
          PropertyId.mk(
              snapshot.getMediaPackage().getIdentifier().toString(),
              "namespace",
              "property-name"),
          Value.mk("value")));
      if (!allowed) {
        fail("Access should not be granted " + currentUser.getUsername());
      }
    } catch (RuntimeException e) {
      if (e.getCause() instanceof UnauthorizedException && allowed) {
        fail("Access should be granted " + currentUser.getUsername());
      }
    }
  }

  @Test
  @Parameters(method = "userAccessParameters")
  public void testGetAsset(User currentUser, boolean allowed) throws Exception {
    final MediaPackage mp = mkMediaPackage(mkCatalog());
    Snapshot snapshot = am.takeSnapshot(OWNER, mp);

    setupSecurityService(currentUser);
    setupAuthService(mp, allowed);

    try {
      am.getAsset(
          snapshot.getVersion(),
          snapshot.getMediaPackage().getIdentifier().toString(),
          snapshot.getMediaPackage().getElements()[0].getIdentifier()).isPresent();
      if (!allowed) {
        fail("Access should not be granted " + currentUser.getUsername());
      }
    } catch (RuntimeException e) {
      if (e.getCause() instanceof UnauthorizedException && allowed) {
        fail("Access should be granted " + currentUser.getUsername());
      }
    }
  }

  private Object userAccessParameters() {
    final Organization org = TestOrganization.mkDefault();
    globalAdmin = TestUser.mk("globalAdmin", org, SecurityConstants.GLOBAL_ADMIN_ROLE);
    orgAdmin = TestUser.mk("orgAdmin", org, DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN);
    someUser = TestUser.mk("someUser", org, "");
    captureAgent = TestUser.mk("captureAgent", org, SecurityConstants.GLOBAL_CAPTURE_AGENT_ROLE);

    return new Object[]{
        new Object[]{globalAdmin, true},
        new Object[]{orgAdmin, true},
        new Object[]{someUser, true},
        new Object[]{someUser, false},
        new Object[]{captureAgent, true}
    };
  }

  @Override
  public AssetManagerImpl makeAssetManager() throws Exception {
    AssetManagerImpl am = super.makeAssetManager();
    return am;
  }
}
