/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.migration;

import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlParsingException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SeriesMigrationServiceTest {

  /** The series acl name for 1.3 */
  private String seriesAcl13 = "/series_acl_13.xml";

  /** The series acl name for 1.4 */
  private String seriesAcl14 = "/series_acl_14.xml";

  /** The series acl file for 1.3 */
  private File seriesAcl13File = null;

  /** The series acl file for 1.4 */
  private File seriesAcl14File = null;

  private String seriesAcl13Content = null;
  private String seriesAcl14Content = null;

  /**
   * Test class for the series ACL migration service
   */
  private SeriesMigrationService seriesMigrationService = new SeriesMigrationService();

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    seriesAcl13File = new File(this.getClass().getResource(seriesAcl13).toURI());
    if (!seriesAcl13File.exists() || !seriesAcl13File.canRead())
      throw new Exception("Unable to access series acl test file '" + seriesAcl13 + "'");
    seriesAcl14File = new File(this.getClass().getResource(seriesAcl14).toURI());
    if (!seriesAcl13File.exists() || !seriesAcl13File.canRead())
      throw new Exception("Unable to access series acl test file '" + seriesAcl14 + "'");

    seriesAcl13Content = getFileContent(seriesAcl13File);
    seriesAcl14Content = getFileContent(seriesAcl14File);
  }

  public static String getFileContent(File file) throws IOException {
    FileInputStream inputStream = null;
    String fileContent = null;
    try {
      inputStream = new FileInputStream(file);
      fileContent = IOUtils.toString(inputStream);
    } catch (IOException e) {
      Assert.fail("Not able to get the content of the file " + file.getAbsolutePath());
    } finally {
      inputStream.close();
    }
    return fileContent;
  }

  @Test
  public void testConvertionTo14() {
    try {
      // the paseAcl for the matterhorn 1.3 acl should returns an empty ACL
      Assert.assertEquals(0, AccessControlParser.parseAcl(seriesAcl13Content).getEntries().size());

      AccessControlList acl = seriesMigrationService.parse13Acl(seriesAcl13Content);
      AccessControlList aclBase = AccessControlParser.parseAcl(seriesAcl14Content);

      Assert.assertEquals(3, acl.getEntries().size());

      // Test each entries to be sure that the parsed 1.3 series acl has been well converted
      int index = 0;
      for (AccessControlEntry entry : acl.getEntries()) {
        Assert.assertTrue(entry.equals(aclBase.getEntries().get(index++)));
      }
    } catch (AccessControlParsingException e) {
      Assert.fail("Not able to pase the ACL: " + e);
    } catch (IOException e) {
      Assert.fail("Parsing of ACL failed: " + e);
    }
  }

}
