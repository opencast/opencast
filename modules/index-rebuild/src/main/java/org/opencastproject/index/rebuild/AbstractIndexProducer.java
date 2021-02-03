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

package org.opencastproject.index.rebuild;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of IndexProducer adds logging methods for convenience.
 */
public abstract class AbstractIndexProducer implements IndexProducer {

  private static final Logger logger = LoggerFactory.getLogger(AbstractIndexProducer.class);

  /**
   * Log the progress of the index rebuild for this service.
   *
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param total
   *           The total amount of elements to be re-added.
   * @param current
   *           The amount of elements that have already been re-added.
   */
  protected void logIndexRebuildProgress(String indexName, int total, int current) {
    logIndexRebuildProgress(indexName, total, current, 1);
  }

  /**
   * Log the progress of the index rebuild for this service.
   *
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param total
   *           The total amount of elements to be re-added.
   * @param current
   *           The amount of elements that have already been re-added.
   * @param batchSize
   *           The size of the batch we re-add in one go.
   */
  protected void logIndexRebuildProgress(String indexName, int total, int current, int batchSize) {
    final int responseInterval = (total < 100) ? 1 : (total / 100);
    if (responseInterval == 1 || batchSize > responseInterval || current == total
            || current % responseInterval < batchSize) {
      logger.info("Updating index {} for service '{}': {}/{} finished, {}% complete.", indexName, getService(), current,
              total, (current * 100 / total));
    }
    if (current == total) {
      logger.info("Waiting for service '{}' indexing to complete", getService().name());
    }
  }

  /**
   * Log an error during an index rebuild for this service.
   *
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param e
   *           The exception that occurred.
   */
  protected void logIndexRebuildError(String indexName, Exception e) {
    logger.error("Error updating index {} for service '{}'.", indexName, getService(), e);
  }

  /**
   * Log an error during an index rebuild for this service.
   *
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param total
   *           The total amount of elements to be re-added.
   * @param current
   *           The amount of elements that have already been re-added.
   * @param e
   *           The exception that occurred.
   */
  protected void logIndexRebuildError(String indexName, int total, int current, Exception e) {
    logger.error("Error updating index {} for service '{}' with {}/{} finished.", indexName, getService(), current,
            total, e);
  }
}
