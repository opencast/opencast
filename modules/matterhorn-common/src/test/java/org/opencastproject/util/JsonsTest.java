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

package org.opencastproject.util;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opencastproject.util.Jsons.NULL;
import static org.opencastproject.util.Jsons.ZERO_ARR;
import static org.opencastproject.util.Jsons.ZERO_OBJ;
import static org.opencastproject.util.Jsons.ZERO_VAL;
import static org.opencastproject.util.Jsons.append;
import static org.opencastproject.util.Jsons.arr;
import static org.opencastproject.util.Jsons.obj;
import static org.opencastproject.util.Jsons.p;
import static org.opencastproject.util.Jsons.toJson;
import static org.opencastproject.util.Jsons.v;

import com.jayway.restassured.path.json.JsonPath;

import org.junit.Test;

import java.util.List;
import java.util.Map;

public class JsonsTest {
  @Test
  public void testComposition1() {
    final Jsons.Obj j = append(obj(p("name", "Karl"), p("city", "Paris"), p("remove_me", ZERO_VAL)), obj(p("age", 79)));
    final JsonPath p = JsonPath.from(toJson(j));
    System.out.println(p.prettyPrint());
    assertEquals("Karl", p.get("name"));
    assertEquals("Paris", p.get("city"));
    assertNull(p.get("remove_me"));
    assertEquals(79, p.get("age"));
  }

  @Test
  public void testComposition2() {
    final Jsons.Obj x = obj(p("name", "Karl"), p("city", "Paris"));
    final Jsons.Obj y = obj(p("name", "Peter"));
    final JsonPath p = JsonPath.from(toJson(append(x, y)));
    assertEquals("Peter", p.get("name"));
    assertEquals("Paris", p.get("city"));
  }

  @Test
  public void testComposition3() {
    final Jsons.Obj j = obj(p("person", obj(p("name", "Karl"), p("city", "Paris"), p("age", 79))));
    final JsonPath p = JsonPath.from(toJson(j));
    assertEquals(79, p.get("person.age"));
    assertEquals("Karl", p.get("person.name"));
    assertEquals("Paris", p.get("person.city"));
  }

  @Test
  public void testComposition4() {
    final Jsons.Arr j = append(arr(v("hallo"), ZERO_VAL, v("hello")), arr(v("hola")));
    final JsonPath p = JsonPath.from(toJson(j));
    assertThat(p.<String> getList(""), contains("hallo", "hello", "hola"));
  }

  @Test
  public void testZeros() {
    assertNull(JsonPath.from(obj(p("val", ZERO_VAL)).toJson()).get("UNKNOWN"));
    // using ZERO_VAL in properties drops the whole property
    System.out.println(obj(p("val", ZERO_VAL)).toJson());
    assertNull(JsonPath.from(obj(p("val", ZERO_VAL)).toJson()).get("val"));
    {
      final Jsons.Obj j = obj(p("val", ZERO_OBJ));
      assertTrue(JsonPath.from(j.toJson()).get("val") instanceof Map);
      assertEquals(0, ((Map) JsonPath.from(j.toJson()).get("val")).size());
    }
    {
      final Jsons.Obj j = obj(p("val", ZERO_ARR));
      assertTrue(JsonPath.from(j.toJson()).get("val") instanceof List);
      assertEquals(0, ((List) JsonPath.from(j.toJson()).get("val")).size());
    }
  }

  @Test
  public void testNull() {
    System.out.println(obj(p("val", NULL)).toJson());
  }
}
