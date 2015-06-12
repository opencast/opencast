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
import org.opencastproject.util.data.functions.Functions;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.util.data.functions.Functions.contra;
import static org.opencastproject.util.data.functions.Functions.identity;
import static org.opencastproject.util.data.functions.Functions.toPredicate;

public class FunctionsTest {
  public static final Function<String, Integer> length = new Function<String, Integer>() {
    @Override public Integer apply(String a) {
      return a.length();
    }
  };

  public static final Function0<String> hello = new Function0<String>() {
    @Override public String apply() {
      return "hello";
    }
  };

  public static final Function<Integer, String> toString = new Function<Integer, String>() {
    @Override public String apply(Integer a) {
      return a.toString();
    }
  };

  public static final Function0<Integer> yield1 = new Function0<Integer>() {
    @Override public Integer apply() {
      return 1;
    }
  };

  public static final Function2<Integer, Integer, Integer> subtract = new Function2<Integer, Integer, Integer>() {
    @Override public Integer apply(Integer a, Integer b) {
      return a - b;
    }
  };

  @Test
  public void testIdentity() {
    Integer one = 1;
    assertEquals(one, identity().apply(one));
    B b = new B();
    A a = Functions.<A>identity().apply(b);
    assertEquals(a, b);
  }

  @Test
  public void testVariance() {
    final Function<Number, String> f = new Function<Number, String>() {
      @Override public String apply(Number s) {
        return s.toString();
      }
    };
    final Function<Number, Object> f1 = Functions.<Number, Object>co(f);
    final Function<Double, String> f2 = contra(f);
    final Function<Number, Object> f3 = Functions.<Number, Object>variant(f);
    final Function<Double, Object> f4 = Functions.<Double, Object>variant(f);
    assertEquals(f.apply(1), f1.apply(1));
    assertEquals(f.apply(1d), f2.apply(1d));
  }

  @Test
  public void testThen() {
    assertEquals("5", Functions.then(length, toString).apply("hello"));
    assertEquals(integer(1), Functions.then(toString, length).apply(5));
    assertEquals("5", length.then(toString).apply("hello"));
    assertEquals(integer(1), toString.then(length).apply(5));
    assertEquals(integer(1), yield1.then(yield1).apply());
    assertEquals("1", yield1.then(toString).apply());
  }

  @Test
  public void testComposition() {
    assertEquals("5", toString.o(length).apply("hello"));
    assertEquals("1", toString.o(yield1).apply());
  }

  @Test
  public void testCurrying() {
    assertEquals("10", toString.curry().apply(10).apply());
    assertEquals("10", toString.curry(10).apply());
    assertEquals(integer(-20), subtract.curry().apply(10).apply(30));
    assertEquals(integer(20), subtract.curry().apply(30).apply(10));
    assertEquals(integer(20), subtract.curry(30).apply(10));
  }

  @Test
  public void testUncurrying() {
    assertEquals(integer(5), Functions.uncurry(subtract.curry()).apply(10, 5));
  }

  @Test
  public void testFlip() {
    assertEquals(integer(-20), subtract.flip().apply(30, 10));
  }

  class A {
  }

  class B extends A {
  }

  // Note that the checked exception escapes the function application even though it is _not_ declared!
  @Test(expected = IOException.class)
  public void testCheckedException() {
    new Function.X<String, String>() {
      @Override protected String xapply(String s) throws IOException {
        throw new IOException(s);
      }
    } .apply("error");
  }

  @Test
  public void testToPredicate() {
    Function<String, Boolean> p = toPredicate(new Function<String, Boolean>() {
      @Override public Boolean apply(String s) {
        return true;
      }
    });
  }

  /** {@link org.junit.Assert#assertEquals} complains when using unboxed integers. */
  private static Integer integer(int i) {
    return i;
  }
}
