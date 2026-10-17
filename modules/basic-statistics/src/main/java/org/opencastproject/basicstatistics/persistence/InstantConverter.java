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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Stores an {@link Instant} as a timestamp in UTC.
 *
 * The JPA implementation does not support {@code Instant} and would store it as serialized Java object, which cannot be
 * compared or processed in the database. Storing it as {@link LocalDateTime} in UTC gives a proper timestamp column,
 * and, unlike {@code java.sql.Timestamp} or {@code java.util.Date}, it does not depend on the time zone of the JVM,
 * which would corrupt values around daylight saving time changes.
 *
 * Applied automatically to all attributes of that type, so the converter has to be listed in the persistence.xml.
 */
@Converter(autoApply = true)
public class InstantConverter implements AttributeConverter<Instant, LocalDateTime> {
  @Override
  public LocalDateTime convertToDatabaseColumn(Instant i) {
    return i == null ? null : LocalDateTime.ofInstant(i, ZoneOffset.UTC);
  }

  @Override
  public Instant convertToEntityAttribute(LocalDateTime t) {
    return t == null ? null : t.toInstant(ZoneOffset.UTC);
  }
}
