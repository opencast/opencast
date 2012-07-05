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

package org.opencastproject.util;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class FileSupportTest {
  
  private File fileToLink;
  private File linkLocation;
  private File fileSupportTestsDirectory;
  private File fileSupportTestsDestinationDirectory;

  @Before
  public void setUp() throws IOException {
    fileSupportTestsDirectory = new File(System.getProperty("java.io.tmpdir"), "fileSupportTestsDirectory");
    fileSupportTestsDestinationDirectory = new File(System.getProperty("java.io.tmpdir"),
            "fileSupportTestsDestinationDirectory");
    fileToLink = new File(fileSupportTestsDirectory.getAbsolutePath(), "file-to-link");
    linkLocation = new File(fileSupportTestsDirectory.getAbsolutePath(), "link-location");
    // Create test directory
    FileUtils.forceMkdir(fileSupportTestsDirectory);
    Assert.assertTrue("Can't read from test directory " + fileSupportTestsDirectory.getAbsolutePath(),
            fileSupportTestsDirectory.canRead());
    Assert.assertTrue("Can't write to test directory " + fileSupportTestsDirectory.getAbsolutePath(),
            fileSupportTestsDirectory.canWrite());
    // Create file that we could link. 
    FileUtils.touch(fileToLink);
    Assert.assertTrue("Can't read from file directory " + fileToLink.getAbsolutePath(),
            fileToLink.canRead());
  }
  
  @After
  public void tearDown() {
    FileUtils.deleteQuietly(fileSupportTestsDirectory);
    FileUtils.deleteQuietly(fileSupportTestsDestinationDirectory);
    fileToLink = null;
    linkLocation = null;
    fileSupportTestsDirectory = null;
    fileSupportTestsDestinationDirectory = null;
  }
  
  @Test
  public void supportsLinkingReturnsTrueOnAppropriateFile() {
    Assert.assertTrue(FileSupport.supportsLinking(fileToLink, linkLocation));
  }
  
  @Test
  public void supportsLinkingReturnsFalseOnMissingFile() {
    try {
      FileSupport.supportsLinking(linkLocation, fileToLink);
      Assert.fail();
    } catch (IllegalArgumentException e) {
      // If exception is thrown then this test has succeeded. 
    }
  }
  
  @Test
  public void linkContentTestWithoutForce() throws IOException {
    FileSupport.linkContent(fileSupportTestsDirectory, fileSupportTestsDestinationDirectory, false);
  }
  
  @Test
  public void linkContentTestWithForce() throws IOException {
    FileUtils.forceMkdir(fileSupportTestsDestinationDirectory);
    FileSupport.linkContent(fileSupportTestsDirectory, fileSupportTestsDestinationDirectory, true);
  }
  
  @Test
  public void linkTestWithoutForce() throws IOException {
    Assert.assertNotNull(FileSupport.link(fileToLink, linkLocation, false));
  }
  
  @Test
  public void linkTestWithForce() throws IOException {
    Assert.assertNotNull(FileSupport.link(fileToLink, linkLocation, true));
  }
  
  @Test
  public void missingLinkTestFailsWithoutForce() {
    try {
      Assert.assertNull(FileSupport.link(linkLocation, fileToLink, false));
      Assert.fail();
    } catch (IOException e) {
      // Test should have IOException. 
    }
  }
  
  @Test
  public void missingLinkTestFailsWithForce() {
    try {
      Assert.assertNull(FileSupport.link(linkLocation, fileToLink, true));
      Assert.fail();
    } catch (IOException e) {
      // Test should have IOException. 
    }
  }
}
