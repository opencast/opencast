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
package org.opencastproject.scheduler.impl;

import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATED;

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.CatalogImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SchedulerUtilTest {

  private Workspace workspace;
  private File workspaceFile;

  @Before
  public void setUp() throws Exception {
    workspaceFile = File.createTempFile("dublincore", "xml");

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject(URI.class))).andReturn(workspaceFile).anyTimes();
    EasyMock.replay(workspace);
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(workspaceFile);
  }

  @Test
  public void testSortByCatalogId() throws Exception {
    String catalogId = "b";
    Catalog catalog = CatalogImpl.fromURI(new URI("location" + catalogId));
    catalog.setIdentifier(catalogId);
    String extendedCatalogId = "a";
    Catalog extendedCatalog = CatalogImpl.fromURI(new URI("location" + extendedCatalogId));
    extendedCatalog.setIdentifier(extendedCatalogId);

    Assert.assertEquals(1, SchedulerUtil.sortCatalogById.compare(catalog, extendedCatalog));
  }

  @Test
  public void testCalculateChecksum() throws Exception {
    String extendedFlavorType = "extended";
    DublinCoreCatalog dc = SchedulerServiceImplTest.generateExtendedEvent(Opt.<String> none(), extendedFlavorType);
    FileUtils.writeStringToFile(workspaceFile, dc.toXmlString(), "UTF-8");

    List<MediaPackageElementFlavor> catalogAdapterFlavors = new ArrayList<>();
    catalogAdapterFlavors.add(new MediaPackageElementFlavor(extendedFlavorType, "episode"));

    AccessControlList acl = new AccessControlList(new AccessControlEntry("ROLE_ADMIN", "write", true));

    Date start = new Date(DateTimeSupport.fromUTC("2008-03-16T14:00:00Z"));
    Date end = new Date(DateTimeSupport.fromUTC("2008-03-16T15:00:00Z"));
    String captureDeviceID = "demo";
    String seriesId = "series1";
    Set<String> userIds = new HashSet<>();
    userIds.add("user2");
    userIds.add("user1");
    MediaPackage mp = SchedulerServiceImplTest.generateEvent(Opt.<String> none());
    mp.setSeries(seriesId);
    DublinCoreCatalog event = SchedulerServiceImplTest.generateEvent(captureDeviceID, start, end);
    event.set(PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(start, Precision.Minute));
    String catalogId = UUID.randomUUID().toString();
    MediaPackageElement catalog = mp.add(new URI("location" + catalogId), Type.Catalog, event.getFlavor());
    catalog.setIdentifier(catalogId);
    String extendedCatalogId = UUID.randomUUID().toString();
    MediaPackageElement extendedCatalog = mp.add(new URI("location" + extendedCatalogId), Type.Catalog, dc.getFlavor());
    extendedCatalog.setIdentifier(extendedCatalogId);
    Map<String, String> caProperties = SchedulerServiceImplTest.generateCaptureAgentMetadata("demo");

    Map<String, String> wfProperties = new HashMap<String, String>();
    wfProperties.put("test", "true");
    wfProperties.put("clear", "all");

    String expectedChecksum = "91f54dbcb65d2759e79f1da9edce7915";
    String checksum = SchedulerUtil.calculateChecksum(workspace, catalogAdapterFlavors, start, end, captureDeviceID,
            userIds, mp, Opt.some(event), wfProperties, caProperties, false, acl);
    Assert.assertEquals(expectedChecksum, checksum);

    // change start date
    start = new Date();

    checksum = SchedulerUtil.calculateChecksum(workspace, catalogAdapterFlavors, start, end, captureDeviceID, userIds,
            mp, Opt.some(event), wfProperties, caProperties, false, acl);
    Assert.assertNotEquals(expectedChecksum, checksum);

    // change end date
    start = new Date(DateTimeSupport.fromUTC("2008-03-16T14:00:00Z"));
    end = new Date();

    checksum = SchedulerUtil.calculateChecksum(workspace, catalogAdapterFlavors, start, end, captureDeviceID, userIds,
            mp, Opt.some(event), wfProperties, caProperties, false, acl);
    Assert.assertNotEquals(expectedChecksum, checksum);

    // change device
    end = new Date(DateTimeSupport.fromUTC("2008-03-16T15:00:00Z"));
    captureDeviceID = "demo1";

    checksum = SchedulerUtil.calculateChecksum(workspace, catalogAdapterFlavors, start, end, captureDeviceID, userIds,
            mp, Opt.some(event), wfProperties, caProperties, false, acl);
    Assert.assertNotEquals(expectedChecksum, checksum);

    // change users
    captureDeviceID = "demo";
    userIds.add("test");

    checksum = SchedulerUtil.calculateChecksum(workspace, catalogAdapterFlavors, start, end, captureDeviceID, userIds,
            mp, Opt.some(event), wfProperties, caProperties, false, acl);
    Assert.assertNotEquals(expectedChecksum, checksum);

    // change episode dublincore
    userIds.remove("test");
    catalog.setChecksum(null);
    event.set(PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(end, Precision.Minute));

    checksum = SchedulerUtil.calculateChecksum(workspace, catalogAdapterFlavors, start, end, captureDeviceID, userIds,
            mp, Opt.some(event), wfProperties, caProperties, false, acl);
    Assert.assertNotEquals(expectedChecksum, checksum);

    // change extended dublincore
    catalog.setChecksum(null);
    event.set(PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(start, Precision.Minute));
    extendedCatalog.setChecksum(null);
    dc.set(PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(start, Precision.Minute));
    FileUtils.writeStringToFile(workspaceFile, dc.toXmlString(), "UTF-8");

    checksum = SchedulerUtil.calculateChecksum(workspace, catalogAdapterFlavors, start, end, captureDeviceID, userIds,
            mp, Opt.some(event), wfProperties, caProperties, false, acl);
    Assert.assertNotEquals(expectedChecksum, checksum);

    // change wf properties
    extendedCatalog.setChecksum(null);
    dc.remove(PROPERTY_CREATED);
    FileUtils.writeStringToFile(workspaceFile, dc.toXmlString(), "UTF-8");
    wfProperties.put("change", "change");

    checksum = SchedulerUtil.calculateChecksum(workspace, catalogAdapterFlavors, start, end, captureDeviceID, userIds,
            mp, Opt.some(event), wfProperties, caProperties, false, acl);
    Assert.assertNotEquals(expectedChecksum, checksum);

    // change ca properties
    wfProperties.remove("change");
    caProperties.put("change", "change");

    checksum = SchedulerUtil.calculateChecksum(workspace, catalogAdapterFlavors, start, end, captureDeviceID, userIds,
            mp, Opt.some(event), wfProperties, caProperties, false, acl);
    Assert.assertNotEquals(expectedChecksum, checksum);

    // change opt out status
    caProperties.remove("change");

    checksum = SchedulerUtil.calculateChecksum(workspace, catalogAdapterFlavors, start, end, captureDeviceID, userIds,
            mp, Opt.some(event), wfProperties, caProperties, true, acl);
    Assert.assertNotEquals(expectedChecksum, checksum);

    checksum = SchedulerUtil.calculateChecksum(workspace, catalogAdapterFlavors, start, end, captureDeviceID, userIds,
            mp, Opt.some(event), wfProperties, caProperties, false, acl);
    Assert.assertEquals(expectedChecksum, checksum);

    // change access control list
    checksum = SchedulerUtil.calculateChecksum(workspace, catalogAdapterFlavors, start, end, captureDeviceID, userIds,
            mp, Opt.some(event), wfProperties, caProperties, false,
            new AccessControlList(new AccessControlEntry("ROLE_ADMIN", "write", false)));
    Assert.assertNotEquals(expectedChecksum, checksum);
  }

}
