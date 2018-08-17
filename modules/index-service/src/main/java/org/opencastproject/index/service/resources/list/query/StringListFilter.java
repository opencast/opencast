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

package org.opencastproject.index.service.resources.list.query;

import org.opencastproject.index.service.resources.list.api.ResourceListFilter;
import org.opencastproject.util.data.Option;

/**
 * Simple {@link ResourceListFilter} implementation for freetext filter
 */
public class StringListFilter implements ResourceListFilter<String> {

  // Default label for the freetext field
  private static final String FREETEXT_LABEL = "FREETEXT.LABEL";

  private String name = FREETEXT;
  private final Option<String> value;
  private SourceType sourceType = SourceType.FREETEXT;
  private String label = FREETEXT_LABEL;

  /**
   * Standard constructor, accepting a key and an {@link Option} value.
   *
   * @param name
   * @param value
   */
  public StringListFilter(String name, Option<String> value) {
    this.value = value;
    this.name = name;
  }

  /**
   * Standard constructor, accepting a key and a value.
   *
   * @param name
   * @param value
   */
  public StringListFilter(String name, String value) {
    this.value = Option.some(value);
    this.name = name;
  }

  /**
   * This constructor is a shortcut for free-text filters. The name will be {@link ResourceListFilter#FREETEXT}.
   *
   * @param value
   */
  public StringListFilter(String value) {
    this.value = Option.some(value);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Option<String> getValue() {
    return value;
  }

  @Override
  public SourceType getSourceType() {
    return sourceType;
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public Option<String> getValuesListName() {
    return Option.none();
  }

}
