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
package org.opencastproject.oaipmh.persistence;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class OaiPmhSetDefinitionImpl implements OaiPmhSetDefinition {

  private String setSpec;
  private String name;
  private String description;
  private Map<String, OaiPmhSetDefinitionFilter> filters = new HashMap<>();

  @Override
  public String getSetSpec() {
    return setSpec;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Collection<OaiPmhSetDefinitionFilter> getFilters() {
    return Collections.unmodifiableCollection(filters.values());
  }

  public OaiPmhSetDefinitionFilter addFilter(String filterId, String flavor, String criterion, String criterionValue) {
    if (StringUtils.isEmpty(filterId)) {
      throw new IllegalArgumentException(String.format("Set definition '%s' filter identifier not set.", setSpec));
    }
    if (StringUtils.isEmpty(flavor)) {
      throw new IllegalArgumentException(String.format(
          "Set definition '%s' flavor not set for filter identified by '%s'.", setSpec, filterId));
    }
    if (!StringUtils.equals(OaiPmhSetDefinitionFilter.CRITERION_CONTAINS, criterion)
        && !StringUtils.equals(OaiPmhSetDefinitionFilter.CRITERION_CONTAINSNOT, criterion)
        && !StringUtils.equals(OaiPmhSetDefinitionFilter.CRITERION_MATCH, criterion)) {
      throw new IllegalArgumentException(String.format(
          "Set definition '%s' filter (idenfied by '%s') criterion '%s' should be one of %s",
          setSpec, filterId, criterion, StringUtils.joinWith(", ",
              OaiPmhSetDefinitionFilter.CRITERION_CONTAINS,
              OaiPmhSetDefinitionFilter.CRITERION_CONTAINSNOT,
              OaiPmhSetDefinitionFilter.CRITERION_MATCH)));
    }
    if (StringUtils.isEmpty(criterionValue)) {
      throw new IllegalArgumentException(String.format(
          "Set definition '%s' filter (idenfied by '%s') criterion '%s' value not set.",
          setSpec, filterId, criterion));
    }
    OaiPmhSetDefinitionFilter filter = filters.get(filterId);
    Map<String, List<String>> criteria = new HashMap<>();
    if (filter != null) {
      filters.remove(filter);
      criteria.putAll(filter.getCriteria());
    }
    if (criteria.containsKey(criterion)) {
      List<String> criteriaValues = new LinkedList<>(criteria.get(criterion));
      criteriaValues.add(criterionValue);
      criteria.replace(criterion, Collections.unmodifiableList(criteriaValues));
    } else {
      List<String> criteriaValues = new LinkedList<>();
      criteriaValues.add(criterionValue);
      criteria.put(criterion, Collections.unmodifiableList(criteriaValues));
    }
    filter = new OaiPmhSetDefinitionFilter() {
      @Override
      public String getFlavor() {
        return flavor;
      }

      @Override
      public Map<String, List<String>> getCriteria() {
        return Collections.unmodifiableMap(criteria);
      }
    };
    filters.put(filterId, filter);
    return filter;
  }

  public static OaiPmhSetDefinitionImpl build(String setSpec, String name, String description) {
    if (StringUtils.isEmpty(setSpec)) {
      throw new IllegalArgumentException("setSpec not set.");
    }
    if (StringUtils.isEmpty(name)) {
      throw new IllegalArgumentException("name not set.");
    }
    OaiPmhSetDefinitionImpl instance = new OaiPmhSetDefinitionImpl();
    instance.setSpec = setSpec;
    instance.name = name;
    instance.description = description;
    return instance;
  }
}
