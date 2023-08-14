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

package org.opencastproject.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.db.DBTestEnv.newDBSession;
import static org.opencastproject.db.Queries.namedQuery;

import org.eclipse.persistence.exceptions.DatabaseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.RollbackException;

public class DBSessionTest {
  private DBSession db;

  @Before
  public void before() {
    db = newDBSession("test");
  }

  @After
  public void after() {
    db.close();
  }

  @Test
  public void testPersistAndFind() {
    final long id = db.execTx(namedQuery.persist(TestDto.create("key", "value"))).getId();
    assertEquals("value", db.execTx(namedQuery.findById(TestDto.class, id)).getValue());
  }

  @Test
  public void testPersistOrUpdateUpdate() {
    assertTrue(db.execTx(TestDto.findAll).isEmpty());
    final TestDto dto = db.execTx(namedQuery.persist(TestDto.create("key", "value")));
    assertEquals("value", db.execTx(namedQuery.findById(TestDto.class, dto.getId())).getValue());
    dto.setValue("new-value");
    db.execTx(namedQuery.persistOrUpdate(dto));
    assertEquals("new-value", db.execTx(namedQuery.findById(TestDto.class, dto.getId())).getValue());
  }

  @Test
  public void testPersistOrUpdatePersist() {
    assertTrue(db.execTx(TestDto.findAll).isEmpty());
    final TestDto dto = db.execTx(namedQuery.persistOrUpdate(TestDto.create("key", "value")));
    assertEquals("value", db.execTx(namedQuery.findById(TestDto.class, dto.getId())).getValue());
    dto.setValue("new-value");
    db.execTx(namedQuery.persistOrUpdate(dto));
    assertEquals("new-value", db.execTx(namedQuery.findById(TestDto.class, dto.getId())).getValue());
  }

  @Test
  public void testCloseEntityManager() {
    // this should not throw an exception in db.execTx()
    db.execTx(EntityManager::close);
  }

  @Test(expected = RuntimeException.class)
  public void testException() {
    db.execTx((Consumer<EntityManager>) em -> {
      throw new RuntimeException("error");
    });
  }

  @Test
  public void testRetries() {
    AtomicInteger calls = new AtomicInteger();

    calls.set(0);
    db.execTx(em -> {
      calls.incrementAndGet();
      if (calls.get() == 1) {
        throw new RollbackException("error");
      }
    });
    assertEquals(2, calls.get());

    calls.set(0);
    db.execTx(em -> {
      calls.incrementAndGet();
      if (calls.get() == 1) {
        SQLException sqlEx = new SQLException("error", SqlState.TRANSACTION_ROLLBACK_SERIALIZATION_FAILURE);
        throw DatabaseException.sqlException(sqlEx);
      }
    });
    assertEquals(2, calls.get());
  }

  @Test
  public void testTransactionPropagation() {
    long id = db.execTx(em -> {
      final TestDto dto = TestDto.create("key", "A");
      em.persist(dto);
      // nested transaction. if transaction propagation would fail a duplicate key exception would be thrown
      return save(dto);
    });
    assertEquals("dto value", "B", db.execTx(namedQuery.findById(TestDto.class, id)).getValue());
  }

  private long save(TestDto dto) {
    dto.setValue("B");
    return db.execTx(namedQuery.persist(dto)).getId();
  }
}
