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
package org.opencastproject.basicstatisticsaggregation.persistence;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Converts an int[] (e.g. hourly bucket values) to a comma-separated column and back.
 */
@Converter(autoApply = false)
public class IntArrayConverter implements AttributeConverter<int[], String> {

  @Override
  public String convertToDatabaseColumn(int[] values) {
    if (values == null) {
      return null;
    }
    return Arrays.stream(values)
        .mapToObj(String::valueOf)
        .collect(Collectors.joining(","));
  }

  @Override
  public int[] convertToEntityAttribute(String column) {
    if (column == null || column.isEmpty()) {
      return new int[0];
    }
    return Arrays.stream(column.split(","))
        .mapToInt(Integer::parseInt)
        .toArray();
  }
}
