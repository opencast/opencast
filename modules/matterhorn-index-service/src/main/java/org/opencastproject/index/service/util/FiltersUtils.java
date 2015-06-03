/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.index.service.util;

import org.opencastproject.index.service.resources.list.api.ResourceListFilter;
import org.opencastproject.index.service.resources.list.api.ResourceListFilter.SourceType;
import org.opencastproject.index.service.resources.list.query.AbstractListFilter;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class providing helpers for all operations related to the filters.
 */
public final class FiltersUtils {

  private FiltersUtils() {

  }

  /**
   * Create a new {@link ResourceListFilter} following the parameters given
   * 
   * @param value
   *          The value of the filter wrapped in an {@link Option}. Can be {@link Option#none()}
   * @param name
   *          The name of the filter, required.
   * @param label
   *          The label of the filter, required.
   * @param type
   *          the {@link SourceType}
   * @param valuesListName
   *          The name of the list from a list provider providing the possible values wrapped in a {@link Option}.Can be
   *          {@link Option#none()}
   * @throws IllegalArgumentException
   *           if the name, label or type is null or empty.
   * @return a new {@link ResourceListFilter} with the parameters given
   */
  public static <A> ResourceListFilter<A> generateFilter(final Option<A> value, final String name, final String label,
          final SourceType type, final Option<String> valuesListName) throws IllegalArgumentException {
    if (StringUtils.isBlank(name) || StringUtils.isBlank(label) || type == null)
      throw new IllegalArgumentException("The filter label, name or type must not be null!");

    return new AbstractListFilter<A>(value) {

      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getLabel() {
        return label;
      }

      @Override
      public Option<String> getValuesListName() {
        return valuesListName != null ? valuesListName : Option.<String> none();
      }

      @Override
      public SourceType getSourceType() {
        return type;
      }

    };
  }
}
