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

package org.opencastproject.mediapackage;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.mediapackage.MediaPackageElementFlavor.flavor;

import org.junit.Test;

public class MediaPackageElementFlavorTest {

  @Test
  public void testInvalidConstructorArguments() {

    String[][] inputArguments = new String[][] {
      new String[] { null, null },
      new String[] { "valid", null },
      new String[] { null, "valid" },
      new String[] { "", "" },
      new String[] { "valid", "" },
      new String[] { "", "valid" },
      new String[] { " \t \n \r ", " \t \n \r " },
      new String[] { " \t \n \r ", "" },
      new String[] { "", " \t \n \r " },
      new String[] { "valid", " \t \n \r " },
      new String[] { " \t \n \r ", "valid" },
      new String[] { "invalid/type", "valid_subtype" },
      new String[] { "valid_type", "invalid/subtype" }
    };

    for (String[] arguments : inputArguments) {
      try {
        new MediaPackageElementFlavor(arguments[0], arguments[1]);
      } catch (Throwable t) {
        assertEquals(format("Catched unexpected exception. The message was: %s", t.getMessage()),
                IllegalArgumentException.class, t.getClass());
        continue;
      }
      fail(format("Flavor constructor did not throw an expected exception with invalid arguments \"%s\" and \"%s\"",
              arguments[0], arguments[1]));
    }
  }

  @Test
  public void testEquals() {
    assertEquals("Leading or trailing whitespace in the flavor type should be trimmed",
            flavor(" \r \n \t type \t \n \r ", "subtype"), flavor("type", "subtype"));
    assertEquals("Leading or trailing whitespace in the flavor subtype should be ignored",
            flavor("type", " \r \n \t subtype \t \n \r "), flavor("type", "subtype"));
    assertEquals("Leading or trailing whitespace in both the flavor type and subtype should be trimmed",
            flavor(" \r \n \t type \t \n \r ", " \r \n \t subtype \t \n \r "), flavor("type", "subtype"));
    assertEquals("Flavor type should be case-insensitive", flavor("TYPE", "subtype"), flavor("type", "subtype"));
    assertEquals("Flavor type should be case-insensitive", flavor("type", "SUBTYPE"), flavor("type", "subtype"));
    assertEquals("Flavor parts should be case-insensitive", flavor("TYPE", "SUBTYPE"), flavor("type", "subtype"));
  }

  @Test
  public void testMatches() throws Exception {
    assertFalse("null should not match any flavor", flavor("a", "b").matches(null));
    assertTrue("Equal flavors should match", flavor("a", "b").matches(flavor("a", "b")));
    assertFalse("Unequal flavors should not match", flavor("a", "b").matches(flavor("b", "a")));
    assertTrue("Wildcard type did not match", flavor("*", "b").matches(flavor("a", "b")));
    assertTrue("Match with wildcard type should be commutative",
            flavor("a", "b").matches(flavor("*", "b")) && flavor("*", "b").matches(flavor("a", "b")));
    assertTrue("Wildcard subtype did not match", flavor("a", "*").matches(flavor("a", "b")));
    assertTrue("Match with wildcard subtype should be commutative",
            flavor("a", "b").matches(flavor("a", "*")) && flavor("a", "*").matches(flavor("a", "b")));
    assertTrue("Wildcard in both type and subtype did not match", flavor("*", "*").matches(flavor("a", "b")));
    assertTrue("Match with wildcards in both type and subtype should be commutative",
            flavor("*", "*").matches(flavor("a", "b")) && flavor("a", "b").matches(flavor("*", "*")));
    assertTrue("Wildcard matches", flavor("*", "*").matches(flavor("*", "b")));
    assertTrue("Match between wildcards should be commutative",
            flavor("*", "*").matches(flavor("*", "b")) && flavor("*", "b").matches(flavor("*", "*")));
    assertTrue("Wildcard matches", flavor("*", "*").matches(flavor("a", "*")));
    assertTrue("Match between wildcards should be commutative",
            flavor("*", "*").matches(flavor("a", "*")) && flavor("a", "*").matches(flavor("*", "*")));
    assertTrue("Wildcard matches", flavor("*", "*").matches(flavor("*", "*")));
  }

  @Test
  public void testParse() {

    String[] invalidFlavors = new String[] {
            null,
            "",
            "/",
            "valid/",
            "/valid",
            " \r \n \t /",
            " \r \n \t /valid",
            "/ \r \n \t ",
            "valid/ \r \n \t ",
            " \r \n \t / \r \n \t ",
            "too/many/slashes",
            "/ \r \n \t too \r / \r many   / \t \r \n slashes \t"
    };

    for (String invalid : invalidFlavors) {
      try {
        MediaPackageElementFlavor.parseFlavor(invalid);
      } catch (Throwable t) {
        assertEquals(format("Catched unexpected exception. The message was: %s", t.getMessage()),
                IllegalArgumentException.class, t.getClass());
        continue;
      }
      fail(format("Invalid flavor should fail but was parsed without errors: \"%s\"", invalid));
    }
  }
}
