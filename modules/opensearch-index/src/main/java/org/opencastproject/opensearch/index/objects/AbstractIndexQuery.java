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

package org.opencastproject.opensearch.index.objects;

import static java.util.Objects.requireNonNull;

import org.opencastproject.opensearch.impl.IndexSchema;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.Permissions.Action;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.requests.SortCriterion.Order;

import org.apache.commons.lang3.StringUtils;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.ScriptSortType;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Operator;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.SearchRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for queries against the event and series index. It collects the conditions set by the {@code with...}
 * methods of its subclasses and turns them into an OpenSearch search request.
 *
 * @param <Q>
 *          the concrete query type, returned by the fluent methods
 */
public abstract class AbstractIndexQuery<Q extends AbstractIndexQuery<Q>> {

  /** The maximum number of results OpenSearch may return. */
  private static final int MAX_RESULT_WINDOW = Integer.MAX_VALUE;

  /** Fields searched by free text queries, in addition to the ones defined by the subclasses. */
  private static final List<String> TEXT_FIELDS = List.of(
      "*" + IndexSchema.SEARCH_FIELD_NAME_EXTENSION + "^1.2",
      // Search-as-you-type generates useful subfields for autocompletion, we should use it
      // https://docs.opensearch.org/latest/field-types/supported-field-types/search-as-you-type/
      "*" + IndexSchema.SEARCH_FIELD_NAME_EXTENSION + "._2gram^1.2",
      "*" + IndexSchema.SEARCH_FIELD_NAME_EXTENSION + "._3gram^1.2",
      "*" + IndexSchema.SEARCH_FIELD_NAME_EXTENSION + "._4gram^1.2");

  /** Sorting by this field sorts by the number of publications */
  private static final String PUBLICATION_SORT_FIELD = "publication";

  private final String organization;
  private final User user;
  private final Set<String> actions = new LinkedHashSet<>();
  private final Map<String, String> accessControlEntries = new LinkedHashMap<>();

  /** Fields which have to match one of the values */
  private final Map<String, Set<Object>> terms = new LinkedHashMap<>();

  /** Fields which must not match any of the values */
  private final Map<String, Set<Object>> termsNot = new LinkedHashMap<>();

  /** Other conditions which have to match, by field */
  private final Map<String, List<Query>> must = new LinkedHashMap<>();

  /** Periods the date in a field has to be in, by field. Independent of the conditions in {@link #must}. */
  private final Map<String, Query> periods = new LinkedHashMap<>();

  /** Other conditions which must not match */
  private final List<Query> mustNot = new ArrayList<>();

  private String text = null;
  private boolean fuzzy = true;
  private final Map<String, Order> sortOrders = new LinkedHashMap<>();
  private int offset = -1;
  private int limit = -1;

  /**
   * Creates a query for documents of the given organization, visible to the given user. Unless changed with
   * {@link #withoutActions()}, only documents the user may read are found.
   *
   * @param organization
   *          the organization
   * @param user
   *          the user
   * @throws IllegalStateException
   *          if the user belongs to a different organization and is not a global administrator
   */
  protected AbstractIndexQuery(String organization, User user) {
    this.organization = requireNonNull(organization, "No organization set on the search query");
    this.user = requireNonNull(user);
    this.actions.add(Permissions.Action.READ.toString());
    if (!SecurityUtil.isGlobalAdmin(user) && !user.getOrganization().getId().equals(organization)) {
      throw new IllegalStateException("User's organization must match search organization");
    }
    and(organizationField(), organization);
  }

  /** The index field holding the organization */
  protected abstract String organizationField();

  /** Prefix of the index fields holding the roles allowed to perform an action */
  protected abstract String aclPermissionPrefix();

  /** Prefix of the index fields holding extended metadata */
  protected abstract String extendedMetadataPrefix();

  /** The index field to sort by when asked to sort by the given field */
  protected abstract String sortField(String field);

  /** Additional fields searched by free text queries, with default boost */
  protected abstract List<String> additionalTextFields();

  @SuppressWarnings("unchecked")
  private Q self() {
    return (Q) this;
  }

  public String getOrganization() {
    return organization;
  }

  public User getUser() {
    return user;
  }

  /**
   * Find documents which have the given value in the given field. Calling this again for the same field adds more
   * values, of which documents have to match one. {@code null} values are ignored.
   */
  protected Q and(String field, Object value) {
    if (value != null) {
      terms.computeIfAbsent(field, f -> new LinkedHashSet<>()).add(value);
    }
    return self();
  }

  /** Find documents which do not have the given value in the given field. {@code null} values are ignored. */
  protected Q andNot(String field, Object value) {
    if (value != null) {
      termsNot.computeIfAbsent(field, f -> new LinkedHashSet<>()).add(value);
    }
    return self();
  }

  /**
   * Find documents which have the given value in the given field, replacing previously set values. Setting
   * {@code null} removes the condition.
   */
  protected Q set(String field, Object value) {
    terms.remove(field);
    return and(field, value);
  }

  /** Find documents with values matching the wildcard pattern. */
  protected Q andWildcard(String field, String pattern, boolean match) {
    final Query query = Query.of(q -> q.wildcard(w -> w.field(field).value(pattern)));
    if (match) {
      must.computeIfAbsent(field, f -> new ArrayList<>()).add(query);
    } else {
      mustNot.add(query);
    }
    return self();
  }

  /** Find documents with a date in the given field after the given date. */
  protected Q andAfter(String field, String date) {
    must.computeIfAbsent(field, f -> new ArrayList<>())
        .add(Query.of(q -> q.range(r -> r.field(field).gt(JsonData.of(date)))));
    return self();
  }

  /** Find documents with a date in the given field before the given date. */
  protected Q andBefore(String field, String date) {
    must.computeIfAbsent(field, f -> new ArrayList<>())
        .add(Query.of(q -> q.range(r -> r.field(field).lt(JsonData.of(date)))));
    return self();
  }

  /**
   * Find documents with a date in the given field within the given period, replacing a previously set period for the
   * same field. Other conditions for the field, e.g. from {@link #andAfter(String, String)}, still apply. Both dates
   * are inclusive.
   */
  protected Q andBetween(String field, Date from, Date to) {
    periods.put(field, Query.of(q -> q.range(r -> r.field(field)
        .gte(JsonData.of(DateTimeSupport.toUTC(from.getTime())))
        .lte(JsonData.of(DateTimeSupport.toUTC(to.getTime()))))));
    return self();
  }

  /** Only return documents for which the user is allowed to perform the given action. */
  public Q withAction(Action action) {
    actions.add(requireNonNull(action, "Action cannot be null").toString());
    return self();
  }

  /** Do not restrict the results to documents the user is allowed to read. */
  public Q withoutActions() {
    actions.clear();
    return self();
  }

  /** Only return documents for which the given role is allowed to perform the given action. */
  public Q withAccessControlEntry(String role, Action action) {
    if (StringUtils.isBlank(role)) {
      throw new IllegalArgumentException("Role cannot be null");
    }
    accessControlEntries.put(role, requireNonNull(action, "Action cannot be null").toString());
    return self();
  }

  /** Only return documents with the given value in the extended metadata field. */
  public Q withExtendedMetadata(String flavor, String key, String value) {
    return and(extendedMetadataPrefix() + flavor + "_" + key, value);
  }

  /** Only return documents matching the given text. Uses fuzzy matching. */
  public Q withText(String text) {
    return withText(true, text);
  }

  /** Only return documents matching the given text. */
  public Q withText(boolean fuzzy, String text) {
    // Only the first text counts, but the last fuzzy setting, like it always did
    requireNonNull(text, "Text must not be null");
    if (this.text == null) {
      this.text = text;
    }
    this.fuzzy = fuzzy;
    return self();
  }

  /** Skip the first {@code offset} results. */
  public Q withOffset(int offset) {
    this.offset = Math.max(0, offset);
    return self();
  }

  /** Return at most {@code limit} results. */
  public Q withLimit(int limit) {
    this.limit = limit;
    return self();
  }

  /** Sort by the given field. Earlier sort orders take precedence. */
  public Q withSortOrder(String field, Order order) {
    sortOrders.put(sortField(requireNonNull(field)), requireNonNull(order));
    return self();
  }

  public int getOffset() {
    return offset;
  }

  public int getLimit() {
    return limit;
  }

  /** The sort orders by index field, in the order of precedence */
  public Map<String, Order> getSortOrders() {
    return Collections.unmodifiableMap(sortOrders);
  }

  /** The sort order for the given field, {@link Order#None} if not sorted by it */
  public Order getSortOrder(String field) {
    return sortOrders.getOrDefault(sortField(field), Order.None);
  }

  /** The values the given field has to match one of, empty if not restricted */
  public Set<Object> getFilterValues(String field) {
    return Collections.unmodifiableSet(terms.getOrDefault(field, Set.of()));
  }

  /**
   * Builds the OpenSearch request for this query.
   *
   * @param index
   *          the name of the index to search
   * @return the search request
   */
  public SearchRequest toSearchRequest(String index) {
    return SearchRequest.of(r -> {
      r.index(index)
          .query(q -> q.bool(this::buildQuery))
          .trackTotalHits(t -> t.enabled(true))
          .preference("_local")
          .source(s -> s.filter(f -> f.includes(IndexSchema.OBJECT)))
          .size(size())
          .sort(sortOptions());
      if (offset >= 0) {
        r.from(offset);
      }
      return r;
    });
  }

  private BoolQuery.Builder buildQuery(BoolQuery.Builder bool) {
    final Map<String, Set<Object>> filters = new LinkedHashMap<>(terms);
    if (!actions.isEmpty() && !SecurityUtil.isAdmin(user, user.getOrganization())) {
      for (String action : actions) {
        for (Role role : user.getRoles()) {
          if (role.getName() != null) {
            filters.computeIfAbsent(aclPermissionPrefix() + action, f -> new LinkedHashSet<>()).add(role.getName());
          }
        }
      }
    }
    accessControlEntries.forEach((role, action) ->
        filters.computeIfAbsent(aclPermissionPrefix() + action, f -> new LinkedHashSet<>()).add(role));

    filters.forEach((field, values) -> bool.filter(termsQuery(field, values)));
    termsNot.forEach((field, values) -> bool.mustNot(termsQuery(field, values)));
    must.values().forEach(bool::must);
    periods.values().forEach(bool::must);
    if (!mustNot.isEmpty()) {
      bool.mustNot(mustNot);
    }

    if (text != null) {
      final List<String> fields = new ArrayList<>(TEXT_FIELDS);
      fields.addAll(additionalTextFields());
      bool.should(q -> q.multiMatch(m -> {
        m.query(text).fields(fields).type(TextQueryType.BoolPrefix).operator(Operator.And);
        if (fuzzy) {
          m.fuzziness("AUTO");
        }
        return m;
      }));
      bool.minimumShouldMatch("1");
    }
    return bool;
  }

  private static Query termsQuery(String field, Set<Object> values) {
    final List<FieldValue> fieldValues = values.stream().map(AbstractIndexQuery::fieldValue).toList();
    return Query.of(q -> q.terms(t -> t.field(field).terms(v -> v.value(fieldValues))));
  }

  private static FieldValue fieldValue(Object value) {
    if (value instanceof Boolean b) {
      return FieldValue.of(b);
    } else if (value instanceof Long || value instanceof Integer) {
      return FieldValue.of(((Number) value).longValue());
    }
    return FieldValue.of(value.toString());
  }

  private int size() {
    if (limit <= 0) {
      return MAX_RESULT_WINDOW;
    }
    if (offset > 0 && (long) offset + (long) limit > MAX_RESULT_WINDOW) {
      return MAX_RESULT_WINDOW - offset;
    }
    return limit;
  }

  private List<SortOptions> sortOptions() {
    final List<SortOptions> sort = new ArrayList<>();
    sortOrders.forEach((field, order) -> {
      if (order == Order.None) {
        return;
      }
      final SortOrder sortOrder = order == Order.Ascending ? SortOrder.Asc : SortOrder.Desc;
      if (PUBLICATION_SORT_FIELD.equals(field)) {
        sort.add(SortOptions.of(s -> s.script(ss -> ss
            .script(sc -> sc.inline(i -> i.source("params._source.publication.length")))
            .type(ScriptSortType.Number)
            .order(sortOrder))));
      } else {
        sort.add(SortOptions.of(s -> s.field(f -> f.field(field).order(sortOrder))));
      }
    });
    sort.add(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))));
    // Used to stabilize the sort order of results with the same score.
    sort.add(SortOptions.of(s -> s.doc(d -> d.order(SortOrder.Asc))));
    return sort;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " " + toSearchRequest("index").toJsonString();
  }
}
