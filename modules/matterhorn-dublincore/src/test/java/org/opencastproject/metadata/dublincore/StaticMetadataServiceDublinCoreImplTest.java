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


package org.opencastproject.metadata.dublincore;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.metadata.api.MetadataValue;
import org.opencastproject.metadata.api.MetadataValues;
import org.opencastproject.metadata.api.StaticMetadata;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Predicate;
import org.opencastproject.workspace.api.Workspace;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.metadata.dublincore.TestUtil.createDate;
import static org.opencastproject.util.data.Collections.find;

public final class StaticMetadataServiceDublinCoreImplTest {

  @Test
  public void testExtractMetadata() throws Exception {
    MediaPackage mp = newMediaPackage("/manifest-simple.xml");
    StaticMetadataServiceDublinCoreImpl ms = newStaticMetadataService();
    StaticMetadata md = ms.getMetadata(mp);
    assertEquals("Land and Vegetation: Key players on the Climate Scene",
                 find(md.getTitles(), new Predicate<MetadataValue<String>>() {
                   @Override
                   public Boolean apply(MetadataValue<String> v) {
                     return v.getLanguage().equals(MetadataValues.LANGUAGE_UNDEFINED);
                   }
                 }).map(new Function<MetadataValue<String>, String>() {
                   @Override
                   public String apply(MetadataValue<String> v) {
                     return v.getValue();
                   }
                 }).getOrElse(""));
    assertEquals(createDate(2007, 12, 5, 0, 0, 0), md.getCreated().get());
  }

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
    final File dcFile = new File(getClass().getResource("/dublincore.xml").toURI());
    final File dcFileDefect = new File(getClass().getResource("/dublincore-defect.xml").toURI());
    Assert.assertNotNull(dcFile);
    // set expectations
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      @Override
      public File answer() throws Throwable {
        return EasyMock.getCurrentArguments()[0].toString().contains("-defect") ? dcFileDefect : dcFile;
      }
    }).anyTimes();
    // put into replay mode
    EasyMock.replay(workspace);
    return workspace;
  }

  private MediaPackage newMediaPackage(String manifest) throws Exception {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = getClass().getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));
    InputStream is = null;
    try {
      is = getClass().getResourceAsStream(manifest);
      return mediaPackageBuilder.loadFromXml(is);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }
}
