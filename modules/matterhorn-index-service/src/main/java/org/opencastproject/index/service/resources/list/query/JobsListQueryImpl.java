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

import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A fluent API for issuing Jobs queries.
 */
public class JobsListQueryImpl extends ResourceListQueryImpl {

  private static final Logger logger = LoggerFactory.getLogger(JobsListQueryImpl.class);

  /** The sort criteria for the jobs */
  public static enum SORT {
    DATE_CREATED, TITLE, SERIES_TITLE, SERIES_ID, MEDIA_PACKAGE_ID, WORKFLOW_DEFINITION_ID, CREATOR, CONTRIBUTOR, LANGUAGE, LICENSE, SUBJECT
  }

  /** The filter criteria for the jobs */
  public static enum FILTERS {
    STATUS, SERIESID, SERIESTITLE, CREATOR, CONTRIBUTOR, FROMDATE, TODATE, LANGUAGE, TEXT
  }

  /**
   * Set the criteria to use for the list sorting
   *
   * @param sortedBy
   *          Sort criteria
   * @return the query object
   */
  public ResourceListQueryImpl setSortedBy(SORT sortedBy) {
    if (sortedBy != null) {
      String value = sortedBy.toString().toLowerCase();
      this.sortBy = Option.option(value);
    }
    return this;
  }

  /**
   * Limit the results to the job with the given status.
   *
   * @param status
   *          the job status
   * @return the query object
   */
  public ResourceListQueryImpl withStatus(WorkflowState status) {
    if (status != null) {
      this.addFilter(new StringListFilter(FILTERS.STATUS.toString(), status.toString()));
    }
    return this;
  }

  /**
   * Returns the current workflow status used to filter the results
   *
   * @return the workflow state wrapped in an Option object
   */
  public Option<WorkflowState> getStatus() {
    String filterName = FILTERS.STATUS.toString();

    if (this.hasFilter(filterName)) {
      StringListFilter filter = (StringListFilter) this.getFilter(filterName);
      if (filter.getValue().isSome())
        return Option.option(WorkflowState.valueOf(filter.getValue().get()));
    }

    return Option.none();
  }

  /**
   * Limit the results to the job containing the given term
   *
   * @param text
   *          the term to use to filter
   * @return the query object
   */
  public ResourceListQueryImpl withText(String text) {
    if (text != null) {
      this.addFilter(new StringListFilter(FILTERS.TEXT.toString(), text));
    }
    return this;
  }

  /**
   * Returns the text term used to filter the results
   *
   * @return the free-text term
   */
  public Option<String> getText() {
    String filterName = FILTERS.TEXT.toString();
    if (this.hasFilter(filterName)) {
      StringListFilter filter = (StringListFilter) this.getFilter(filterName);
      return filter.getValue();
    } else {
      return Option.none();
    }
  }
}
