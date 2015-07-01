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

package org.opencastproject.archive.opencast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opencastproject.util.ReflectionUtil.run;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.archive.api.Version;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.schema.OcDublinCore;
import org.opencastproject.schema.test.TestUtil;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.data.Option;

import org.junit.Test;

import java.util.Random;
import java.util.UUID;

public class JaxbResultItemTest {
  /** Ensure all fields of an OpencastResultItem are copied to its JAXB DTO. */
  @Test
  public void testCreate() throws Exception {
    final OpencastResultItem source = randomResultItem();
    final JaxbResultItem target = JaxbResultItem.create(source);
    run(OpencastResultItem.class, new OpencastResultItem() {
      @Override
      public OcDublinCore getDublinCore() {
        assertNotNull("dublin core copy", target.dublinCore);
        return null;
      }

      @Override
      public String getMediaPackageId() {
        assertEquals("media package id copy", source.getMediaPackageId(), target.id);
        return null;
      }

      @Override
      public MediaPackage getMediaPackage() {
        assertEquals("media package copy", source.getMediaPackage().getIdentifier(),
                target.mediaPackage.getIdentifier());
        return null;
      }

      @Override
      public Option<String> getSeriesId() {
        assertEquals("series id copy", source.getSeriesId().getOrElseNull(), target.seriesId);
        return null;
      }

      @Override
      public AccessControlList getAcl() {
        assertEquals("acl copy", source.getAcl(), target.acl);
        return null;
      }

      @Override
      public String getOrganizationId() {
        assertEquals("organization id copy", source.getOrganizationId(), target.organization);
        return null;
      }

      @Override
      public Version getVersion() {
        assertEquals("version copy", source.getVersion().value(), target.version);
        return null;
      }

      @Override
      public boolean isLatestVersion() {
        assertEquals("latest version copy", source.isLatestVersion(), target.latestVersion);
        return false;
      }

      @Override
      public Option<OcDublinCore> getSeriesDublinCore() {
        assertNotNull("series dublin core copy", target.dublinCore);
        return null;
      }
    });
  }

  public static OpencastResultItem randomResultItem() throws Exception {
    final OcDublinCore dublinCore = TestUtil.randomDc();
    final OcDublinCore seriesDublinCore = TestUtil.randomDc();
    final MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    final Option<String> seriesId = some(UUID.randomUUID().toString());
    final AccessControlList acl = new AccessControlList();
    final String organizationId = UUID.randomUUID().toString();
    final Version version = Version.version(new Random().nextLong());
    final boolean latestVersion = Math.random() < 0.5;
    return new OpencastResultItem() {
      @Override
      public OcDublinCore getDublinCore() {
        return dublinCore;
      }

      @Override
      public Option<OcDublinCore> getSeriesDublinCore() {
        return Option.some(seriesDublinCore);
      }

      @Override
      public String getMediaPackageId() {
        return mediaPackage.getIdentifier().toString();
      }

      @Override
      public MediaPackage getMediaPackage() {
        return mediaPackage;
      }

      @Override
      public Option<String> getSeriesId() {
        return seriesId;
      }

      @Override
      public AccessControlList getAcl() {
        return acl;
      }

      @Override
      public String getOrganizationId() {
        return organizationId;
      }

      @Override
      public Version getVersion() {
        return version;
      }

      @Override
      public boolean isLatestVersion() {
        return latestVersion;
      }

    };
  }
}
