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
package org.opencastproject.index.service.message;

import com.entwinemedia.fn.Fn;
import com.google.common.util.concurrent.Striped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;

public class MessageReceiverLockService {

  private static final Logger logger = LoggerFactory.getLogger(MessageReceiverLockService.class);

  private final Striped<Lock> lock = Striped.lazyWeakLock(1024);

  public <K, A> A synchronize(K resource, Fn<K, A> function) {
    final Lock lock = this.lock.get(resource);
    lock.lock();
    logger.debug("Locked resource '{}'", resource);
    try {
      return function.apply(resource);
    } finally {
      lock.unlock();
      logger.debug("Released locked resource '{}'", resource);
    }
  }

}
