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
package org.opencastproject.basicstatistics.persistence;

import org.opencastproject.basicstatistics.EventType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class EventTypeConverter implements AttributeConverter<EventType, Short> {

  @Override
  public Short convertToDatabaseColumn(EventType type) {
    if (type == null) {
      return null;
    }
    return type.getDbId();
  }

  @Override
  public EventType convertToEntityAttribute(Short dbId) {
    if (dbId == null) {
      return null;
    }
    return EventType.fromDbId(dbId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown event type database ID " + dbId));
  }
}
