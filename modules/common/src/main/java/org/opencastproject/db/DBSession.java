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

import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;

/**
 * DBSession implements common DB query execution handlers.
 */
public interface DBSession extends AutoCloseable {
  /**
   * Execute given function without opening a new transaction. It should be assumed that the EntityManager was newly
   * created.
   *
   * @param fn Function to execute.
   */
  void exec(Consumer<EntityManager> fn);

  /**
   * Execute given function that can throw a checked exception without opening a new transaction. It should be assumed
   * that the EntityManager was newly created.
   *
   * @param fn Function to execute.
   * @param <E> Exception type that could be thrown by fn.
   * @throws E Exception thrown by fn.
   */
  <E extends Throwable> void execChecked(ThrowingConsumer<EntityManager, E> fn) throws E;

  /**
   * Execute given function without opening a new transaction. It should be assumed that the EntityManager was newly
   * created. The return value of given function is returned.
   *
   * @param fn Function to execute.
   * @return Object fn has returned.
   * @param <T> Return type of fn.
   */
  <T> T exec(Function<EntityManager, T> fn);

  /**
   * Execute given function that can throw a checked exception without opening a new transaction. It should be assumed
   * that the EntityManager was newly created. The return value of given function is returned.
   *
   * @param fn Function to execute.
   * @return Object fn has returned.
   * @param <T> Return type of fn.
   * @param <E> Exception type that could be thrown by fn.
   * @throws E Exception thrown by fn.
   */
  <T, E extends Throwable> T execChecked(ThrowingFunction<EntityManager, T, E> fn) throws E;

  /**
   * Execute given function within a transaction. There can only be a single transaction per DBSession object. It should
   * be assumed that the EntityManager and transaction are reused if this method is executed again. Further, should be
   * assumed that fn could be executed multiple times if the transaction fails and should be retried.
   *
   * @param fn Function to execute.
   */
  void execTx(Consumer<EntityManager> fn);

  /**
   * Execute given function that can throw a checked exception within a transaction. There can only be a single
   * transaction per DBSession object. It should be assumed that the EntityManager and transaction are reused if this
   * method is executed again. Further, should be assumed that fn could be executed multiple times if the transaction
   * fails and should be retried.
   *
   * @param fn Function to execute.
   * @param <E> Exception type that could be thrown by fn.
   * @throws E Exception thrown by fn.
   */
  <E extends Throwable> void execTxChecked(ThrowingConsumer<EntityManager, E> fn) throws E;

  /**
   * Execute given function within a transaction. There can only be a single transaction per DBSession object. It should
   * be assumed that the EntityManager and transaction are reused if this method is executed again. Further, should be
   * assumed that fn could be executed multiple times if the transaction fails and should be retried.
   *
   * @param maxTransactionRetries Maximal number of times fn can be executed again in case of transaction errors.
   * @param fn Function to execute.
   */
  void execTx(int maxTransactionRetries, Consumer<EntityManager> fn);

  /**
   * Execute given function that can throw a checked exception within a transaction. There can only be a single
   * transaction per DBSession object. It should be assumed that the EntityManager and transaction are reused if this
   * method is executed again. Further, should be assumed that fn could be executed multiple times if the transaction
   * fails and should be retried.
   *
   * @param maxTransactionRetries Maximal number of times fn can be executed again in case of transaction errors.
   * @param fn Function to execute.
   * @param <E> Exception type that could be thrown by fn.
   * @throws E Exception thrown by fn.
   */
  <E extends Throwable> void execTxChecked(int maxTransactionRetries, ThrowingConsumer<EntityManager, E> fn) throws E;

  /**
   * Execute given function within a transaction. There can only be a single transaction per DBSession object. It should
   * be assumed that the EntityManager and transaction are reused if this method is executed again. Further, should be
   * assumed that fn could be executed multiple times if the transaction fails and should be retried. The return value
   * of given function is returned.
   *
   * @param fn Function to execute.
   * @return Object fn has returned.
   * @param <T> Return type of fn.
   */
  <T> T execTx(Function<EntityManager, T> fn);

  /**
   * Execute given function that can throw a checked exception within a transaction. There can only be a single
   * transaction per DBSession object. It should be assumed that the EntityManager and transaction are reused if this
   * method is executed again. Further, should be assumed that fn could be executed multiple times if the transaction
   * fails and should be retried. The return value of given function is returned.
   *
   * @param fn Function to execute.
   * @return Object fn has returned.
   * @param <T> Return type of fn.
   * @param <E> Exception type that could be thrown by fn.
   * @throws E Exception thrown by fn.
   */
  <T, E extends Throwable> T execTxChecked(ThrowingFunction<EntityManager, T, E> fn) throws E;

  /**
   * Execute given function within a transaction. There can only be a single transaction per DBSession object. It should
   * be assumed that the EntityManager and transaction are reused if this method is executed again. Further, should be
   * assumed that fn could be executed multiple times if the transaction fails and should be retried. The return value
   * of given function is returned.
   *
   * @param maxTransactionRetries Maximal number of times fn can be executed again in case of transaction errors.
   * @param fn Function to execute.
   * @return Object fn has returned.
   * @param <T> Return type of fn.
   */
  <T> T execTx(int maxTransactionRetries, Function<EntityManager, T> fn);

  /**
   * Execute given function that can throw a checked exception within a transaction. There can only be a single
   * transaction per DBSession object. It should be assumed that the EntityManager and transaction are reused if this
   * method is executed again. Further, should be assumed that fn could be executed multiple times if the transaction
   * fails and should be retried. The return value of given function is returned.
   *
   * @param maxTransactionRetries Maximal number of times fn can be executed again in case of transaction errors.
   * @param fn Function to execute.
   * @return Object fn has returned.
   * @param <T> Return type of fn.
   * @param <E> Exception type that could be thrown by fn.
   * @throws E Exception thrown by fn.
   */
  <T, E extends Throwable> T execTxChecked(int maxTransactionRetries, ThrowingFunction<EntityManager, T, E> fn) throws E;

  /**
   * Closes this DBSession and cleans up related objects.
   */
  void close();
}
