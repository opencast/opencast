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

package org.opencastproject.elasticsearch.index.rebuild;

import org.opencastproject.elasticsearch.index.rebuild.IndexRebuildService.DataType;

import java.util.Arrays;

/**
 * This service handles data that's added to an ElasticSearch index.
 */
public interface IndexProducer {

  /**
   * Re-add the data of this service to the index.
   *
   * @param dataType
   *          Limit the data added to the index. Use ALL to re-index all data.
   */
  void repopulate(DataType dataType) throws IndexRebuildException;

  /**
   * Get the service that implements IndexProducer.
   *
   * @return service
   *           The service that implements IndexProducer.
   */
  IndexRebuildService.Service getService();

  /**
   * Get supported data types for reindexing. Default: All.
   *
   * Should be overridden if a service offers partial index rebuilds.
   *
   * @return Array of supported data types
   */
  default DataType[] getSupportedDataTypes() {
    return new DataType[]{ DataType.ALL };
  }

  /**
   * Check if the data type is supported by this service.
   *
   * @param dataType The data type to check.
   *
   * @return Whether the data type is supported or not.
   */
  default boolean dataTypeSupported(DataType dataType) {
    return Arrays.stream(getSupportedDataTypes()).anyMatch(s -> s == dataType);
  }
}
