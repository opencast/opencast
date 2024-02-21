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
package org.opencastproject.assetmanager.api.fn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.assetmanager.api.fn.Properties.mkProperty;

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyName;
import org.opencastproject.assetmanager.api.Value;

import com.entwinemedia.fn.data.Opt;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class PropertiesTest {
  static final Property pb1 = mkProperty("mp-1", "org.opencastproject.approval", "approved", Value.mk(false));
  static final Property ps1 = mkProperty("mp-1", "org.opencastproject.approval", "comment", Value.mk("Bad audio"));
  static final Property pd1 = mkProperty("mp-2", "org.opencastproject.approval", "date", Value.mk(new Date(0)));
  static final Property ps2 = mkProperty("mp-3", "org.opencastproject.comment", "comment", Value.mk("Hello world"));
  static final Property pl1 = mkProperty("mp-3", "org.opencastproject.comment", "count", Value.mk(1L));

  static final List<Property> ps = new ArrayList<>() {
    {
      add(pb1);
      add(ps1);
      add(pd1);
      add(ps2);
      add(pl1);
    }
  };

  @Test
  @Parameters({
          "unknown.namespace | 0",
          "org.opencastproject.approval | 3",
          "org.opencastproject.comment | 2"})
  public void testByNamespace(String namespace, int expectedCount) throws Exception {
    assertEquals(expectedCount, ps.stream().filter(p -> p.getId().getNamespace().equals(namespace)).count());
  }

  @Test
  @Parameters({
          "unknown.name | 0",
          "approved | 1",
          "date | 1",
          "comment | 2"})
  public void testByPropertyName(String propertyName, int expectedCount) throws Exception {
    assertEquals(expectedCount, ps.stream().filter(p -> p.getId().getName().equals(propertyName)).count());
  }

  @Test
  @Parameters({
          "unknown.name | approved | 0",
          "org.opencastproject.approval | approved | 1",
          "org.opencastproject.approval | date | 1",
          "org.opencastproject.approval | comment | 1"})

  public void testByFqnName(String namespace, String propertyName, int expectedCount) throws Exception {
    PropertyName pName = new PropertyName(namespace, propertyName);
    assertEquals(expectedCount, ps.stream()
            .filter(p -> p.getId().getNamespace().equals(pName.getNamespace())
                && p.getId().getName().equals(pName.getName()))
        .count()
    );
  }

  @Test
  @Parameters({
          "mp-x | 0",
          "mp-1 | 2",
          "mp-2 | 1",
          "mp-3 | 2"})
  public void testByMediaPackageId(String mpId, int expectedCount) throws Exception {
    assertEquals(expectedCount, ps.stream().filter(p -> p.getId().getMediaPackageId().equals(mpId)).count());
  }

  @Test
  public void testGetValue() throws Exception {
    List values = new ArrayList();
    values.add(Value.mk(false));
    values.add(Value.mk("Bad audio"));
    values.add(Value.mk(new Date(0)));
    values.add(Value.mk("Hello world"));
    values.add(Value.mk(1L));

    assertEquals(values, ps.stream().map(p -> p.getValue()).collect(Collectors.toList()));
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetValueWithExpectedTypeTypesDoNotMatch() throws Exception {
    ps.stream().map(p -> p.getValue().get(Value.STRING)).collect(Collectors.toList());
  }

  @Test
  public void testGetValueWithExpectedType() throws Exception {
    List values = new ArrayList();
    values.add("Bad audio");
    values.add("Hello world");

    assertEquals(values, ps.stream()
        .filter(p -> p.getId().getName().equals("comment"))
        .map(p -> p.getValue().get(Value.STRING))
        .collect(Collectors.toList())
    );
  }

  @Test
  public void testGetValueFold() throws Exception {
    assertEquals("Bad audio",
        ps.stream()
            .filter(p -> p.getId().getName().equals("comment"))
            .findFirst()
            .get()
            .getValue()
            .get(Value.STRING)
    );
    assertEquals(false,
        ps.stream()
            .filter(p -> p.getId().getName().equals("approved"))
            .findFirst()
            .get()
            .getValue()
            .get(Value.BOOLEAN)
    );
    assertEquals(new Date(0),
        ps.stream()
            .filter(p -> p.getId().getName().equals("date"))
            .findFirst()
            .get()
            .getValue()
            .get(Value.DATE)
    );
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetValueFoldNotFound() throws Exception {
    ps.stream()
        .filter(p -> p.getId().getName().equals("unknown"))
        .findFirst()
        .get()
        .getValue()
        .get(Value.STRING);
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetValueFoldTypeDoesNotMatch() throws Exception {
    ps.stream()
        .filter(p -> p.getId().getName().equals("comment"))
        .findFirst()
        .get()
        .getValue()
        .get(Value.BOOLEAN);
  }

  // TODO: Remove this?
  @Test
  public void testGetValueFoldOpt() throws Exception {
    assertEquals(Opt.some("Bad audio"),
        Opt.some(ps.stream()
            .filter(p -> p.getId().getName().equals("comment"))
            .findFirst()
            .get()
            .getValue()
            .get(Value.STRING))
    );
    assertEquals(Opt.some(false),
        Opt.some(ps.stream()
            .filter(p -> p.getId().getName().equals("approved"))
            .findFirst()
            .get()
            .getValue()
            .get(Value.BOOLEAN))
    );
    assertEquals(Opt.some(new Date(0)),
        Opt.some(ps.stream()
            .filter(p -> p.getId().getName().equals("date"))
            .findFirst()
            .get()
            .getValue()
            .get(Value.DATE))
    );
  }

  @Test
  public void testGetValueFoldOptNotFound() throws Exception {
    assertEquals(Opt.none(),
        Opt.nul(ps.stream()
            .filter(p -> p.getId().getName().equals("unknown"))
            .findFirst()
            .map(p -> p.getValue().get(Value.STRING))
            .orElse(null))
    );
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetValueFoldOptTypeDoesNotMatch() throws Exception {
    assertEquals(Opt.none(),
        Opt.nul(ps.stream()
            .filter(p -> p.getId().getName().equals("comment"))
            .findFirst()
            .map(p -> p.getValue().get(Value.BOOLEAN))
            .orElse(null))
    );
  }

  @Test
  public void testGetStrings() throws Exception {
    assertEquals(List.of("Bad audio", "Hello world"),
        ps.stream()
        .filter(p -> p.getId().getName().equals("comment"))
        .map(p -> p.getValue().get(Value.STRING))
        .collect(Collectors.toList())
    );
    PropertyName name = PropertyName.mk("org.opencastproject.comment", "comment");
    assertEquals(List.of("Hello world"),
        ps.stream()
            .filter(p -> p.getId().getName().equals(name.getName())
                && p.getId().getNamespace().equals(name.getNamespace()))
            .map(p -> p.getValue().get(Value.STRING))
            .collect(Collectors.toList())
    );
  }

  @Test
  public void testGetStringsNotFound() throws Exception {
    assertTrue(ps.stream()
        .filter(p -> p.getId().getName().equals("unknown"))
        .map(p -> p.getValue().get(Value.STRING))
        .collect(Collectors.toList())
        .isEmpty());
    PropertyName name = PropertyName.mk("org.opencastproject.approval", "unknown");
    assertTrue(ps.stream()
        .filter(p -> p.getId().getName().equals(name.getName()) && p.getId().getNamespace().equals(name.getNamespace()))
        .map(p -> p.getValue().get(Value.STRING))
        .collect(Collectors.toList())
        .isEmpty());
  }

  @Test
  public void testGetBoolean() throws Exception {
    assertEquals(false,
        ps.stream()
        .filter(p -> p.getId().getName().equals("approved"))
        .findFirst()
        .map(p -> p.getValue().get(Value.BOOLEAN))
        .orElseThrow(() -> {
          throw new RuntimeException();
        })
    );
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetBooleanNotFound() throws Exception {
    ps.stream()
        .filter(p -> p.getId().getName().equals("unknown"))
        .findFirst()
        .map(p -> p.getValue().get(Value.BOOLEAN))
        .orElseThrow(() -> {
          throw new RuntimeException();
        });
  }

  @Test
  public void testGetString() throws Exception {
    assertEquals("Bad audio",
        ps.stream()
            .filter(p -> p.getId().getName().equals("comment"))
            .findFirst()
            .map(p -> p.getValue().get(Value.STRING))
            .orElseThrow(() -> {
              throw new RuntimeException();
            })
    );
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetStringNotFound() throws Exception {
    ps.stream()
        .filter(p -> p.getId().getName().equals("unknown"))
        .findFirst()
        .map(p -> p.getValue().get(Value.STRING))
        .orElseThrow(() -> {
          throw new RuntimeException();
        });
  }

  @Test
  public void testGetDate() throws Exception {
    assertEquals(new Date(0),
        ps.stream()
            .filter(p -> p.getId().getName().equals("date"))
            .findFirst()
            .map(p -> p.getValue().get(Value.DATE))
            .orElseThrow(() -> {
              throw new RuntimeException();
            })
    );
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetDateNotFound() throws Exception {
    ps.stream()
        .filter(p -> p.getId().getName().equals("unknown"))
        .findFirst()
        .map(p -> p.getValue().get(Value.DATE))
        .orElseThrow(() -> {
          throw new RuntimeException();
        });
  }

  @Test
  public void testGetLong() throws Exception {
    assertEquals((Long) 1L,
        ps.stream()
            .filter(p -> p.getId().getName().equals("count"))
            .findFirst()
            .map(p -> p.getValue().get(Value.LONG))
            .orElseThrow(() -> {
              throw new RuntimeException();
            })
    );
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetNotFoundLong() throws Exception {
    ps.stream()
        .filter(p -> p.getId().getName().equals("unknown"))
        .findFirst()
        .map(p -> p.getValue().get(Value.LONG))
        .orElseThrow(() -> {
          throw new RuntimeException();
        });
  }

  @Test
  public void testGetStringOpt() throws Exception {
    assertEquals(Opt.some("Bad audio"),
        Opt.some(ps.stream()
            .filter(p -> p.getId().getName().equals("comment"))
            .findFirst()
            .map(p -> p.getValue().get(Value.STRING))
            .orElse(null))
    );
    assertEquals(Opt.none(),
        Opt.nul(ps.stream()
            .filter(p -> p.getId().getName().equals("unknown"))
            .findFirst()
            .map(p -> p.getValue().get(Value.STRING))
            .orElse(null))
    );
  }

  @Test
  public void testGetDateOpt() throws Exception {
    assertEquals(Opt.some(new Date(0)),
        Opt.some(ps.stream()
            .filter(p -> p.getId().getName().equals("date"))
            .findFirst()
            .map(p -> p.getValue().get(Value.DATE))
            .orElse(null))
    );
    assertEquals(Opt.none(),
        Opt.nul(ps.stream()
            .filter(p -> p.getId().getName().equals("unknown"))
            .findFirst()
            .map(p -> p.getValue().get(Value.DATE))
            .orElse(null))
    );
  }

  @Test
  public void testGetLongOpt() throws Exception {
    assertEquals(Opt.some(1L),
        Opt.some(ps.stream()
            .filter(p -> p.getId().getName().equals("count"))
            .findFirst()
            .map(p -> p.getValue().get(Value.LONG))
            .orElse(null))
    );
    assertEquals(Opt.none(),
        Opt.nul(ps.stream()
            .filter(p -> p.getId().getName().equals("unknown"))
            .findFirst()
            .map(p -> p.getValue().get(Value.LONG))
            .orElse(null))
    );
  }
}
