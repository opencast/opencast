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

package org.opencastproject.workflow.api;

import static org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState.INSTANTIATED;
import static org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState.PAUSED;
import static org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState.RETRY;
import static org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState.RUNNING;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Entity object for storing workflows in persistence storage. Workflow ID is stored as primary key, DUBLIN_CORE field is
 * used to store serialized Dublin core and ACCESS_CONTROL field is used to store information about access control
 * rules.
 *
 */
@Entity(name = "WorkflowInstance")
@Access(AccessType.FIELD)
@Table(name = "oc_workflow", indexes = {
        @Index(name = "IX_oc_workflow_mediapackage_id", columnList = ("mediapackage_id")),
        @Index(name = "IX_oc_workflow_series_id", columnList = ("series_id")), })
@NamedQueries({
        @NamedQuery(
                name = "Workflow.findAll",
                query = "select w from WorkflowInstance w where w.organizationId=:organizationId order by w.dateCreated"
        ),
        @NamedQuery(
                name = "Workflow.countLatest",
                query = "SELECT COUNT(DISTINCT w.mediaPackageId) FROM WorkflowInstance w"
        ),
        @NamedQuery(
                name = "Workflow.findAllOrganizationIndependent",
                query = "select w from WorkflowInstance w"
        ),
        @NamedQuery(
                name = "Workflow.workflowById",
                query = "SELECT w FROM WorkflowInstance as w where w.workflowId=:workflowId and w.organizationId=:organizationId"
        ),
        @NamedQuery(
                name = "Workflow.getCount",
                query = "select COUNT(w) from WorkflowInstance w where w.organizationId=:organizationId "
                        + "and (:state is null or w.state = :state) "
        ),
        @NamedQuery(
                name = "Workflow.toCleanup",
                query = "SELECT w FROM WorkflowInstance w where w.state = :state "
                + "and w.dateCreated < :dateCreated and w.organizationId = :organizationId"
        ),

        // For media packages
        @NamedQuery(name = "Workflow.byMediaPackage", query = "SELECT w FROM WorkflowInstance w where "
                + "w.mediaPackageId = :mediaPackageId and w.organizationId = :organizationId order by w.dateCreated"),
        @NamedQuery(name = "Workflow.countActiveByMediaPackage", query = "SELECT COUNT(w) FROM WorkflowInstance w where "
                + "w.mediaPackageId = :mediaPackageId and w.organizationId = :organizationId and "
                + "(w.state = :stateInstantiated or w.state = :statePaused or w.state = :stateRunning "
                + "or w.state = :stateFailing)"),
        @NamedQuery(name = "Workflow.byMediaPackageAndActive", query = "SELECT w FROM WorkflowInstance w where "
                + "w.mediaPackageId = :mediaPackageId and w.organizationId = :organizationId and "
                + "(w.state = :stateInstantiated or w.state = :statePaused or w.state = :stateRunning "
                + "or w.state = :stateFailing) order by w.dateCreated"),
})
public class WorkflowInstance {

  /** Workflow ID, primary key */
  /** The workflow id is the same as the related job id */
  /** It is set by the workflow service when creating the instance */
  @Id
  @Column(name = "id")
  private long workflowId;

  @Column(name = "state", length = 128)
  private WorkflowState state;

  @Column(name = "template")
  private String template;

  @Column(name = "title")
  private String title;

  @Column(name = "description")
  private String description;

  @Column(name = "creator_id")
  private String creatorName;

  @Column(name = "organization_id")  //NB: This column definition needs to match WorkflowIndexData!
  private String organizationId;

  @Column(name = "date_created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCreated = null;

  @Column(name = "date_completed")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCompleted = null;

  @Lob
  @Column(name = "mediapackage", length = 16777215)
  private String mediaPackage;

  @Transient
  private MediaPackage mediaPackageObj;

  @OneToMany(
          mappedBy = "instance",
          cascade = CascadeType.ALL,
          orphanRemoval = true,
          fetch = FetchType.LAZY
  )
  @OrderColumn(name = "position")
  protected List<WorkflowOperationInstance> operations;

  @ElementCollection
  @CollectionTable(
          name = "oc_workflow_configuration",
          joinColumns = @JoinColumn(name = "workflow_id")
  )
  @MapKeyColumn(name = "configuration_key")
  @Lob
  @Column(name = "configuration_value")
  protected Map<String, String> configurations;

  @Column(name = "mediapackage_id", length = 128) //NB: This column definition needs to match WorkflowIndexData!
  protected String mediaPackageId;

  @Column(name = "series_id", length = 128)
  protected String seriesId;

  public enum WorkflowState {
    INSTANTIATED, RUNNING, STOPPED, PAUSED, SUCCEEDED, FAILED, FAILING;

    public boolean isTerminated() {
      switch (this) {
        case STOPPED:
        case SUCCEEDED:
        case FAILED:
          return true;
        default:
          return false;
      }
    }
    public static class Adapter extends XmlAdapter<String, WorkflowState> {

      @Override
      public String marshal(WorkflowState workflowState) {
        return workflowState == null ? null : workflowState.toString().toLowerCase();
      }

      @Override
      public WorkflowState unmarshal(String val) {
        return val == null ? null : WorkflowState.valueOf(val.toUpperCase());
      }

    }
  }

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowInstance.class);

  /**
   * Default constructor without any import.
   */
  public WorkflowInstance() {

  }

  /**
   * Constructs a new workflow instance from the given definition, mediapackage, and optional parent workflow ID and
   * properties.
   */
  public WorkflowInstance(WorkflowDefinition def, MediaPackage mediaPackage, User creator,
          Organization organization, Map<String, String> configuration) {
    this.workflowId = -1; // this should be set by the workflow service once the workflow is persisted
    this.title = def.getTitle();
    this.template = def.getId();
    this.description = def.getDescription();
    this.creatorName = creator != null ? creator.getUsername() : null;
    this.organizationId = organization != null ? organization.getId() : null;
    this.state = WorkflowState.INSTANTIATED;
    this.dateCreated = new Date();
    this.mediaPackageObj = mediaPackage;
    this.mediaPackage = mediaPackage == null ? null : MediaPackageParser.getAsXml(mediaPackage);
    this.mediaPackageId = mediaPackage == null ? null : mediaPackage.getIdentifier().toString();
    this.seriesId = mediaPackage == null ? null : mediaPackage.getSeries();

    this.operations = new ArrayList<>();
    extend(def);

    this.configurations = new HashMap<>();
    if (configuration != null) {
      this.configurations.putAll(configuration);
    }
  }

  public WorkflowInstance(
          long id,
          WorkflowState state,
          String template,
          String title,
          String description,
          String creatorName,
          String organizationId,
          Date dateCreated,
          Date dateCompleted,
          MediaPackage mediaPackage,
          List<WorkflowOperationInstance> operations,
          Map<String, String> configurations,
          String mediaPackageId,
          String seriesId) {
    this.workflowId = id;
    this.state = state;
    this.template = template;
    this.title = title;
    this.description = description;
    this.creatorName = creatorName;
    this.organizationId = organizationId;
    this.dateCreated = dateCreated;
    this.dateCompleted = dateCompleted;
    this.mediaPackageObj = mediaPackage;
    this.mediaPackage = mediaPackage == null ? null : MediaPackageParser.getAsXml(mediaPackage);
    this.operations = operations;
    this.configurations = configurations;
    this.mediaPackageId = mediaPackageId;
    this.seriesId = seriesId;
  }

  public long getId() {
    return workflowId;
  }

  public void setId(long workflowId) {
    this.workflowId = workflowId;
  }

  public WorkflowState getState() {
    return state;
  }

  public void setState(WorkflowState state) {
    if (dateCompleted == null && state.isTerminated()) {
      dateCompleted = new Date();
    }

    this.state = state;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCreatorName() {
    return creatorName;
  }

  public void setCreatorName(String creatorName) {
    this.creatorName = creatorName;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  public Date getDateCreated() {
    return dateCreated;
  }

  public void setDateCreated(Date dateCreated) {
    this.dateCreated = dateCreated;
  }

  public Date getDateCompleted() {
    return dateCompleted;
  }

  public void setDateCompleted(Date dateCompleted) {
    this.dateCompleted = dateCompleted;
  }

  public MediaPackage getMediaPackage()  {
    try {
      if (mediaPackageObj != null) {
        return mediaPackageObj;
      }
      if (mediaPackage != null) {
        mediaPackageObj = MediaPackageParser.getFromXml(mediaPackage);
        return mediaPackageObj;
      }
    } catch (MediaPackageException e) {
      logger.error("Error parsing media package in workflow instance", e);
    }
    return null;
  }

  public void setMediaPackage(MediaPackage mediaPackage) {
    this.mediaPackageObj = mediaPackage;
    this.mediaPackage = mediaPackage == null ? null : MediaPackageParser.getAsXml(mediaPackage);
    this.mediaPackageId = mediaPackage == null ? null : mediaPackage.getIdentifier().toString();
    this.seriesId = mediaPackage == null ? null : mediaPackage.getSeries();
  }

  public boolean isActive() {
    return !getState().isTerminated();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#getOperations()
   */
  public List<WorkflowOperationInstance> getOperations() {
    if (operations == null) {
      operations = new ArrayList<>();
    }
    return operations;
  }

  /**
   * Sets the workflow operations on this workflow instance
   *
   * @param workflowOperationInstanceList List of operations to set.
   */
  public final void setOperations(List<WorkflowOperationInstance> workflowOperationInstanceList) {
    for (var workflowOperationInstance : workflowOperationInstanceList) {
      workflowOperationInstance.setWorkflowInstance(this);
    }
    this.operations = workflowOperationInstanceList;
  }

  /**
   * Returns the workflow operation that is currently active or next to be executed.
   *
   * @return the current operation
   */
  public WorkflowOperationInstance getCurrentOperation() {
    logger.debug("operations: {}", operations);
    if (operations == null) {
      return null;
    }

    // Find first operation to work on. This should be the first one in state RUNNING; PAUSED, INSTANTIATED or RETRY.
    // If one is active right now, it should be RUNNING or PAUSED.
    // If none is active right now, it should be INSTANTIATED or RETRY as this should be the next one being run.
    var currentStates = List.of(RUNNING, PAUSED, RETRY, INSTANTIATED);
    for (var operation : operations) {
      if (currentStates.contains(operation.getState())) {
        logger.debug("current operation: {}", operation);
        return operation;
      }
    }
    return null;
  }

  public Map<String, String> getConfigurations() {
    if (configurations == null) {
      return Collections.emptyMap();
    }
    return configurations;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.Configurable#getConfiguration(java.lang.String)
   */
  public String getConfiguration(String key) {
    if (key == null || configurations == null)
      return null;
    return configurations.get(key);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.Configurable#getConfigurationKeys()
   */
  public Set<String> getConfigurationKeys() {
    if (configurations == null) {
      return Collections.emptySet();
    }
    return configurations.keySet();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.Configurable#removeConfiguration(java.lang.String)
   */
  public void removeConfiguration(String key) {
    if (key == null || configurations == null)
      return;
    configurations.remove(key);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.Configurable#setConfiguration(java.lang.String, java.lang.String)
   */
  public void setConfiguration(String key, String value) {
    if (key == null)
      return;
    if (configurations == null)
      configurations = new TreeMap<>();

    // Adjust already existing values
    configurations.put(key, value);
  }

  @Override
  public int hashCode() {
    return Long.valueOf(workflowId).hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WorkflowInstance) {
      WorkflowInstance other = (WorkflowInstance) obj;
      return workflowId == other.getId();
    }
    return false;
  }

  @Override
  public String toString() {
    return "Workflow {" + workflowId + "}";
  }


  public void extend(WorkflowDefinition workflowDefinition) {
    for (var operation : workflowDefinition.getOperations()) {
      var operationInstance = new WorkflowOperationInstance(operation);
      operationInstance.setWorkflowInstance(this);
      operations.add(operationInstance);
    }
    setTemplate(workflowDefinition.getId());
  }

  public void insert(WorkflowDefinition workflowDefinition, WorkflowOperationInstance after) {
    var index = operations.indexOf(after) + 1;
    for (var operation : workflowDefinition.getOperations()) {
      var operationInstance = new WorkflowOperationInstance(operation);
      operationInstance.setWorkflowInstance(this);
      operations.add(index, operationInstance);
      index++;
    }
  }
}
