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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.opencastproject.util.IoSupport;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ENameTest {
  @Test
  public void testEquals() throws Exception {
    final EName a = new EName("http://localhost/a", "a");
    final EName b = new EName("http://localhost/a", "a");
    final EName c = new EName("http://localhost/b", "a");
    final EName d = new EName("http://localhost/a", "b");
    assertEquals(a, a);
    assertEquals(a, b);
    assertEquals(b, a);
    assertNotEquals(a, c);
    assertNotEquals(c, a);
    assertNotEquals(c, d);
    assertNotEquals(d, c);
  }

  @Test
  public void testSerializability() {
    final EName a = new EName("http://localhost/a", "a");
    assertEquals(a, IoSupport.serializeDeserialize(a));
  }

  @Test
  public void testCompareTo() {
    final EName a1 = new EName("http://localhost/a", "a");
    final EName b1 = new EName("http://localhost/b", "b");
    final EName a2 = new EName("", "a");
    final EName b2 = new EName("", "b");

    List<EName> eNames = new ArrayList<>();
    eNames.add(b1);
    eNames.add(a1);
    eNames.add(b2);
    eNames.add(a2);
    Collections.sort(eNames);

    assertEquals(a2, eNames.get(0));
    assertEquals(b2, eNames.get(1));
    assertEquals(a1, eNames.get(2));
    assertEquals(b1, eNames.get(3));
  }

  @Test
  public void testFromString() {
    final String[] invalids = {
            "",
            "{", "}",
            "{}",
            "{http://localhost/a}",
            "invalid{name",
            "invalid}name",
            "invalid name",
            "\t invalid\nname",
            "{invalid namespace}correct-name",
            "{ invalidnamespace}correct-name",
            "{invalid{namespace}correct-name",
            "{invalid name{space}incorrect\t name"
            };
    for (String invalid : invalids) {
      try {
        EName.fromString(invalid);
      } catch (IllegalArgumentException iae) {
        // This is fine
      }
    }

    final String[] valids = {
            "validname",
            "{}validwithemptyNS",
            "{http://localhost/a}valid-with-namespace"
    };
    final EName[] validENames = {
            new EName("", "validname"),
            new EName("", "validwithemptyNS"),
            new EName("http://localhost/a", "valid-with-namespace")
    };

    for (int i = 0; i < valids.length; i++) {
      assertEquals(validENames[i], EName.fromString(valids[i]));
    }
  }

@Test
  public void testFromStringDefault() {
    final String defaultNS = "http://default.na/mespace";
    final String[] strings = {
            "localname",
            "{}with-empty-namespace",
            "{http://myname.spa/ce}localname"
    };
    final EName[] eNames = {
            new EName(defaultNS, "localname"),
            new EName("", "with-empty-namespace"),
            new EName("http://myname.spa/ce", "localname")
    };

    for (int i = 0; i < strings.length; i++) {
      assertEquals(eNames[i], EName.fromString(strings[i], defaultNS));
    }
  }
}
