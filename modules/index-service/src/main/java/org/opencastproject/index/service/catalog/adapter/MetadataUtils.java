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

package org.opencastproject.index.service.catalog.adapter;

import org.opencastproject.metadata.dublincore.MetadataField;

import java.util.ArrayList;
import java.util.List;

/**
 * A helper class for dealing with metadata.
 */
public final class MetadataUtils {

  private MetadataUtils() {
  }

  /*
   * Get an Iterable<String> from a {@link MetadataField} if available.
   *
   * @param metadataField
   *          The {@link MetadataField} to get the value from.
   * @return A Iterable (empty if not available)
   */
  public static Iterable<String> getIterableStringMetadata(final MetadataField metadataField) {
    final List<String> strings = new ArrayList<>();
    if (metadataField.getValue() != null) {
      if (metadataField.getValue() instanceof Iterable<?>) {
        final Iterable<?> iterableCollection = (Iterable<?>) metadataField.getValue();
        for (final Object value : iterableCollection) {
          if (value instanceof String) {
            strings.add(value.toString());
          }
        }
      } else if (metadataField.getValue() instanceof String) {
        strings.add(metadataField.getValue().toString());
      }

    }
    return strings;
  }
}
