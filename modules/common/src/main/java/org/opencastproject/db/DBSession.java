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

import org.opencastproject.util.function.ThrowingConsumer;
import org.opencastproject.util.function.ThrowingFunction;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;

public interface DBSession extends AutoCloseable {
  void exec(Consumer<EntityManager> fn);

  <E extends Throwable> void execChecked(ThrowingConsumer<EntityManager, E> fn) throws E;

  <T> T exec(Function<EntityManager, T> fn);

  <T, E extends Throwable> T execChecked(ThrowingFunction<EntityManager, T, E> fn) throws E;

  void execTx(Consumer<EntityManager> fn);

  <E extends Throwable> void execTxChecked(ThrowingConsumer<EntityManager, E> fn) throws E;

  void execTx(int maxTransactionRetries, Consumer<EntityManager> fn);

  <E extends Throwable> void execTxChecked(int maxTransactionRetries, ThrowingConsumer<EntityManager, E> fn) throws E;

  <T> T execTx(Function<EntityManager, T> fn);

  <T, E extends Throwable> T execTxChecked(ThrowingFunction<EntityManager, T, E> fn) throws E;

  <T> T execTx(int maxTransactionRetries, Function<EntityManager, T> fn);

  <T, E extends Throwable> T execTxChecked(int maxTransactionRetries, ThrowingFunction<EntityManager, T, E> fn) throws E;

  void close();
}
