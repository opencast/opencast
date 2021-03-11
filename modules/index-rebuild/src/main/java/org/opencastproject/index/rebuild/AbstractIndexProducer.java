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

import org.opencastproject.security.api.Organization;

import org.slf4j.Logger;

/**
 * This implementation of IndexProducer adds logging methods for convenience.
 */
public abstract class AbstractIndexProducer implements IndexProducer {

  /**
   * Log beginning of index rebuild for this service.
   *
   * @param logger
   *           An slf4j logger to preserve the context.
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param total
   *           The total amount of elements to be re-added.
   * @param elementName
   *           The elements to be added (e.g. 'events').
   */
  protected void logIndexRebuildBegin(Logger logger, String indexName, int total, String elementName) {
    logger.info("Starting update of index '{}' for service '{}' with {} {}", indexName, getService(), total,
            elementName);
  }

  /**
   * Log beginning of index rebuild for this service and a specific organization.
   *
   * @param logger
   *           An slf4j logger to preserve the context.
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param total
   *           The total amount of elements to be re-added.
   * @param elementName
   *           The elements to be added (e.g. 'events').
   * @param org
   *           The organization.
   */
  protected void logIndexRebuildBegin(Logger logger, String indexName, int total, String elementName,
          Organization org) {
    logger.info("Starting update of index '{}' for service '{}' with {} {} of organization '{}'", indexName,
            getService(), total, elementName, org);
  }

  /**
   * Log the progress of the index rebuild for this service.
   *
   * @param logger
   *           An slf4j logger to preserve the context.
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param total
   *           The total amount of elements to be re-added.
   * @param current
   *           The amount of elements that have already been re-added.
   */
  protected void logIndexRebuildProgress(Logger logger, String indexName, int total, int current) {
    logIndexRebuildProgress(logger, indexName, total, current, 1);
  }

  /**
   * Log the progress of the index rebuild for this service and a specific organization.
   *
   * @param logger
   *           An slf4j logger to preserve the context.
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param total
   *           The total amount of elements to be re-added.
   * @param current
   *           The amount of elements that have already been re-added.
   * @param org
   *           The organization.
   */
  protected void logIndexRebuildProgress(Logger logger, String indexName, int total, int current, Organization org) {
    logIndexRebuildProgress(logger, indexName, total, current, 1, org);
  }

  /**
   * Log the progress of the index rebuild for this service.
   *
   * @param logger
   *           An slf4j logger to preserve the context.
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param total
   *           The total amount of elements to be re-added.
   * @param current
   *           The amount of elements that have already been re-added.
   * @param batchSize
   *           The size of the batch we re-add in one go.
   */
  protected void logIndexRebuildProgress(Logger logger, String indexName, int total, int current, int batchSize) {
    logIndexRebuildProgress(logger, indexName, total, current, batchSize, null);
  }

  /**
   * Log the progress of the index rebuild for this service.
   *
   * @param logger
   *           An slf4j logger to preserve the context.
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param total
   *           The total amount of elements to be re-added.
   * @param current
   *           The amount of elements that have already been re-added.
   * @param batchSize
   *           The size of the batch we re-add in one go.
   * @param org
   *           The organization (can be null).
   */
  protected void logIndexRebuildProgress(Logger logger, String indexName, int total, int current, int batchSize,
          Organization org) {
    final int responseInterval = (total < 100) ? 1 : (total / 100);
    if (responseInterval == 1 || batchSize > responseInterval || current == total
            || current % responseInterval < batchSize) {

      if (org == null) {
        logger.info("Updating index '{}' for service '{}': {}/{} finished, {}% complete.", indexName, getService(),
                current, total, (current * 100 / total));
      } else {
        logger.info("Updating index '{}' for service '{}' and organization '{}': {}/{} finished, {}% complete.",
                indexName, getService(), org.getId(), current, total, (current * 100 / total));
      }
    }
  }

  /**
   * Log an error when one element can't be re-indexed.
   *
   * @param logger
   *           An slf4j logger to preserve the context.
   * @param elementName
   *           The name of the element that can't be added (e.g. 'event').
   * @param element
   *           The element that can't be added.
   * @param t
   *           The error that occurred.
   */
  protected void logSkippingElement(Logger logger, String elementName, String element, Throwable t) {
    logger.error("Unable to re-index '{}' {}, skipping.", elementName, element, t);
  }

  /**
   * Log an error when one element can't be re-indexed.
   *
   * @param logger
   *           An slf4j logger to preserve the context.
   * @param elementName
   *           The name of the element that can't be added (e.g. 'event').
   * @param element
   *           The element that can't be added.
   * @param t
   *           The error that occurred.
   * @param org
   *           The organization.
   */
  protected void logSkippingElement(Logger logger, String elementName, String element, Organization org, Throwable t) {
    logger.error("Unable to re-index '{}' {} of organization '{}', skipping.", elementName, element, org.getId(), t);
  }

  /**
   * Log an error during an index rebuild for this service.
   *
   * @param logger
   *           An slf4j logger to preserve the context.
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param t
   *           The error that occurred.
   */
  protected void logIndexRebuildError(Logger logger, String indexName, Throwable t) {
    logger.error("Error updating index '{}' for service '{}'.", indexName, getService(), t);
  }

  /**
   * Log an error during an index rebuild for this service.
   *
   * @param logger
   *           An slf4j logger to preserve the context.
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param total
   *           The total amount of elements to be re-added.
   * @param current
   *           The amount of elements that have already been re-added.
   * @param t
   *           The error that occurred.
   */
  protected void logIndexRebuildError(Logger logger, String indexName, int total, int current, Throwable t) {
    logger.error("Error updating index '{}' for service '{}': {}/{} could be finished.", indexName, getService(),
            current, total, t);
  }

  /**
   * Log an error during an index rebuild for this service.
   *
   * @param logger
   *           An slf4j logger to preserve the context.
   * @param indexName
   *           The name of the index that's being rebuild.
   * @param t
   *           The error that occurred.
   * @param org
   *           The organization.
   */
  protected void logIndexRebuildError(Logger logger, String indexName, Throwable t, Organization org) {
    logger.error("Error updating index '{}' for service '{}' and organization '{}'.", indexName, getService(),
            org.getId(), t);
  }
}
