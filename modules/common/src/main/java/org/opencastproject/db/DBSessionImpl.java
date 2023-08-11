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

package org.opencastproject.db;

import org.opencastproject.util.function.ThrowingConsumer;
import org.opencastproject.util.function.ThrowingFunction;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

public class DBSessionImpl implements DBSession {
  private static final Random RAND = new Random();

  private EntityManagerFactory emf;
  private int maxTransactionRetries = DBSessionFactoryImpl.DEFAULT_MAX_TRANSACTION_RETRIES;

  private final ThreadLocal<EntityManager> entityManagerStore = new ThreadLocal<>();

  public DBSessionImpl(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Override
  public void exec(Consumer<EntityManager> fn) {
    exec(em -> {
      fn.accept(em);
      return null;
    });
  }

  @Override
  public <E extends Throwable> void execChecked(ThrowingConsumer<EntityManager, E> fn) throws E {
    execChecked(em -> {
      fn.accept(em);
      return null;
    });
  }

  @Override
  public <T> T exec(Function<EntityManager, T> fn) {
    try {
      return execChecked(fn::apply);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T, E extends Throwable> T execChecked(ThrowingFunction<EntityManager, T, E> fn) throws E {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return fn.apply(em);
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  @Override
  public void execTx(Consumer<EntityManager> fn) {
    execTx(maxTransactionRetries, fn);
  }

  @Override
  public <E extends Throwable> void execTxChecked(ThrowingConsumer<EntityManager, E> fn) throws E {
    execTxChecked(maxTransactionRetries, fn);
  }

  @Override
  public void execTx(int maxTransactionRetries, Consumer<EntityManager> fn) {
    execTx(maxTransactionRetries, em -> {
      fn.accept(em);
      return null;
    });
  }

  @Override
  public <E extends Throwable> void execTxChecked(int maxTransactionRetries, ThrowingConsumer<EntityManager, E> fn) throws E {
    execTxChecked(maxTransactionRetries, em -> {
      fn.accept(em);
      return null;
    });
  }

  @Override
  public <T> T execTx(Function<EntityManager, T> fn) {
    return execTx(maxTransactionRetries, fn);
  }

  @Override
  public <T, E extends Throwable> T execTxChecked(ThrowingFunction<EntityManager, T, E> fn) throws E {
    return execTxChecked(maxTransactionRetries, fn);
  }

  @Override
  public <T> T execTx(int maxTransactionRetries, Function<EntityManager, T> fn) {
    try {
      return execTxChecked(maxTransactionRetries, fn::apply);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T, E extends Throwable> T execTxChecked(int maxTransactionRetries, ThrowingFunction<EntityManager, T, E> fn) throws E {
    EntityManager em = entityManagerStore.get();

    if (em != null) {
      // We are already in a transaction. Opening another one can lead to deadlocks.
      return fn.apply(em);
    }

    EntityTransaction tx = null;
    RuntimeException ex = null;

    for (int attempt = 0; attempt < maxTransactionRetries; attempt++) {
      try {
        em = emf.createEntityManager();
        entityManagerStore.set(em);
        tx = em.getTransaction();
        tx.begin();
        T res = fn.apply(em);
        tx.commit();
        return res;
      } catch (RuntimeException e) { // we only catch RuntimeException as other exceptions are not related to DB errors
        // TODO: do we need to catch all exceptions and look at the cause chain?
        ex = e;

        if (tx != null && tx.isActive()) {
          tx.rollback();
        }

        // only retry if exception has something to do with the transaction
        if (!DBUtils.isTransactionException(e)) {
          throw e;
        }
      } finally {
        if (em != null && em.isOpen()) {
          em.close();
        }
        entityManagerStore.remove();
      }

      // exponential backoff before next iteration
      int sleepMillis = (int) (Math.pow(2, attempt) * 100) + RAND.nextInt(100);
      try {
        Thread.sleep(sleepMillis);
      } catch (InterruptedException ignore) {
      }
    }

    // we only get here if all retries led to an exception: throw the last one up the stack
    throw ex;
  }

  @Override
  public void close() {
    if (emf.isOpen()) {
      emf.close();
    }
  }

  public EntityManagerFactory getEntityManagerFactory() {
    return emf;
  }

  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  public int getMaxTransactionRetries() {
    return maxTransactionRetries;
  }

  public void setMaxTransactionRetries(int maxTransactionRetries) {
    this.maxTransactionRetries = maxTransactionRetries;
  }
}
