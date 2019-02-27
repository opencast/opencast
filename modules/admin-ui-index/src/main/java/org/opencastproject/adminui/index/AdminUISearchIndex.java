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

package org.opencastproject.adminui.index;

import org.opencastproject.index.service.api.EventIndex;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventIndexSchema;
import org.opencastproject.index.service.impl.index.group.Group;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.impl.index.theme.Theme;
import org.opencastproject.index.service.impl.index.theme.ThemeIndexSchema;
import org.opencastproject.util.data.Option;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;

import java.io.IOException;
import java.util.List;

/**
 * A search index implementation based on ElasticSearch.
 */
public class AdminUISearchIndex extends AbstractSearchIndex implements EventIndex {

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
   * @throws ComponentException
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
   * @see org.opencastproject.matterhorn.search.impl.AbstractElasticsearchIndex#getDocumentTypes()
   */
  @Override
  public String[] getDocumentTypes() {
    return DOCUMENT_TYPES;
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

  @Override
  public List<String> getEventTechnicalPresenters() {
    return getTermsForField(EventIndexSchema.TECHNICAL_PRESENTERS, Option.some(new String[] { Event.DOCUMENT_TYPE }));
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

  /**
   * Returns all the known events' publishers
   *
   * @return a list of events' publishers
   */
  @Override
  public List<String> getEventPublishers() {
    return getTermsForField(EventIndexSchema.PUBLISHER, Option.some(new String[] { Event.DOCUMENT_TYPE }));
  }

}
