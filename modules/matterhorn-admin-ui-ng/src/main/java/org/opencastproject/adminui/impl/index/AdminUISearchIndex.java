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
package org.opencastproject.adminui.impl.index;

import org.opencastproject.index.service.api.EventIndex;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventIndexSchema;
import org.opencastproject.index.service.impl.index.group.Group;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.impl.index.theme.Theme;
import org.opencastproject.index.service.impl.index.theme.ThemeIndexSchema;
import org.opencastproject.util.data.Option;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A search index implementation based on ElasticSearch.
 */
public class AdminUISearchIndex extends AbstractSearchIndex implements EventIndex {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AdminUISearchIndex.class);

  /** The name of this index */
  private static final String INDEX_NAME = "adminui";

  /** The required index version */
  private static final int INDEX_VERSION = 101;

  /** The document types */
  private static final String[] DOCUMENT_TYPES = new String[] { Event.DOCUMENT_TYPE, Group.DOCUMENT_TYPE,
          Series.DOCUMENT_TYPE, Theme.DOCUMENT_TYPE, "version" };

  /**
   * OSGi callback to activate this component instance.
   *
   * @param ctx
   *          the component context
   * @throws IOException
   *           if the search index cannot be initialized
   */
  @Override
  public void activate(ComponentContext ctx) throws ComponentException {
    super.activate(ctx);
    try {
      init(INDEX_NAME, INDEX_VERSION);
    } catch (Throwable t) {
      throw new ComponentException("Error initializing elastic search index", t);
    }
  }

  /**
   * OSGi callback to deactivate this component.
   *
   * @param ctx
   *          the component context
   * @throws IOException
   */
  public void deactivate(ComponentContext ctx) throws IOException {
    close();
  }

  @Override
  public String getIndexName() {
    return INDEX_NAME;
  }

  /**
   * @see org.opencastproject.matterhorn.search.impl.AbstractElasticsearchIndex#getDocumenTypes()
   */
  @Override
  public String[] getDocumenTypes() {
    return DOCUMENT_TYPES;
  }

  /**
   * Returns all the known terms for a field (aka facets).
   *
   * @param field
   *          the field name
   * @param types
   *          an optional array of document types
   * @return the list of terms
   */
  private List<String> getTermsForField(String field, Option<String[]> types) {
    final String facetName = "terms";
    TermsBuilder aggBuilder = AggregationBuilders.terms(facetName).field(field);
    SearchRequestBuilder search = getSearchClient().prepareSearch(INDEX_NAME).addAggregation(aggBuilder);

    if (types.isSome())
      search = search.setTypes(types.get());

    SearchResponse response = search.execute().actionGet();

    List<String> terms = new ArrayList<String>();
    Terms aggs = response.getAggregations().get(facetName);

    for (Bucket bucket : aggs.getBuckets()) {
      terms.add(bucket.getKey());
    }

    return terms;
  }

  /**
   * Returns all the known event locations.
   *
   * @return a list of event locations
   */
  @Override
  public List<String> getEventLocations() {
    return getTermsForField(EventIndexSchema.LOCATION, Option.some(new String[] { Event.DOCUMENT_TYPE }));
  }

  /**
   * Returns all the known event subjects.
   *
   * @return a list of event subjects
   */
  @Override
  public List<String> getEventSubjects() {
    return getTermsForField(EventIndexSchema.SUBJECT, Option.some(new String[] { Event.DOCUMENT_TYPE }));
  }

  /**
   * Returns all the known event contributors.
   *
   * @return a list of contributors
   */
  @Override
  public List<String> getEventContributors() {
    return getTermsForField(EventIndexSchema.CONTRIBUTOR, Option.some(new String[] { Event.DOCUMENT_TYPE }));
  }

  /**
   * Returns all the known event presenters
   *
   * @return a list of presenters
   */
  @Override
  public List<String> getEventPresenters() {
    return getTermsForField(EventIndexSchema.PRESENTER, Option.some(new String[] { Event.DOCUMENT_TYPE }));
  }

  /**
   * Returns all the known theme names
   *
   * @return a list of names
   */
  @Override
  public List<String> getThemeNames() {
    return getTermsForField(ThemeIndexSchema.NAME, Option.some(new String[] { Theme.DOCUMENT_TYPE }));
  }

}
