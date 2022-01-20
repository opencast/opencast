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

import static org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState.FAILED;
import static org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState.INSTANTIATED;
import static org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState.RETRY;
import static org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState.SKIPPED;
import static org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState.SUCCEEDED;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
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
 * Enitity object for storing workflows in persistence storage. Workflow ID is stored as primary key, DUBLIN_CORE field is
 * used to store serialized Dublin core and ACCESS_CONTROL field is used to store information about access control
 * rules.
 *
 */
@Entity(name = "WorkflowInstance")
@Access(AccessType.FIELD)
@Table(name = "oc_workflow", indexes = {
        @Index(name = "IX_oc_workflow_mediaPackageId", columnList = ("mediaPackageId")),
        @Index(name = "IX_oc_workflow_seriesId", columnList = ("seriesId")), })
@NamedQueries({
        @NamedQuery(
                name = "Workflow.findAll",
                query = "select w from WorkflowInstance w where w.organizationId=:organizationId order by w.dateCreated"
        ),
        @NamedQuery(
                name = "Workflow.findAllOrganizationIndependent",
                query = "select w from WorkflowInstance w"
        ),
        @NamedQuery(
                name = "Workflow.workflowById",
                query = "SELECT w FROM WorkflowInstance as w where w.workflowId=:workflowId and w.organizationId=:organizationId"
        ),

        // For media packages
        @NamedQuery(name = "Workflow.byMediaPackage", query = "SELECT w FROM WorkflowInstance w where "
                + "w.mediaPackageId = :mediaPackageId and w.organizationId = :organizationId order by w.dateCreated"),
        @NamedQuery(name = "Workflow.countActiveByMediaPackage", query = "SELECT COUNT(w) FROM WorkflowInstance w where "
                + "w.mediaPackageId = :mediaPackageId and w.organizationId = :organizationId and "
                + "(w.state = :stateInstantiated or w.state = :statePaused or w.state = :stateRunning)"),
        @NamedQuery(name = "Workflow.byMediaPackageAndOneOfThreeStates", query = "SELECT w FROM WorkflowInstance w where "
                + "w.mediaPackageId = :mediaPackageId and w.organizationId = :organizationId and "
                + "(w.state = :stateOne or w.state = :stateTwo or w.state = :stateThree) order by w.dateCreated"),
})
public class WorkflowInstance {

  /** Workflow ID, primary key */
  /** The workflow id is the same as the related job id */
  /** It is set by the workflow service when creating the instance */
  /** TODO: Figure out reasonable lengths */
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

  @Column(name = "parent", nullable = true)
  private Long parentId;

  @Column(name = "creatorId")
  private String creatorName;

  @Column(name = "organizationId")
  private String organizationId;

  @Column(name = "dateCreated")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCreated = null;

  @Column(name = "dateCompleted")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCompleted = null;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "mediaPackage", length = 16777215)
  private String mediaPackage;

//  @Lob
//  @Basic(fetch = FetchType.LAZY)
//  @Column(name = "operations", length = 16777215)
//  protected String operations;

  @OneToMany(
          mappedBy = "instance",
          cascade = CascadeType.ALL,
          orphanRemoval = true,
          fetch = FetchType.LAZY
  )
  @OrderColumn
  protected List<WorkflowOperationInstance> operations;

//  @Lob
//  @Basic(fetch = FetchType.LAZY)
//  @Column(name = "configurations", length = 16777215)
//  protected String configurations;

//  @OneToMany(
//          mappedBy = "instance",
//          cascade = CascadeType.ALL,
//          orphanRemoval = true,
//          fetch = FetchType.LAZY
//  )
//  protected Set<WorkflowConfigurationForWorkflowInstance> configurations;

  @ElementCollection
  @CollectionTable(
          name = "oc_workflow_configuration",
          joinColumns = @JoinColumn(name = "workflow_id")
  )
  @MapKeyColumn(name = "key_part", nullable = false)
  @Column(name = "value_part", nullable = false)
  protected Map<String, String> configurations;

  @Column(name = "mediaPackageId", length = 128)
  protected String mediaPackageId;

  @Column(name = "seriesId", length = 128)
  protected String seriesId;

  @Transient
  protected boolean initialized = false;

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
  public WorkflowInstance(WorkflowDefinition def, MediaPackage mediaPackage, Long parentWorkflowId, User creator,
          Organization organization, Map<String, String> properties) {
    this.workflowId = -1; // this should be set by the workflow service once the workflow is persisted
    this.title = def.getTitle();
    this.template = def.getId();
    this.description = def.getDescription();
    this.parentId = parentWorkflowId;
    this.creatorName = creator != null ? creator.getUsername() : null;
    if (organization != null)
      this.organizationId = organization.getId();
    this.state = WorkflowState.INSTANTIATED;
    this.dateCreated = new Date();
    this.mediaPackage = mediaPackage == null ? null : MediaPackageParser.getAsXml(mediaPackage);
    this.mediaPackageId = mediaPackage == null ? null : mediaPackage.getIdentifier().toString();
    this.seriesId = mediaPackage == null ? null : mediaPackage.getSeries();

    this.operations = new ArrayList<WorkflowOperationInstance>();
    try {
      extend(def);
    } catch (WorkflowParsingException e) {
      logger.error("Error: ", e);
    }

    this.configurations = new TreeMap<String, String>();
    if (properties != null) {
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        addConfiguration(entry.getKey() , entry.getValue());
      }
    }
  }

  public WorkflowInstance(
          long id,
          WorkflowState state,
          String template,
          String title,
          String description,
          Long parentId,
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
    this.parentId = parentId;
    this.creatorName = creatorName;
    this.organizationId = organizationId;
    this.dateCreated = dateCreated;
    this.dateCompleted = dateCompleted;
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

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
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
      if (mediaPackage == null) {
        return null;
      }
      return MediaPackageParser.getFromXml(mediaPackage);
    } catch (MediaPackageException e) {
      // TODO: Error handling
      logger.error("Error: ", e);
    }
    return null;
  }

  public void setMediaPackage(MediaPackage mediaPackage) {
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
    if (operations == null)
      operations = new ArrayList<WorkflowOperationInstance>();
    if (!initialized)
      init();

    return new ArrayList<WorkflowOperationInstance>(operations);
  }

  /**
   * Sets the workflow operations on this workflow instance
   *
   * @param workflowOperationInstanceList
   */
  public final void setOperations(List<WorkflowOperationInstance> workflowOperationInstanceList) {
    for (int i = 0; i < workflowOperationInstanceList.size(); i++) {
      workflowOperationInstanceList.get(i).setWorkflowInstance(this);
    }
    this.operations = workflowOperationInstanceList;
    init();
  }

  protected void init() {
    if (operations == null || operations.isEmpty())
      return;

    // Operation's position, so we fix it here
    for (int i = 0; i < operations.size(); i++) {
      operations.get(i).setPosition(i);
    }

    initialized = true;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#getCurrentOperation()
   */
  public WorkflowOperationInstance getCurrentOperation() throws IllegalStateException {
    if (!initialized)
      init();

    if (operations == null || operations.isEmpty())
      throw new IllegalStateException("Workflow " + workflowId + " has no operations");

    WorkflowOperationInstance currentOperation = null;

    // Handle newly instantiated workflows
    if (INSTANTIATED.equals(operations.get(0).getState()) || RETRY.equals(operations.get(0).getState())) {
      currentOperation = operations.get(0);
    } else {
      WorkflowOperationInstance.OperationState previousState = null;

      int position = 0;
      while (currentOperation == null && position < operations.size()) {

        WorkflowOperationInstance operation = operations.get(position);

        switch (operation.getState()) {
          case FAILED:
            break;
          case RETRY:
          case INSTANTIATED:
            if (SUCCEEDED.equals(previousState) || SKIPPED.equals(previousState) || FAILED.equals(previousState))
              currentOperation = operation;
            break;
          case PAUSED:
            currentOperation = operation;
            break;
          case RUNNING:
            currentOperation = operation;
            break;
          case SKIPPED:
            break;
          case SUCCEEDED:
            break;
          default:
            throw new IllegalStateException("Found operation in unknown state '" + operation.getState() + "'");
        }

        previousState = operation.getState();
        position++;
      }

      // If we are at the last operation and there is no more work to do, we're done
      if (operations.get(operations.size() - 1) == currentOperation) {
        switch (currentOperation.getState()) {
          case FAILED:
          case SKIPPED:
          case SUCCEEDED:
            currentOperation = null;
            break;
          case INSTANTIATED:
          case PAUSED:
          case RUNNING:
          case RETRY:
            break;
          default:
            throw new IllegalStateException("Found operation in unknown state '" + currentOperation.getState() + "'");
        }
      }

    }

    return currentOperation;
  }

  public Map<String, String> getConfigurations() {
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
//    for (WorkflowConfiguration config : configurations) {
//      if (config.getKey().equals(key))
//        return config.getValue();
//    }
//    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.Configurable#getConfigurationKeys()
   */
  public Set<String> getConfigurationKeys() {
    Set<String> keys = new TreeSet<String>();
    if (configurations != null && !configurations.isEmpty()) {
      for (String key : configurations.keySet()) {
        keys.add(key);
      }
    }
    return keys;
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
//    for (Iterator<WorkflowConfigurationForWorkflowInstance> configIter = configurations.iterator(); configIter.hasNext();) {
//      WorkflowConfigurationForWorkflowInstance config = configIter.next();
//      if (config.getKey().equals(key)) {
//        config.setWorkflowInstance(null);
//        configIter.remove();
//        return;
//      }
//    }
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
      configurations = new TreeMap<String, String>();

    // Adjust already existing values
    configurations.put(key, value);
//    for (WorkflowConfigurationForWorkflowInstance config : configurations) {
//      if (config.getKey().equals(key)) {
//        ((WorkflowConfigurationForWorkflowInstance) config).setValue(value);
//        return;
//      }
//    }
//
//    // No configurations were found, so add a new one
//    addConfiguration(key , value);
  }

  private void addConfiguration(String key, String value) {
    configurations.put(key, value);
//    WorkflowConfigurationForWorkflowInstance newConfig = new WorkflowConfigurationForWorkflowInstance(key, value);
//    newConfig.setWorkflowInstance(this);
//    configurations.add(newConfig);
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


  public void extend(WorkflowDefinition workflowDefinition) throws WorkflowParsingException {
    if (!workflowDefinition.getOperations().isEmpty()) {
      for (WorkflowOperationDefinition entry : workflowDefinition.getOperations()) {
        operations.add(new WorkflowOperationInstance(entry, -1));
      }
      setOperations(operations);

      setTemplate(workflowDefinition.getId());
    }
  }

  public void insert(WorkflowDefinition workflowDefinition, WorkflowOperationInstance after) {
    if (!workflowDefinition.getOperations().isEmpty() && after.getPosition() >= 0) {
      int offset = 0;
      for (WorkflowOperationDefinition entry : workflowDefinition.getOperations()) {
        offset++;
        operations.add(after.getPosition() + offset, new WorkflowOperationInstance(entry, -1));
      }
      setOperations(operations);
    }
  }
}


