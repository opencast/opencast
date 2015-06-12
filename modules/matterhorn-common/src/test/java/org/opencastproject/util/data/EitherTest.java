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


package org.opencastproject.util.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;

public class EitherTest {

  @Test
  public void testEither() {
    assertTrue(left("a").isLeft());
    assertTrue(right("a").isRight());
    Either<String, Integer> a = left("error");
    assertEquals("error", a.left().value());
    Either<String, Integer> b = right(1);
    assertEquals(new Integer(1), b.right().value());
  }

  @Test(expected = java.lang.Error.class)
  public void testProjection1() {
    left("a").right().value();
  }

  @Test(expected = java.lang.Error.class)
  public void testProjection2() {
    right("a").left().value();
  }

  @Test
  public void testProjection3() {
    assertEquals("x", right("a").left().getOrElse("x"));
    assertEquals("a", right("a").right().getOrElse("x"));
    assertEquals("x", left("a").right().getOrElse("x"));
    assertEquals("a", left("a").left().getOrElse("x"));
    Either<String, Integer> a = left("a");
    assertEquals("a", a.left().getOrElse("x"));
    assertEquals(new Integer(10), a.right().getOrElse(10));
  }

  @Test
  public void testFold() {
    Either<String, Integer> a = left("a");
    assertEquals("a", a.fold(new Either.Match<String, Integer, String>() {
      @Override
      public String left(String s) {
        return s;
      }

      @Override
      public String right(Integer integer) {
        return integer.toString();
      }
    }));
    Either<String, Integer> b = right(1);
    assertEquals("1", b.fold(new Either.Match<String, Integer, String>() {
      @Override
      public String left(String s) {
        return s;
      }

      @Override
      public String right(Integer integer) {
        return integer.toString();
      }
    }));
  }
}

