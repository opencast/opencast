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


package org.opencastproject.metadata.dublincore;

import static org.junit.Assert.assertNotNull;

import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;

public final class StaticMetadataServiceDublinCoreImplTest {

  @Test(expected = IllegalArgumentException.class)
  public void testDefectMetadata() throws Exception {
    MediaPackage mp = newMediaPackage("/manifest-simple-defect.xml");
    StaticMetadataServiceDublinCoreImpl ms = newStaticMetadataService();
    // should throw an IllegalArgumentException
    ms.getMetadata(mp);
  }

  private StaticMetadataServiceDublinCoreImpl newStaticMetadataService() throws Exception {
    StaticMetadataServiceDublinCoreImpl ms = new StaticMetadataServiceDublinCoreImpl();
    ms.setWorkspace(newWorkspace());
    return ms;
  }

  private Workspace newWorkspace() throws Exception {
    // mock workspace
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    InputStream dcFile = getClass().getResourceAsStream("/dublincore.xml");
    InputStream dcFileDefect = getClass().getResourceAsStream("/dublincore-defect.xml");
    assertNotNull(dcFile);
    // set expectations
    EasyMock.expect(workspace.read(EasyMock.anyObject())).andAnswer(
            () -> EasyMock.getCurrentArguments()[0].toString().contains("-defect") ? dcFileDefect : dcFile).anyTimes();
    // put into replay mode
    EasyMock.replay(workspace);
    return workspace;
  }

  private MediaPackage newMediaPackage(String manifest) throws Exception {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = getClass().getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));
    try (InputStream is = getClass().getResourceAsStream(manifest)) {
      return mediaPackageBuilder.loadFromXml(is);
    }
  }
}
