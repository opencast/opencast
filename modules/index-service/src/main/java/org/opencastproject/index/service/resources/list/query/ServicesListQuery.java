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


import org.opencastproject.index.service.resources.list.provider.BooleanListProvider;
import org.opencastproject.index.service.resources.list.provider.ServersListProvider;
import org.opencastproject.index.service.resources.list.provider.ServicesListProvider;
import org.opencastproject.index.service.util.FiltersUtils;
import org.opencastproject.list.api.ResourceListFilter;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.list.impl.ResourceListQueryImpl;
import org.opencastproject.util.data.Option;

/**
 * Query for the services list.
 *
 * The following filters can be used:
 * <ul>
 * <li>name</li>
 * <li>host</li>
 * <li>status</li>
 * <li>actions</li>
 * </ul>
 */
public class ServicesListQuery extends ResourceListQueryImpl {

  /** Prefix for the filter labels. */
  private static final String FILTER_PREFIX = "FILTERS.SERVICES";
  /** Name filter name. */
  public static final String FILTER_NAME_NAME = "name";
  /** Name filter label. */
  public static final String FILTER_LABEL_NAME = FILTER_PREFIX + ".NAME.LABEL";
  /** Hostname filter name. */
  public static final String FILTER_NAME_HOSTNAME = "hostname";
  /** Hostname filter label. */
  public static final String FILTER_LABEL_HOSTNAME = FILTER_PREFIX + ".HOSTNAME.LABEL";
  /** NodeName filter name. */
  public static final String FILTER_NAME_NODE_NAME = "nodeName";
  /** NodeName filter label. */
  public static final String FILTER_LABEL_NODE_NAME = FILTER_PREFIX + ".NODE_NAME.LABEL";
  /** Status filter name. */
  public static final String FILTER_NAME_STATUS = "status";
  /** Status filter label. */
  public static final String FILTER_LABEL_STATUS = FILTER_PREFIX + ".STATUS.LABEL";
  /** Actions filter name. */
  public static final String FILTER_NAME_ACTIONS = "actions";
  /** Actions filter label. */
  public static final String FILTER_LABEL_ACTIONS = FILTER_PREFIX + ".ACTIONS.LABEL";

  /** Default constructor. */
  public ServicesListQuery() {
    super();
    this.availableFilters.add(createNameFilter(Option.<String> none()));
    this.availableFilters.add(createHostnameFilter(Option.<String> none()));
    this.availableFilters.add(createNodeNameFilter(Option.<String> none()));
    this.availableFilters.add(createStatusFilter(Option.<String> none()));
    this.availableFilters.add(createActionsFilter(Option.<Boolean> none()));
  }

  /**
   * Copy constructor for the base class {@code ResourceListQuery}.
   *
   * @param query copy values from the given query
   */
  public ServicesListQuery(ResourceListQuery query) {
    this();
    availableFilters.addAll(query.getAvailableFilters());

    for (ResourceListFilter filter : query.getFilters())
      addFilter(filter);

    sortBy = query.getSortBy();
    if (query.getOffset().isSome())
      setOffset(query.getOffset().get());
    if (query.getLimit().isSome())
      setLimit(query.getLimit().get());
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given name.
   *
   * @param name the name to filter for
   */
  public void withName(Option<String> name) {
    addFilter(createNameFilter(name));
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given hostname.
   *
   * @param hostname the hostname to filter for
   */
  public void withHostname(Option<String> hostname) {
    addFilter(createHostnameFilter(hostname));
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given node name.
   *
   * @param nodeName the node name to filter for
   */
  public void withNodeName(String nodeName) {
    addFilter(createNodeNameFilter(Option.option(nodeName)));
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given status.
   *
   * @param status the status to filter for
   */
  public void withStatus(Option<String> status) {
    addFilter(createStatusFilter(status));
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with any actions.
   *
   * @param hasActions any actions to filter
   */
  public void withActions(Option<Boolean> hasActions) {
    addFilter(createActionsFilter(hasActions));
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given free text.
   *
   * @param freeText the free text to filter for
   */
  public void withFreeText(Option<String> freeText) {
    addFilter(createFreeTextFilter(freeText));
  }

  /**
   * Returns an {@link Option} containing the name used to filter if set.
   * {@link Option#none()} otherwise.
   *
   * @return an {@link Option} containing the name or none.
   */
  public Option<String> getName() {
    return getFilterValue(FILTER_NAME_NAME);
  }

  /**
   * Returns an {@link Option} containing the hostname used to filter if set.
   * {@link Option#none()} otherwise.
   *
   * @return an {@link Option} containing the hostname or none.
   */
  public Option<String> getHostname() {
    return getFilterValue(FILTER_NAME_HOSTNAME);
  }

  /**
   * Returns an {@link Option} containing the node name used to filter if set.
   * {@link Option#none()} otherwise.
   *
   * @return an {@link Option} containing the node name or none.
   */
  public Option<String> getNodeName() {
    return getFilterValue(FILTER_NAME_NODE_NAME);
  }

  /**
   * Returns an {@link Option} containing the status used to filter if set.
   * {@link Option#none()} otherwise.
   *
   * @return an {@link Option} containing the status or none.
   */
  public Option<String> getStatus() {
    return getFilterValue(FILTER_NAME_STATUS);
  }

  /**
   * Returns an {@link Option} containing boolean value used to filter any actions if set.
   * {@link Option#none()} otherwise.
   *
   * @return an {@link Option} containing the boolean value for any actions or none.
   */
  public Option<Boolean> getActions() {
    Option result = getFilterValue(FILTER_NAME_ACTIONS);
    if (result.isSome()) {
      if (result.get() instanceof String) {
        return BooleanListProvider.parseValue((String)result.get());
      }
    }
    return result;
  }

  /**
   * Returns an {@link Option} containing the free text used to filter if set.
   * {@link Option#none()} otherwise.
   *
   * @return an {@link Option} containing the free text or none.
   */
  public Option<String> getFreeText() {
    return getFilterValue(ResourceListFilter.FREETEXT);
  }

  /**
   * Create a new {@link ResourceListFilter} based on a name.
   *
   * @param value the name to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a name based query
   */
  public static <String> ResourceListFilter<String> createNameFilter(Option<String> value) {
    return FiltersUtils.generateFilter(
            value,
            FILTER_NAME_NAME,
            FILTER_LABEL_NAME,
            ResourceListFilter.SourceType.SELECT,
            Option.some(ServicesListProvider.LIST_NAME));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a hostname.
   *
   * @param value the hostname to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a hostname based query
   */
  public static <String> ResourceListFilter<String> createHostnameFilter(Option<String> value) {
    return FiltersUtils.generateFilter(
            value,
            FILTER_NAME_HOSTNAME,
            FILTER_LABEL_HOSTNAME,
            ResourceListFilter.SourceType.SELECT,
            Option.some(ServersListProvider.LIST_HOSTNAME));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a nodeName.
   *
   * @param value the nodeName to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a nodeName based query
   */
  public static ResourceListFilter<String> createNodeNameFilter(Option<String> value) {
    return FiltersUtils.generateFilter(
            value,
            FILTER_NAME_NODE_NAME,
            FILTER_LABEL_NODE_NAME,
            ResourceListFilter.SourceType.SELECT,
            Option.some(ServersListProvider.LIST_NODE_NAME));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a status.
   *
   * @param value the status to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a status based query
   */
  public static <String> ResourceListFilter<String> createStatusFilter(Option<String> value) {
    return FiltersUtils.generateFilter(
            value,
            FILTER_NAME_STATUS,
            FILTER_LABEL_STATUS,
            ResourceListFilter.SourceType.SELECT,
            Option.some(ServicesListProvider.LIST_STATUS));
  }

  /**
   * Create a new {@link ResourceListFilter} based on any actions.
   *
   * @param value the boolean value for any action to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a any actions based query
   */
  public static <Boolean> ResourceListFilter<Boolean> createActionsFilter(Option<Boolean> value) {
    return FiltersUtils.generateFilter(
            value,
            FILTER_NAME_ACTIONS,
            FILTER_LABEL_ACTIONS,
            ResourceListFilter.SourceType.SELECT,
            Option.some(BooleanListProvider.YES_NO));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a free text.
   *
   * @param value the free text to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a free text based query
   */
  public static <String> ResourceListFilter<String> createFreeTextFilter(Option<String> value) {
    return FiltersUtils.generateFilter(
            value,
            ResourceListFilter.FREETEXT,
            ResourceListFilter.FREETEXT,
            ResourceListFilter.SourceType.FREETEXT,
            null);
  }
}
