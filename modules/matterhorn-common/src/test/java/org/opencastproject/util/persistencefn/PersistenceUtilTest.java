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
package org.opencastproject.util.persistencefn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Unit;
import com.entwinemedia.fn.data.Opt;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;

public class PersistenceUtilTest {
  private PersistenceEnv penv;

  @Before
  public void before() {
    penv = PersistenceEnvs.mkTestEnv("test");
  }

  @After
  public void after() {
    penv.close();
  }

  @Test
  public void testPersistAndFind() {
    final long id = penv.tx(Queries.persist(TestDto.mk("key", "value"))).getId();
    assertEquals("value", penv.tx(Queries.find(TestDto.class, id)).get().getValue());
  }

  @Test
  public void testPersistOrUpdateUpdate() {
    assertTrue(penv.tx(TestDto.findAll).isEmpty());
    final TestDto dto = penv.tx(Queries.persist(TestDto.mk("key", "value")));
    assertEquals("value", penv.tx(Queries.find(TestDto.class, dto.getId())).get().getValue());
    dto.setValue("new-value");
    penv.tx(Queries.persistOrUpdate(dto));
    assertEquals("new-value", penv.tx(Queries.find(TestDto.class, dto.getId())).get().getValue());
  }

  @Test
  public void testPersistOrUpdatePersist() {
    assertTrue("The database should not contain any test entities",
               penv.tx(TestDto.findAll).isEmpty());
    final TestDto dto = penv.tx(Queries.persistOrUpdate(TestDto.mk("key", "value")));
    assertNotNull("The persisted entity should have an ID", penv.tx(Queries.getId(dto)));
    assertEquals("The persisted entity should be found by ID",
                 "value", penv.tx(Queries.find(TestDto.class, dto.getId())).get().getValue());
    dto.setValue("new-value");
    penv.tx(Queries.persistOrUpdate(dto));
    assertEquals("new-value", penv.tx(Queries.find(TestDto.class, dto.getId())).get().getValue());
  }

  @Test
  public void testPersistOrUpdate() {
    assertTrue("The database should not contain any test entities",
               penv.tx(TestDto.findAll).isEmpty());
    final TestDto dto = penv.tx(Queries.persistOrUpdate(TestDto.mk("key", "value")));
    assertNotNull("The persisted entity should have an ID", dto.getId());
    assertEquals("The generic ID getter should return the same value as the getId method on the entity",
                 dto.getId(), penv.tx(Queries.getId(dto)));
    assertEquals("The persisted entity should be found by ID",
                 "value", penv.tx(Queries.find(TestDto.class, dto.getId())).get().getValue());
    final long generatedId = dto.getId() + 1;
    assertTrue("The generated ID should not be used already", penv.tx(Queries.find(TestDto.class, generatedId)).isNone());
    penv.tx(Queries.persistOrUpdate(TestDto.mk(generatedId, "k", "v")));
    assertEquals("The entity with the generated ID should be found",
                 "v", penv.tx(Queries.find(TestDto.class, generatedId)).get().getValue());
  }

  @Test
  public void testCloseEntityManager() {
    penv.tx(new Fn<EntityManager, Object>() {
      @Override public Object apply(EntityManager entityManager) {
        // this should not throw an exception in penv.tx()
        entityManager.close();
        return null;
      }
    });
  }

  @Test(expected = RuntimeException.class)
  public void testException() {
    penv.tx(new Fn<EntityManager, Object>() {
      @Override public Object apply(EntityManager entityManager) {
        throw new RuntimeException("error");
      }
    });
  }

  @Test
  public void testTransactionPropagation() {
    long id = penv.tx(new Fn<EntityManager, Long>() {
      @Override public Long apply(EntityManager em) {
        final TestDto dto = TestDto.mk("key", "A");
        em.persist(dto);
        // nested transaction. if transaction propagation would fail a duplicate key exception would be thrown
        return save(dto);
      }
    });
    assertEquals("dto value", "B", penv.tx(Queries.find(TestDto.class, id)).get().getValue());
  }

  @Test
  public void testRemove() {
    final TestDto dto = TestDto.mk("key", "value");
    assertNull("New entity should not have an ID",
               dto.getId());
    final TestDto persistedDto = penv.tx(Queries.persist(dto));
    assertNotNull("Persisted entity should have an ID",
                  persistedDto.getId());
    assertEquals("Persisting an entity modifies it",
                 dto, persistedDto);
    final Opt<TestDto> fetchedDto = penv.tx(Queries.find(TestDto.class, dto.getId()));
    assertTrue("The entity should be found by ID",
               fetchedDto.isSome());
    assertNotEquals("The fetched entity should not be the same object as the persisted one",
                    dto, fetchedDto.get());
    assertFalse("The fetched entity should be in detached state", penv.tx(Queries.contains(fetchedDto.get())));
    assertEquals("The entity should be removed even though it is in detached state",
                 Unit.unit, penv.tx(Queries.remove(fetchedDto.get())));
    assertTrue("The entity should be removed now",
               penv.tx(Queries.find(TestDto.class, dto.getId())).isNone());
  }

  private long save(TestDto dto) {
    dto.setValue("B");
    return penv.tx(Queries.persist(dto)).getId();
  }
}
