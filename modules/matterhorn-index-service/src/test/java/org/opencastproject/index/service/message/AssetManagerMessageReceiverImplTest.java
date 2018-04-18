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
package org.opencastproject.index.service.message;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem.TakeSnapshot;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;

public class AssetManagerMessageReceiverImplTest {

  private AssetManagerMessageReceiverImpl assetManager;
  private Workspace workspace;
  private final TestSearchIndex index = new TestSearchIndex();

  @Before
  public void setUp() throws Exception {
    workspace = createNiceMock(Workspace.class);
    expect(workspace.read(EasyMock.anyObject(URI.class)))
            .andReturn(new File(getClass().getResource("/dublincore.xml").toURI())).anyTimes();
    replay(workspace);

    AclService aclService = createNiceMock(AclService.class);
    expect(aclService.getAcls()).andReturn(new ArrayList<ManagedAcl>()).anyTimes();
    replay(aclService);

    DefaultOrganization organization = new DefaultOrganization();
    AclServiceFactory aclServiceFactory = createNiceMock(AclServiceFactory.class);
    expect(aclServiceFactory.serviceFor(organization)).andReturn(aclService).anyTimes();
    replay(aclServiceFactory);

    SecurityService securityService = TestSearchIndex.createSecurityService(organization);

    assetManager = new AssetManagerMessageReceiverImpl();
    assetManager.setAclServiceFactory(aclServiceFactory);
    assetManager.setSecurityService(securityService);
    assetManager.setSearchIndex(index);
  }

  @Test
  public void testUpdateCreator() throws Exception {
    MediaPackage mediaPackage = new MediaPackageBuilderImpl()
            .loadFromXml(getClass().getResourceAsStream("/jobs_mediapackage1.xml"));
    TakeSnapshot takeSnapshot = AssetManagerItem.add(workspace, mediaPackage, new AccessControlList(), 0, new Date());

    // Test initial set of creator
    assetManager.execute(takeSnapshot);
    Event event = index.getEventResult();
    assertNotNull(event);
    assertEquals("Current user is expected to be creator as no other creator has been set explicitly", "Creator",
            event.getCreator());

    // Test updating creator
    event.setCreator("Hans");
    index.setInitialEvent(event);
    assetManager.execute(takeSnapshot);
    event = index.getEventResult();
    assertNotNull(event);
    assertEquals("Creator has been updated", "Hans", event.getCreator());
  }

}
