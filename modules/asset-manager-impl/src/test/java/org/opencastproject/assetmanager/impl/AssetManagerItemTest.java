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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem.DeleteEpisode;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem.DeleteSnapshot;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem.TakeSnapshot;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.IoSupport;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.Date;

/**
 * Testing of the {@link AssetManagerItem} happens in this module to avoid a cyclic dependency of modules. The reason is
 * that the test class needs access to the {@link VersionImpl}.
 */
public class AssetManagerItemTest {
  @Test
  public void testSerializeUpdate() throws Exception {
    final Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject(URI.class)))
            .andReturn(new File(getClass().getResource("/dublincore-a.xml").toURI())).once();
    EasyMock.expect(workspace.read(EasyMock.anyObject(URI.class)))
            .andAnswer(() -> getClass().getResourceAsStream("/dublincore-a.xml")).once();
    EasyMock.replay(workspace);
    final MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mp.add(DublinCores.mkOpencastEpisode().getCatalog());
    final AccessControlList acl = new AccessControlList(new AccessControlEntry("admin", "read", true));
    final Date now = new Date();
    final AssetManagerItem item = AssetManagerItem.add(workspace, mp, acl, 10L, now);
    final AssetManagerItem deserialized = IoSupport.serializeDeserialize(item);
    assertEquals(item.getDate(), deserialized.getDate());
    assertEquals(item.getType(), deserialized.getType());
    assertEquals(item.decompose(TakeSnapshot.getMediaPackage, null, null).getIdentifier(),
            deserialized.decompose(TakeSnapshot.getMediaPackage, null, null).getIdentifier());
    assertEquals(item.decompose(TakeSnapshot.getAcl, null, null).getEntries(),
            deserialized.decompose(TakeSnapshot.getAcl, null, null).getEntries());
    assertTrue(DublinCoreUtil.equals(item.decompose(TakeSnapshot.getEpisodeDublincore, null, null).get(),
            deserialized.decompose(TakeSnapshot.getEpisodeDublincore, null, null).get()));
  }

  @Test
  public void testSerializeDeleteSnapshot() throws Exception {
    final Date now = new Date();
    final AssetManagerItem item = AssetManagerItem.deleteSnapshot("id", 0L, now);
    final AssetManagerItem deserialized = IoSupport.serializeDeserialize(item);
    assertEquals(item.getDate(), deserialized.getDate());
    assertEquals(item.getType(), deserialized.getType());
    assertEquals(item.decompose(null, DeleteSnapshot.getMediaPackageId, null),
            deserialized.decompose(null, DeleteSnapshot.getMediaPackageId, null));
  }

  @Test
  public void testSerializeDeleteEpisode() throws Exception {
    final Date now = new Date();
    final AssetManagerItem item = AssetManagerItem.deleteEpisode("id", now);
    final AssetManagerItem deserialized = IoSupport.serializeDeserialize(item);
    assertEquals(item.getDate(), deserialized.getDate());
    assertEquals(item.getType(), deserialized.getType());
    assertEquals(item.decompose(null, null, DeleteEpisode.getMediaPackageId),
            deserialized.decompose(null, null, DeleteEpisode.getMediaPackageId));
  }
}
