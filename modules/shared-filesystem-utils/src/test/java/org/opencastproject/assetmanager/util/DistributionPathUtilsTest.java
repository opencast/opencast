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

package org.opencastproject.assetmanager.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class DistributionPathUtilsTest {

  private static final String DOWNLOAD_URL = "http://localhost:8080/static";
  private static final String ORGANIZATION = "mh_default_org";
  private static final String FILE_URL = DOWNLOAD_URL + "/" + ORGANIZATION + "/engage-player/mp1/element1/video.mp4";

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  private File downloadDirectory;
  private File distributedFile;

  @Before
  public void setUp() throws Exception {
    downloadDirectory = testFolder.newFolder("download");
    distributedFile = new File(downloadDirectory, ORGANIZATION + "/engage-player/mp1/element1/video.mp4");
    FileUtils.writeStringToFile(distributedFile, "video", StandardCharsets.UTF_8);
  }

  private File getLocalFile(String uri) {
    return DistributionPathUtils.getLocalFile(downloadDirectory.getAbsolutePath(), DOWNLOAD_URL, ORGANIZATION,
        URI.create(uri));
  }

  @Test
  public void testGetLocalFile() {
    assertEquals(distributedFile.getAbsoluteFile(), getLocalFile(FILE_URL).getAbsoluteFile());
  }

  // Stream security adds a query to the URLs, which is not part of the path of the file
  @Test
  public void testGetLocalFileIgnoresQuery() {
    String signed = FILE_URL + "?policy=eyJTdGF0ZW1lbnQiOnt9fQ&keyId=mh_default_org&signature=abc123";

    assertEquals(distributedFile.getAbsoluteFile(), getLocalFile(signed).getAbsoluteFile());
  }

  @Test
  public void testGetLocalFileForOtherUrlsOrMissingFiles() {
    assertNull(getLocalFile("http://example.com/static/mh_default_org/engage-player/mp1/element1/video.mp4"));
    assertNull(getLocalFile(DOWNLOAD_URL + "/" + ORGANIZATION + "/engage-player/mp1/element1/missing.mp4"));
    assertNull(getLocalFile(DOWNLOAD_URL + "/" + ORGANIZATION + "/engage-player/mp1/element1/missing.mp4?a=b"));
  }
}
