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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.mediapackage.MediaPackageElementFlavor.flavor;

public class MediaPackageElementFlavorTest {
  @Test
  public void testMatches() throws Exception {
    assertFalse("null does not match", flavor("a", "b").matches(null));
    assertTrue("Equal flavors match", flavor("a", "b").matches(flavor("a", "b")));
    assertFalse("Unequal flavors do not match", flavor("a", "b").matches(flavor("b", "a")));
    assertTrue("Wildcard type matches", flavor("*", "b").matches(flavor("a", "b")));
    assertTrue("Match is commutative",
               flavor("a", "b").matches(flavor("*", "b")) && flavor("*", "b").matches(flavor("a", "b")));
    assertTrue("Wildcard subtype matches", flavor("a", "*").matches(flavor("a", "b")));
    assertTrue("Match is commutative",
               flavor("a", "b").matches(flavor("a", "*")) && flavor("a", "*").matches(flavor("a", "b")));
    assertTrue("Wildcard matches", flavor("*", "*").matches(flavor("a", "b")));
    assertTrue("Match is commutative",
               flavor("*", "*").matches(flavor("a", "b")) && flavor("a", "b").matches(flavor("*", "*")));
    assertTrue("Wildcard matches", flavor("*", "*").matches(flavor("*", "b")));
    assertTrue("Match is commutative",
               flavor("*", "*").matches(flavor("*", "b")) && flavor("*", "b").matches(flavor("*", "*")));
    assertTrue("Wildcard matches", flavor("*", "*").matches(flavor("a", "*")));
    assertTrue("Match is commutative",
               flavor("*", "*").matches(flavor("a", "*")) && flavor("a", "*").matches(flavor("*", "*")));
    assertTrue("Wildcard matches", flavor("*", "*").matches(flavor("*", "*")));
  }
}

