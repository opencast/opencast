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

package org.opencastproject.util.data;

import org.junit.Test;
import org.opencastproject.util.data.functions.Functions;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.util.data.functions.Functions.contra;
import static org.opencastproject.util.data.functions.Functions.identity;
import static org.opencastproject.util.data.functions.Functions.toPredicate;

public class FunctionsTest {

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
}
