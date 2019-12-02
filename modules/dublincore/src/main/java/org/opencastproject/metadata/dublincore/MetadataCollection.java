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
package org.opencastproject.metadata.dublincore;

import java.util.List;
import java.util.Map;

public interface MetadataCollection {

  /**
   * Copy all fields of a metadata collection into a new collection.
   *
   * @return a copy of the current MetadataCollection
   */
  MetadataCollection getCopy();

  Map<String, MetadataField<?>> getInputFields();

  Map<String, MetadataField<?>> getOutputFields();

  /**
   * Add the given {@link MetadataField} field to the metadata list
   *
   * @param metadata
   *          The {@link MetadataField} field to add
   * @throws IllegalArgumentException
   *           if the {@link MetadataField} is null
   *
   */
  void addField(MetadataField<?> metadata);

  void removeField(MetadataField<?> metadata);

  List<MetadataField<?>> getFields();

  void updateStringField(MetadataField<?> current, String value);

  boolean isUpdated();

}
