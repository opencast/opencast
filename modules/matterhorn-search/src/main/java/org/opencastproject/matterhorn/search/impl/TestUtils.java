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

package org.opencastproject.matterhorn.search.impl;

/**
 * Utility class containing a few helper methods.
 */
public final class TestUtils {

  /** Name of the property to indicate an ongoing unit or integration test */
  private static final String TEST_PROPERTY = "matterhorn.test";

  /**
   * This utility class is not intended to be instantiated.
   */
  private TestUtils() {
    // Nothing to do
  }

  /**
   * Enables testing by setting a system property. This method is used to add test specific code to production
   * implementations while using a consistent methodology to determine testing status.
   * <p>
   * Use {@link #isTest()} to determine whether testing has been turned on.
   */
  public static void startTesting() {
    System.setProperty(TEST_PROPERTY, Boolean.TRUE.toString());
  }

  /**
   * Returns <code>true</code> if a test is currently going on.
   * 
   * @return <code>true</code> if the current code is being executed as a test
   */
  public static boolean isTest() {
    return "true".equalsIgnoreCase(System.getProperty(TEST_PROPERTY));
  }

}
