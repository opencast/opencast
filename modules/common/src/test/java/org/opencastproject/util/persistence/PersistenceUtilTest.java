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

package org.opencastproject.util.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.persistence.PersistenceEnvs.testPersistenceEnv;

import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;

public class PersistenceUtilTest {
  private PersistenceEnv penv;

  @Before
  public void before() {
    penv = testPersistenceEnv("test");
  }

  @After
  public void after() {
    penv.close();
  }

  @Test
  public void testPersistAndFind() {
    final long id = penv.tx(Queries.persist(TestDto.create("key", "value"))).getId();
    assertEquals("value", penv.tx(Queries.find(TestDto.class, id)).get().getValue());
  }

  @Test
  public void testPersistOrUpdateUpdate() {
    assertTrue(penv.tx(TestDto.findAll).isEmpty());
    final TestDto dto = penv.tx(Queries.persist(TestDto.create("key", "value")));
    assertEquals("value", penv.tx(Queries.find(TestDto.class, dto.getId())).get().getValue());
    dto.setValue("new-value");
    penv.tx(Queries.persistOrUpdate(dto));
    assertEquals("new-value", penv.tx(Queries.find(TestDto.class, dto.getId())).get().getValue());
  }

  @Test
  public void testPersistOrUpdatePersist() {
    assertTrue(penv.tx(TestDto.findAll).isEmpty());
    final TestDto dto = penv.tx(Queries.persistOrUpdate(TestDto.create("key", "value")));
    assertEquals("value", penv.tx(Queries.find(TestDto.class, dto.getId())).get().getValue());
    dto.setValue("new-value");
    penv.tx(Queries.persistOrUpdate(dto));
    assertEquals("new-value", penv.tx(Queries.find(TestDto.class, dto.getId())).get().getValue());
  }

  @Test
  public void testCloseEntityManager() {
    penv.tx(new Function<EntityManager, Object>() {
      @Override public Object apply(EntityManager entityManager) {
        // this should not throw an exception in penv.tx()
        entityManager.close();
        return null;
      }
    });
  }

  @Test(expected = RuntimeException.class)
  public void testException() {
    penv.tx(new Function<EntityManager, Object>() {
      @Override public Object apply(EntityManager entityManager) {
        throw new RuntimeException("error");
      }
    });
  }

  @Test(expected = IllegalStateException.class)
  public void testExceptionTransformation1() {
    penv.tx().rethrow(new Function<Exception, Exception>() {
      @Override public Exception apply(Exception e) {
        return new IllegalStateException(e);
      }
    }).apply(new Function<EntityManager, Object>() {
      @Override public Object apply(EntityManager entityManager) {
        throw new RuntimeException("error");
      }
    });
  }

  @Test
  public void testExceptionTransformation2() {
    final boolean[] exception = {false};
    penv.<Void>tx().handle(new Effect<Exception>() {
      @Override public void run(Exception e) {
        exception[0] = true;
      }
    }).apply(new Effect<EntityManager>() {
      @Override protected void run(EntityManager entityManager) {
        throw new RuntimeException("error");
      }
    });
    assertTrue(exception[0]);
  }

  @Test
  public void testExceptionTransformation3() {
    final Option<String> r = penv.<Option<String>>tx().handle(new Function<Exception, Option<String>>() {
      @Override public Option<String> apply(Exception e) {
        return none();
      }
    }).apply(new Function<EntityManager, Option<String>>() {
      @Override public Option<String> apply(EntityManager entityManager) {
        throw new RuntimeException("error");
      }
    });
    assertTrue(r.isNone());
  }

  @Test
  public void testExceptionTransformation4() {
    final Either<String, Integer> r = penv.<Integer>tx().either(new Function<Exception, String>() {
      @Override public String apply(Exception e) {
        return "error";
      }
    }).apply(new Function<EntityManager, Integer>() {
      @Override public Integer apply(EntityManager entityManager) {
        throw new RuntimeException("error");
      }
    });
    assertEquals("error", r.left().value());
  }

  @Test
  public void testTransactionPropagation() {
    long id = penv.tx(new Function<EntityManager, Long>() {
      @Override public Long apply(EntityManager em) {
        final TestDto dto = TestDto.create("key", "A");
        em.persist(dto);
        // nested transaction. if transaction propagation would fail a duplicate key exception would be thrown
        return save(dto);
      }
    });
    assertEquals("dto value", "B", penv.tx(Queries.find(TestDto.class, id)).get().getValue());
  }

  private long save(TestDto dto) {
    dto.setValue("B");
    return penv.tx(Queries.persist(dto)).getId();
  }
}
