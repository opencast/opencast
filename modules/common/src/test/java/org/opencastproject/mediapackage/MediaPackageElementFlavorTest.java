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

package org.opencastproject.mediapackage;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencastproject.mediapackage.MediaPackageElementFlavor.flavor;

import org.junit.jupiter.api.Test;

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
        assertEquals(IllegalArgumentException.class,
                t.getClass(), format("Catched unexpected exception. The message was: %s", t.getMessage()));
        continue;
      }
      fail(format("Flavor constructor did not throw an expected exception with invalid arguments \"%s\" and \"%s\"",
              arguments[0], arguments[1]));
    }
  }

  @Test
  public void testEquals() {
    assertEquals(flavor(" \r \n \t type \t \n \r ", "subtype"),
            flavor("type", "subtype"), "Leading or trailing whitespace in the flavor type should be trimmed");
    assertEquals(flavor("type", " \r \n \t subtype \t \n \r "),
            flavor("type", "subtype"), "Leading or trailing whitespace in the flavor subtype should be ignored");
    assertEquals(flavor(" \r \n \t type \t \n \r ", " \r \n \t subtype \t \n \r "),
            flavor("type", "subtype"),
            "Leading or trailing whitespace in both the flavor type and subtype should be trimmed");
    assertEquals(flavor("TYPE", "subtype"), flavor("type", "subtype"), "Flavor type should be case-insensitive");
    assertEquals(flavor("type", "SUBTYPE"), flavor("type", "subtype"), "Flavor type should be case-insensitive");
    assertEquals(flavor("TYPE", "SUBTYPE"), flavor("type", "subtype"), "Flavor parts should be case-insensitive");
  }

  @Test
  public void testMatches() throws Exception {
    assertFalse(flavor("a", "b").matches(null), "null should not match any flavor");
    assertTrue(flavor("a", "b").matches(flavor("a", "b")), "Equal flavors should match");
    assertFalse(flavor("a", "b").matches(flavor("b", "a")), "Unequal flavors should not match");
    assertTrue(flavor("*", "b").matches(flavor("a", "b")), "Wildcard type did not match");
    assertTrue(flavor("a", "b").matches(flavor("*", "b")) && flavor("*", "b").matches(flavor("a", "b")),
            "Match with wildcard type should be commutative");
    assertTrue(flavor("a", "*").matches(flavor("a", "b")), "Wildcard subtype did not match");
    assertTrue(flavor("a", "b").matches(flavor("a", "*")) && flavor("a", "*").matches(flavor("a", "b")),
            "Match with wildcard subtype should be commutative");
    assertTrue(flavor("*", "*").matches(flavor("a", "b")), "Wildcard in both type and subtype did not match");
    assertTrue(flavor("*", "*").matches(flavor("a", "b")) && flavor("a", "b").matches(flavor("*", "*")),
            "Match with wildcards in both type and subtype should be commutative");
    assertTrue(flavor("*", "*").matches(flavor("*", "b")), "Wildcard matches");
    assertTrue(flavor("*", "*").matches(flavor("*", "b")) && flavor("*", "b").matches(flavor("*", "*")),
            "Match between wildcards should be commutative");
    assertTrue(flavor("*", "*").matches(flavor("a", "*")), "Wildcard matches");
    assertTrue(flavor("*", "*").matches(flavor("a", "*")) && flavor("a", "*").matches(flavor("*", "*")),
            "Match between wildcards should be commutative");
    assertTrue(flavor("*", "*").matches(flavor("*", "*")), "Wildcard matches");
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
        assertEquals(IllegalArgumentException.class,
                t.getClass(), format("Catched unexpected exception. The message was: %s", t.getMessage()));
        continue;
      }
      fail(format("Invalid flavor should fail but was parsed without errors: \"%s\"", invalid));
    }
  }
}
