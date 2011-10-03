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

package org.opencastproject.mediapackage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.mediapackage.identifier.Handle;
import org.opencastproject.mediapackage.identifier.HandleBuilder;
import org.opencastproject.mediapackage.identifier.HandleBuilderFactory;
import org.opencastproject.mediapackage.identifier.HandleException;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.mediapackage.identifier.SerialIdBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Test case to make sure creation of handle is working as expected.
 */
public class HandleBuilderTest {

  /** The handle builder */
  private HandleBuilder handleBuilder = null;

  /** The handle url */
  private URL url = null;

  /** The handle naming authority */
  private static final String namingAuthority = "10.0000";

  /** List of created handles */
  private List<Handle> newHandles = new ArrayList<Handle>();

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    System.setProperty(IdBuilderFactory.PROPERTY_NAME, SerialIdBuilder.class.getName());
    handleBuilder = HandleBuilderFactory.newInstance().newHandleBuilder();
    assertNotNull(handleBuilder);
    url = new URL("http://www.opencastproject.org");
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    for (Handle h : newHandles) {
      try {
        handleBuilder.delete(h);
      } catch (HandleException e) {
        fail("Error deleting handle " + h + ": " + e.getMessage());
      }
    }
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.identifier.HandleBuilderImpl#createNew()} .
   */
  @Test
  public void testCreateNew() {
    Handle handle = null;
    try {
      handle = handleBuilder.createNew();
      newHandles.add(handle);
      assertNotNull(handle);
      assertNotNull(handle.getNamingAuthority());
      assertNotNull(handle.getLocalName());
      assertNotNull(handle.resolve());
    } catch (HandleException e) {
      fail("Error creating handle: " + e.getMessage());
    }
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.identifier.HandleBuilderImpl#createNew(java.net.URL)} .
   */
  @Test
  public void testCreateNewURL() {
    Handle handle = null;
    try {
      handle = handleBuilder.createNew(url);
      newHandles.add(handle);
      assertNotNull(handle);
      assertNotNull(handle.getNamingAuthority());
      assertNotNull(handle.getLocalName());
      assertNotNull(handle.resolve());
      assertEquals(url, handle.resolve());
    } catch (HandleException e) {
      fail("Error creating handle: " + e.getMessage());
    }
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.identifier.HandleBuilderImpl#fromString(java.lang.String)}
   * .
   */
  @Test
  public void testFromValueOK() {
    String[] testsOK = new String[] { namingAuthority + "/5636213123", namingAuthority + "/mnvmnmvxvx",
            "hdl://" + namingAuthority + "/mnvmnmvxvx", };
    for (String t : testsOK) {
      Handle handle = handleBuilder.fromString(t);
      assertNotNull(handle);
      assertEquals(namingAuthority, handle.getNamingAuthority());
      assertEquals(10, handle.getLocalName().length());
    }
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.identifier.HandleBuilderImpl#fromString(java.lang.String)}
   * .
   */
  @Test
  public void testFromValueFail() {
    String[] testsFail = new String[] { "10.12324/5636213123", "11.1221/mnvmnmvxvx", "101221/mnvmnmvxvx", "10.1221/",
            "hdl://10.12313/mnvmnmvxvx" };
    int failCount = testsFail.length;
    for (String t : testsFail) {
      try {
        handleBuilder.fromString(t);
      } catch (Exception e) {
        failCount -= 1;
      }
    }
    if (failCount != 0)
      fail(failCount + " not failed");
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.identifier.HandleBuilderImpl#update(org.opencastproject.mediapackage.identifier.Id, java.net.URL)}
   */
  @Test
  public void testUpdate() {
    try {
      Handle newHandle = handleBuilder.createNew(url);
      newHandles.add(newHandle);

      // Create new target and update
      URL newTarget = new URL("http://www.apple.com");
      boolean updated = handleBuilder.update(newHandle, newTarget);
      assertTrue(updated);

      // TODO: Our handle server at the moment does caching in the
      // webservice, so this test always fails, although resolving
      // is working properly
      // URL resolvedUrl = handleBuilder.resolve(newHandle);
      // assertEquals(newTarget, resolvedUrl);
    } catch (HandleException e) {
      fail("Error updating handle: " + e.getMessage());
    } catch (MalformedURLException e) {
      fail("Error creating new handle url: " + e.getMessage());
    }
  }

}
