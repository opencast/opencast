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
package org.opencastproject.workflow.api;

import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A fluent API for issuing WorkflowInstance queries. This object is thread unsafe.
 */
public class WorkflowQuery {
  protected String id;
  protected long count;
  protected long startPage;
  protected String text;
  protected String seriesTitle;
  protected String seriesId;
  protected String mediaPackageId;
  protected String workflowDefinitionId;
  protected Date fromDate;
  protected Date toDate;
  protected String creator;
  protected String contributor;
  protected String language;
  protected String license;
  protected String title;
  protected String subject;
  protected Sort sort = Sort.DATE_CREATED;
  protected boolean sortAscending = true;

  public enum Sort {
    DATE_CREATED, TITLE, SERIES_TITLE, SERIES_ID, MEDIA_PACKAGE_ID, WORKFLOW_DEFINITION_ID, CREATOR, CONTRIBUTOR, LANGUAGE, LICENSE, SUBJECT
  }

  /**
   * The list of current operation terms that have been added to this query.
   */
  protected List<QueryTerm> currentOperationTerms = new ArrayList<QueryTerm>();

  /**
   * The list of state terms that have been added to this query.
   */
  protected List<QueryTerm> stateTerms = new ArrayList<QueryTerm>();

  public WorkflowQuery() {
  }

  public WorkflowQuery withId(String id) {
    this.id = id;
    return this;
  }

  /** Include a limit for the number of items to return in the result */
  public WorkflowQuery withCount(long count) {
    this.count = count;
    return this;
  }

  /** Include a paging offset for the items returned */
  public WorkflowQuery withStartPage(long startPage) {
    this.startPage = startPage;
    return this;
  }

  /** Limit results to workflow instances matching a free text search */
  public WorkflowQuery withText(String text) {
    if (StringUtils.isNotBlank(text))
      this.text = text;
    return this;
  }

  /**
   * Limit results to workflow instances in a specific state. This method overrides and will be overridden by future
   * calls to {@link #withoutState(String)}
   *
   * @param state
   *          the workflow state
   * @return this query
   */
  public WorkflowQuery withState(WorkflowState state) {
    if (state != null)
      stateTerms.add(new QueryTerm(state.toString(), true));
    return this;
  }

  /**
   * Limit results to workflow instances not in a specific state. This method overrides and will be overridden by future
   * calls to {@link #withState(String)}
   *
   * @param state
   *          the workflow state
   * @return this query
   */
  public WorkflowQuery withoutState(WorkflowState state) {
    if (state != null)
      stateTerms.add(new QueryTerm(state.toString(), false));
    return this;
  }

  /**
   * Limit results to workflow instances with a specific series title
   *
   * @param seriesTitle
   *          the series title
   */
  public WorkflowQuery withSeriesTitle(String seriesTitle) {
    if (StringUtils.isNotBlank(seriesTitle))
      this.seriesTitle = seriesTitle;
    return this;
  }

  /**
   * Limit results to workflow instances for a specific series
   *
   * @param seriesId
   *          the series identifier
   */
  public WorkflowQuery withSeriesId(String seriesId) {
    if (StringUtils.isNotBlank(seriesId))
      this.seriesId = seriesId;
    return this;
  }

  /**
   * Limit results to workflow instances for a specific media package
   *
   * @param mediaPackageId
   *          the media package identifier
   */
  public WorkflowQuery withMediaPackage(String mediaPackageId) {
    if (StringUtils.isNotBlank(mediaPackageId))
      this.mediaPackageId = mediaPackageId;
    return this;
  }

  /**
   * Limit results to workflow instances that are currently handling the specified operation. This method overrides and
   * will be overridden by future calls to {@link #withoutCurrentOperation(String)}
   *
   * @param currentOperation
   *          the current operation
   * @return this query
   */
  public WorkflowQuery withCurrentOperation(String currentOperation) {
    if (StringUtils.isNotBlank(currentOperation))
      currentOperationTerms.add(new QueryTerm(currentOperation, true));
    return this;
  }

  /**
   * Limit results to workflow instances to those that are not currently in the specified operation. This method
   * overrides and will be overridden by future calls to {@link #withCurrentOperation(String)}
   *
   * @param currentOperation
   *          the current operation
   * @return this query
   */
  public WorkflowQuery withoutCurrentOperation(String currentOperation) {
    if (StringUtils.isNotBlank(currentOperation))
      currentOperationTerms.add(new QueryTerm(currentOperation, false));
    return this;
  }

  /**
   * Limit results to workflow instances with a specific workflow definition.
   *
   * @param workflowDefinitionId
   *          the workflow identifier
   */
  public WorkflowQuery withWorkflowDefintion(String workflowDefinitionId) {
    if (StringUtils.isNotBlank(workflowDefinitionId))
      this.workflowDefinitionId = workflowDefinitionId;
    return this;
  }

  /**
   * Limit the results to workflow instances with a creation date starting with <code>fromDate</code>.
   *
   * @param fromDate
   *          the starting date
   */
  public WorkflowQuery withDateAfter(Date fromDate) {
    this.fromDate = fromDate;
    return this;
  }

  /**
   * Limit the results to workflow instances with a creation date no later than <code>fromDate</code>.
   *
   * @param toDate
   *          the ending date
   */
  public WorkflowQuery withDateBefore(Date toDate) {
    this.toDate = toDate;
    return this;
  }

  /**
   * Limit results to workflow instances with a specific mediapackage creator.
   *
   * @param creator
   *          the mediapackage creator
   */
  public WorkflowQuery withCreator(String creator) {
    if (StringUtils.isNotBlank(creator))
      this.creator = creator;
    return this;
  }

  /**
   * Limit results to workflow instances with a specific mediapackage contributor.
   *
   * @param contributor
   *          the mediapackage contributor
   */
  public WorkflowQuery withContributor(String contributor) {
    if (StringUtils.isNotBlank(contributor))
      this.contributor = contributor;
    return this;
  }

  /**
   * Limit results to workflow instances with a specific mediapackage language.
   *
   * @param language
   *          the mediapackage language
   */
  public WorkflowQuery withLanguage(String language) {
    if (StringUtils.isNotBlank(language))
      this.language = language;
    return this;
  }

  /**
   * Limit results to workflow instances with a specific mediapackage license.
   *
   * @param license
   *          the mediapackage license
   */
  public WorkflowQuery withLicense(String license) {
    if (StringUtils.isNotBlank(license))
      this.license = license;
    return this;
  }

  /**
   * Limit results to workflow instances with a specific mediapackage title.
   *
   * @param title
   *          the mediapackage title
   */
  public WorkflowQuery withTitle(String title) {
    if (StringUtils.isNotBlank(title))
      this.title = title;
    return this;
  }

  /**
   * Limit results to workflow instances with a specific mediapackage subject.
   *
   * @param subject
   *          the mediapackage subject
   */
  public WorkflowQuery withSubject(String subject) {
    if (StringUtils.isNotBlank(subject))
      this.subject = subject;
    return this;
  }

  /**
   * Sort the results by the specified field in ascending order.
   *
   * @param sort
   *          the sort field
   */
  public WorkflowQuery withSort(Sort sort) {
    return withSort(sort, true);
  }

  /**
   * Sort the results by the specified field, either ascending or descending.
   *
   * @param sort
   *          the sort field
   * @param ascending
   *          whether to sort ascending (true) or descending (false)
   */
  public WorkflowQuery withSort(Sort sort, boolean ascending) {
    this.sort = sort;
    this.sortAscending = ascending;
    return this;
  }

  /**
   * Return the field to use in sorting the results of the query.
   *
   * @return the sort field
   */
  public Sort getSort() {
    return sort;
  }

  /**
   * Return whether to sort the results in ascending order.
   *
   * @return whether the search results should be sorted in ascending order
   */
  public boolean isSortAscending() {
    return sortAscending;
  }

  public String getId() {
    return id;
  }

  /**
   * Returns the number of result items to return.
   *
   * @return the number of result items
   */
  public long getCount() {
    return count;
  }

  /**
   * Returns the number of the first page within the full result set.
   *
   * @return the first page
   */
  public long getStartPage() {
    return startPage;
  }

  /**
   * Returns the text that workflow instances need to match by any metadata field (fulltext).
   *
   * @return the text
   */
  public String getText() {
    return text;
  }

  /**
   * Returns the list of states that workflow instances need to match.
   *
   * @return the states
   */
  public List<QueryTerm> getStates() {
    return stateTerms;
  }

  /**
   * Returns the list of current operations that workflow instances need to match.
   *
   * @return the current operations
   */
  public List<QueryTerm> getCurrentOperations() {
    return currentOperationTerms;
  }

  /**
   * Returns the media package series identifier that workflow instances need to match.
   *
   * @return the media package series identifier
   */
  public String getSeriesId() {
    return seriesId;
  }

  /**
   * Returns the media package title that workflow instances need to match.
   *
   * @return the media package title
   */
  public String getSeriesTitle() {
    return seriesTitle;
  }

  /**
   * Returns the media package identifier that workflow instances need to match.
   *
   * @return the media package identifier
   */
  public String getMediaPackageId() {
    return mediaPackageId;
  }

  /**
   * Returns the workflow defintions that workflow instances need to match.
   *
   * @return the workflow definition identifier
   */
  public String getWorkflowDefinitionId() {
    return workflowDefinitionId;
  }

  /**
   * Returns the selection start date for workflow instances.
   *
   * @return the start date
   */
  public Date getFromDate() {
    return fromDate;
  }

  /**
   * Returns the selection end date for workflow instances.
   *
   * @return the end date
   */
  public Date getToDate() {
    return toDate;
  }

  /**
   * Returns the media package creator that workflow instances need to match.
   *
   * @return the media package creator
   */
  public String getCreator() {
    return creator;
  }

  /**
   * Returns the media package contributor that workflow instances need to match.
   *
   * @return the media package contributor
   */
  public String getContributor() {
    return contributor;
  }

  /**
   * Returns the media package language that workflow instances need to match.
   *
   * @return the media package language
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Returns the media package license that workflow instances need to match.
   *
   * @return the media package license
   */
  public String getLicense() {
    return license;
  }

  /**
   * Returns the media package title that workflow instances need to match.
   *
   * @return the media package title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Returns the media package subject that workflow instances need to match.
   *
   * @return the media package subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Returns the search terms for operations. A term can either mean to include or exclude the specified operation.
   *
   * @return the operation search terms
   */
  public List<QueryTerm> getCurrentOperationTerms() {
    return currentOperationTerms;
  }

  /**
   * Returns the search terms for workflow states. A term can either mean to include or exclude the specified state.
   *
   * @return the state search terms
   */
  public List<QueryTerm> getStateTerms() {
    return stateTerms;
  }

  /**
   * A tuple of a query value and whether this search term should be included or excluded from the search results.
   */
  public static class QueryTerm {

    private String value = null;
    private boolean include = false;

    /** Constructs a new query term */
    public QueryTerm(String value, boolean include) {
      this.value = value;
      this.include = include;
    }

    /**
     * @return the value
     */
    public String getValue() {
      return value;
    }

    /**
     * @return whether this query term is to be excluded
     */
    public boolean isInclude() {
      return include;
    }
  }

}
