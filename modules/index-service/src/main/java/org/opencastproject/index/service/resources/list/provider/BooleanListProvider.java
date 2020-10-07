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

import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/** Boolean list provider. */
public class BooleanListProvider implements ResourceListProvider {

  public static final String YES_NO = "YES_NO";
  public static final String YES = "YES";
  public static final String NO = "NO";

  /** The names of the different list available through this provider. */
  private static final String[] NAMES = new String[] {
    YES_NO, YES, NO
  };

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query) {
    Map<String, String> result = new HashMap<String, String>();

    String listNameTrimmed = StringUtils.trimToEmpty(listName);

    if (StringUtils.equalsIgnoreCase(YES, listNameTrimmed)
            || StringUtils.equalsIgnoreCase(YES_NO, listNameTrimmed))
      result.put("true", YES);
    if (StringUtils.equalsIgnoreCase(NO, listNameTrimmed)
            || StringUtils.equalsIgnoreCase(YES_NO, listNameTrimmed))
      result.put("false", NO);

    return result;
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
            || StringUtils.equalsIgnoreCase("true", value))
      return (Option<Boolean>) Option.option(true);
    else if (StringUtils.equalsIgnoreCase(NO, value)
            || StringUtils.equalsIgnoreCase("false", value))
      return (Option<Boolean>) Option.option(false);
    else return Option.<Boolean> none();
  }

  @Override
  public boolean isTranslatable(String listName) {
    return true;
  }

  @Override
  public String getDefault() {
    return null;
  }
}
