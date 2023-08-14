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

package org.opencastproject.statistics.api;

import java.util.List;
import java.util.OptionalDouble;

/**
 * TimeSeries result with labels and values. For each label, there is one value.
 */
public class TimeSeries {
  private List<String> labels;
  private List<Double> values;
  private Double total;

  public TimeSeries(List<String> labels, List<Double> values) {
    this.labels = labels;
    this.values = values;
  }

  public TimeSeries(List<String> labels, List<Double> values, Double total) {
    this.labels = labels;
    this.values = values;
    this.total = total;
  }

  public List<String> getLabels() {
    return labels;
  }

  public List<Double> getValues() {
    return values;
  }

  public OptionalDouble getTotal() {
    if (total == null) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of(total);
  }

  public  void setTotal(Double total) {
    this.total = total;
  }
}
