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
package org.opencastproject.index.service.resources.list.api;

import org.opencastproject.util.data.Option;

/**
 * Resource list filter interface
 * 
 * @param <A>
 *          The type of which the filter is based
 */
public interface ResourceListFilter<A> {

  public static enum SourceType {
    BOOLEAN, DATE, FREETEXT, LIST, SELECT, PERIOD
  }

  /**
   * Denotes free text which may appear in any field.
   */
  String FREETEXT = "textFilter";

  /**
   * Returns the name of this filter target field
   * 
   * @return the filter name
   */
  String getName();

  /**
   * Returns the filter label as translation key.
   * 
   * @return the filter label translation key.
   */
  String getLabel();

  /**
   * Returns the name of the list containing the possible values for this filter.
   * 
   * @return the list name giving the values available for this filter
   */
  Option<String> getValuesListName();

  /**
   * Returns the value of the filter
   * 
   * @return the filter value
   */
  Option<A> getValue();

  /**
   * Return the type of the filter
   * 
   * @return the filter type
   */
  SourceType getSourceType();

}
