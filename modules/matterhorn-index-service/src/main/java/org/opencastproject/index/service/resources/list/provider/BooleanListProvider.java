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

package org.opencastproject.index.service.resources.list.provider;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/** Boolean list provider. */
public class BooleanListProvider implements ResourceListProvider {

  public static final String TRUE = "TRUE";
  public static final String FALSE = "FALSE";
  public static final String TRUE_FALSE = "TRUE_FALSE";
  public static final String YES = "YES";
  public static final String NO = "NO";
  public static final String YES_NO = "YES_NO";
  public static final String ON = "ON";
  public static final String OFF = "OFF";
  public static final String ON_OFF = "ON_OFF";

  /** The names of the different list available through this provider. */
  private static final String[] NAMES = new String[] {
    TRUE, FALSE, TRUE_FALSE, YES, NO, YES_NO, ON, OFF, ON_OFF
  };

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query, Organization organization) throws ListProviderException {
    Map<String, String> result = new HashMap<String, String>();

    String listNameTrimmed = StringUtils.trimToEmpty(listName);

    if (StringUtils.equalsIgnoreCase(YES, listNameTrimmed)
            || StringUtils.equalsIgnoreCase(YES_NO, listNameTrimmed))
      result.put("true", YES);
    if (StringUtils.equalsIgnoreCase(NO, listNameTrimmed)
            || StringUtils.equalsIgnoreCase(YES_NO, listNameTrimmed))
      result.put("false", NO);
    if (StringUtils.equalsIgnoreCase(TRUE, listNameTrimmed)
            || StringUtils.equalsIgnoreCase(TRUE_FALSE, listNameTrimmed))
      result.put("true", TRUE);
    if (StringUtils.equalsIgnoreCase(FALSE, listNameTrimmed)
            || StringUtils.equalsIgnoreCase(TRUE_FALSE, listNameTrimmed))
      result.put("false", FALSE);
    if (StringUtils.equalsIgnoreCase(ON, listNameTrimmed)
            || StringUtils.equalsIgnoreCase(ON_OFF, listNameTrimmed))
      result.put("true", ON);
    if (StringUtils.equalsIgnoreCase(OFF, listNameTrimmed)
            || StringUtils.equalsIgnoreCase(ON_OFF, listNameTrimmed))
      result.put("false", OFF);

    return result;
  }

  /**
   * Parse boolean value from the given string wrapped in an {@link Option}.
   *
   * @param filterValue boolean value as string
   * @return boolean value wrapped in a {@link Option} or {@link Option#none()}
   */
  public static <Boolean> Option<Boolean> parseOptValue(Option<String> filterValue) {
    if (filterValue.isSome())
      return parseValue(filterValue.get());

    return Option.none();
  }

  /**
   * Parse boolean value from the given string.
   *
   * @param filterValue boolean value as string
   * @return boolean value wrapped in an {@link Option} or {@link Option#none()}
   */
  public static <Boolean> Option<Boolean> parseValue(String filterValue) {
    String value = StringUtils.trimToEmpty(filterValue);
    if (StringUtils.equalsIgnoreCase(YES, value)
            || StringUtils.equalsIgnoreCase(ON, value)
            || StringUtils.equalsIgnoreCase(TRUE, value))
      return (Option<Boolean>) Option.option(true);
    else if (StringUtils.equalsIgnoreCase(NO, value)
            || StringUtils.equalsIgnoreCase(OFF, value)
            || StringUtils.equalsIgnoreCase(FALSE, value))
      return (Option<Boolean>) Option.option(false);
    else return Option.<Boolean> none();
  }
}
