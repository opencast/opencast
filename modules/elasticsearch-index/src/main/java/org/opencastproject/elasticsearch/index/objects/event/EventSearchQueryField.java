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
package org.opencastproject.elasticsearch.index.objects.event;

public class EventSearchQueryField<T> {

  private T value;
  private EventQueryType type;
  private boolean must;

  public EventSearchQueryField() {
    this(null, EventQueryType.SEARCH, true);
  }

  public EventSearchQueryField(T value) {
    this(value, EventQueryType.SEARCH, true);
  }
  public EventSearchQueryField(T value, EventQueryType type) {
    this(value, type, true);
  }

  public EventSearchQueryField(T value, boolean must) {
    this(value, EventQueryType.SEARCH, must);
  }

  public EventSearchQueryField(T value, EventQueryType type, boolean must) {
    this.value = value;
    this.type = type;
    this.must = must;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }

  public EventQueryType getType() {
    return type;
  }

  public void setType(EventQueryType type) {
    this.type = type;
  }

  public boolean isMust() {
    return must;
  }

  public void setMust(boolean must) {
    this.must = must;
  }
}
