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

package org.opencastproject.assetmanager.api.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.Version;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.function.Function;

/**
 * Test the {@link org.opencastproject.assetmanager.api.Value} type.
 * <p>
 * Not in the same package as {@link org.opencastproject.assetmanager.api.Value}
 * on purpose to showcase method visibility.
 */
public class ValueTest {
  @Test
  public void testGet() throws Exception {
    Assert.assertEquals("a value", Value.mk("a value").get(Value.STRING));
    assertThat(Value.mk(1511L).get(Value.LONG), Matchers.instanceOf(Long.class));
    assertEquals(Value.mk(10L).get(), new Long(10L));
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetTypesDoNotMatch() throws Exception {
    Value.mk(1511L).get(Value.STRING);
  }

  @Test
  public void testDecompose() {
    final Object value = Value.mk(1511L)
        .<Object>decompose(
            new Function<String, String>() {
              @Override public String apply(String a) {
                return a;
              }
            },
            new Function<Date, Date>() {
              @Override public Date apply(Date a) {
                return a;
              }
            },
            new Function<Long, Long>() {
              @Override public Long apply(Long a) {
                return a;
              }
            },
            new Function<Boolean, Boolean>() {
              @Override public Boolean apply(Boolean a) {
                return a;
              }
            },
            new Function<Version, Version>() {
              @Override public Version apply(Version a) {
                return a;
              }
            }
        );
    assertEquals(1511L, value);
    final String valueAsString = Value.mk(1511L).decompose(
            Value.<String>doNotMatch(),
            o(asString, new Function<Date, Date>() {
              @Override public Date apply(Date a) {
                return a;
              }
            }),
            o(asString, new Function<Long, Long>() {
              @Override public Long apply(Long a) {
                return a;
              }
            }),
            Value.<String>doNotMatch(),
            Value.<String>doNotMatch());
    assertEquals("1511", valueAsString);
  }

  @Test(expected = java.lang.Error.class)
  public void testDecomposeNoMatch() {
    Value.mk(new Date()).decompose(
            Value.doNotMatch(),
            Value.doNotMatch(),
            Value.doNotMatch(),
            Value.doNotMatch(),
            Value.doNotMatch());
  }

  @Test
  public void testMkGeneric() {
    assertEquals("sl", Value.mk(Value.STRING, "sl").get());
    new Value.StringValue("23");
  }

  @Test
  public void testEquality() {
    assertEquals(Value.mk(true), Value.mk(true));
    assertEquals(Value.mk(12L), Value.mk(12L));
    assertEquals(Value.mk("test"), Value.mk("test"));
    final Date now = new Date();
    assertEquals(Value.mk(now), Value.mk(now));
    assertNotEquals(Value.mk(true), Value.mk(false));
    assertNotEquals(Value.mk(11L), Value.mk(15L));
    assertNotEquals(Value.mk("test"), Value.mk("teest"));
    assertNotEquals(Value.mk(now), Value.mk(new Date(0)));
  }

  private static final Function<Object, String> asString = new Function<Object, String>() {
    @Override public String apply(Object o) {
      return o.toString();
    }
  };

  // Utility function copied from com.entwinemedia.fn.Fns
  public static <A, B, C> Function<A, C> o(
      final Function<? super B, ? extends C> f,
      final Function<? super A, ? extends B> g
  ) {
    return new Function<A, C>() {
      public C apply(A a) {
        return f.apply(g.apply(a));
      }
    };
  }
}
