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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * The search result represents a set of result items that has been compiled as a result for a search operation.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "workflows", namespace = "http://workflow.opencastproject.org")
public class WorkflowSetImpl implements WorkflowSet {

  /** A list of search items. */
  @XmlElement(name = "workflow")
  private List<WorkflowInstance> resultSet = null;

  /** The pagination offset. */
  @XmlAttribute(name = "startPage")
  private long startPage;

  /** The pagination limit. */
  @XmlAttribute(name = "count")
  private long pageSize;

  /** The search time in milliseconds */
  @XmlAttribute(name = "searchTime")
  private long searchTime;

  /** The total number of results without paging */
  @XmlAttribute(name = "totalCount")
  private long totalCount;

  /**
   * A no-arg constructor needed by JAXB
   */
  public WorkflowSetImpl() {
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowSet#getItems()
   */
  @Override
  public WorkflowInstance[] getItems() {
    return resultSet == null || resultSet.size() == 0 ? new WorkflowInstance[0] : resultSet
            .toArray(new WorkflowInstance[resultSet.size()]);
  }

  /**
   * Adds an item to the result set.
   * 
   * @param item
   *          the item to add
   */
  public void addItem(WorkflowInstance item) {
    if (item == null)
      throw new IllegalArgumentException("Parameter item cannot be null");
    if (resultSet == null)
      resultSet = new ArrayList<WorkflowInstance>();
    resultSet.add((WorkflowInstanceImpl) item);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowSet#size()
   */
  @Override
  public long size() {
    return resultSet != null ? resultSet.size() : 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowSet#getStartPage()
   */
  public long getStartPage() {
    return startPage;
  }

  /**
   * Set the start page offset.
   * 
   * @param startPage
   *          The start page offset.
   */
  public void setStartPage(long startPage) {
    this.startPage = startPage;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowSet#getPageSize()
   */
  @Override
  public long getPageSize() {
    return pageSize;
  }

  /**
   * Set the pageSize.
   * 
   * @param pageSize
   *          The pageSize.
   */
  public void setPageSize(long pageSize) {
    this.pageSize = pageSize;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowSet#getSearchTime()
   */
  public long getSearchTime() {
    return searchTime;
  }

  /**
   * Set the search time.
   * 
   * @param searchTime
   *          The time in ms.
   */
  public void setSearchTime(long searchTime) {
    this.searchTime = searchTime;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowSet#getTotalCount()
   */
  @Override
  public long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(long totalCount) {
    this.totalCount = totalCount;
  }

  static class Adapter extends XmlAdapter<WorkflowSetImpl, WorkflowSet> {
    public WorkflowSetImpl marshal(WorkflowSet set) throws Exception {
      return (WorkflowSetImpl) set;
    }

    public WorkflowSet unmarshal(WorkflowSetImpl set) throws Exception {
      return set;
    }
  }

}
