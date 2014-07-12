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

import static org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState.FAILED;
import static org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState.INSTANTIATED;
import static org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState.RETRY;
import static org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState.SKIPPED;
import static org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState.SUCCEEDED;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.User;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlType(name = "workflow", namespace = "http://workflow.opencastproject.org")
@XmlRootElement(name = "workflow", namespace = "http://workflow.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public class WorkflowInstanceImpl implements WorkflowInstance {

  @XmlAttribute()
  private long id;

  @XmlAttribute()
  private WorkflowState state;

  @XmlElement(name = "template")
  private String template;

  @XmlElement(name = "title")
  private String title;

  @XmlElement(name = "description")
  private String description;

  @XmlElement(name = "parent", nillable = true)
  private Long parentId;

  @XmlJavaTypeAdapter(UserAdapter.class)
  @XmlElement(name = "creator", namespace = "http://org.opencastproject.security")
  private User creator;

  @XmlJavaTypeAdapter(OrganizationAdapter.class)
  @XmlElement(name = "organization", namespace = "http://org.opencastproject.security")
  private JaxbOrganization organization;

  @XmlElement(name = "mediapackage", namespace = "http://mediapackage.opencastproject.org")
  private MediaPackage mediaPackage;

  @XmlElement(name = "operation")
  @XmlElementWrapper(name = "operations")
  protected List<WorkflowOperationInstance> operations;

  @XmlElement(name = "configuration")
  @XmlElementWrapper(name = "configurations")
  protected Set<WorkflowConfiguration> configurations;

  @XmlElement(name = "error")
  @XmlElementWrapper(name = "errors")
  protected String[] errorMessages = new String[0];

  @XmlTransient
  protected boolean initialized = false;

  /**
   * Default no-arg constructor needed by JAXB
   */
  public WorkflowInstanceImpl() {
  }

  /**
   * Constructs a new workflow instance from the given definition, mediapackage, and optional parent workflow ID and
   * properties.
   *
   * @param def
   *          the workflow definition
   * @param mediaPackage
   *          the mediapackage
   * @param parentWorkflowId
   *          the parent workflow ID
   * @param creator
   *          the user that created this workflow instance
   * @param organization
   *          the organization
   * @param properties
   *          the properties
   */
  public WorkflowInstanceImpl(WorkflowDefinition def, MediaPackage mediaPackage, Long parentWorkflowId, User creator,
          Organization organization, Map<String, String> properties) {
    this.id = -1; // this should be set by the workflow service once the workflow is persisted
    this.title = def.getTitle();
    this.template = def.getId();
    this.description = def.getDescription();
    this.parentId = parentWorkflowId;
    this.creator = creator;
    if (organization != null)
      this.organization = JaxbOrganization.fromOrganization(organization);
    this.state = WorkflowState.INSTANTIATED;
    this.mediaPackage = mediaPackage;
    this.operations = new ArrayList<WorkflowOperationInstance>();
    this.configurations = new TreeSet<WorkflowConfiguration>();
    if (properties != null) {
      for (Entry<String, String> entry : properties.entrySet()) {
        configurations.add(new WorkflowConfigurationImpl(entry.getKey(), entry.getValue()));
      }
    }
    extend(def);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#getId()
   */
  public long getId() {
    return id;
  }

  /**
   * Sets the identifier of this workflow instance
   *
   * @param id
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#getTitle()
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title of this workflow instance
   *
   * @param title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#getCreator()
   */
  public User getCreator() {
    return creator;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#getOrganization()
   */
  @Override
  public Organization getOrganization() {
    return organization;
  }

  /**
   * @param creator
   *          the creator to set
   */
  public void setCreator(User creator) {
    this.creator = creator;
  }

  /**
   * Sets the workflow's organization.
   *
   * @param organization
   *          the organization
   */
  public void setOrganization(Organization organization) {
    if (organization == null)
      this.organization = null;
    else
      this.organization = JaxbOrganization.fromOrganization(organization);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#getDescription()
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of this workflow instance
   *
   * @param description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the parentId
   */
  public Long getParentId() {
    return parentId;
  }

  /**
   * @param parentId
   *          the parentId to set
   */
  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#getState()
   */
  public WorkflowState getState() {
    return state;
  }

  /**
   * Sets the state of this workflow instance
   *
   * @param state
   */
  public void setState(WorkflowState state) {
    this.state = state;
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
      throw new IllegalStateException("Workflow " + id + " has no operations");

    WorkflowOperationInstance currentOperation = null;

    // Handle newly instantiated workflows
    if (INSTANTIATED.equals(operations.get(0).getState()) || RETRY.equals(operations.get(0).getState())) {
      currentOperation = operations.get(0);
    } else {
      OperationState previousState = null;

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
  @Override
  public final void setOperations(List<WorkflowOperationInstance> workflowOperationInstanceList) {
    this.operations = workflowOperationInstanceList;
    init();
  }

  protected void init() {
    if (operations == null || operations.isEmpty())
      return;

    // Jaxb will lose the workflow operation's position, so we fix it here
    for (int i = 0; i < operations.size(); i++) {
      ((WorkflowOperationInstanceImpl) operations.get(i)).setPosition(i);
    }

    initialized = true;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#getMediaPackage()
   */
  public MediaPackage getMediaPackage() {
    return mediaPackage;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#setMediaPackage(org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public void setMediaPackage(MediaPackage mediaPackage) {
    this.mediaPackage = mediaPackage;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.Configurable#getConfiguration(java.lang.String)
   */
  @Override
  public String getConfiguration(String key) {
    if (key == null || configurations == null)
      return null;
    for (WorkflowConfiguration config : configurations) {
      if (config.getKey().equals(key))
        return config.getValue();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.Configurable#getConfigurationKeys()
   */
  @Override
  public Set<String> getConfigurationKeys() {
    Set<String> keys = new TreeSet<String>();
    if (configurations != null && !configurations.isEmpty()) {
      for (WorkflowConfiguration config : configurations) {
        keys.add(config.getKey());
      }
    }
    return keys;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.Configurable#removeConfiguration(java.lang.String)
   */
  @Override
  public void removeConfiguration(String key) {
    if (key == null || configurations == null)
      return;
    for (Iterator<WorkflowConfiguration> configIter = configurations.iterator(); configIter.hasNext();) {
      WorkflowConfiguration config = configIter.next();
      if (config.getKey().equals(key)) {
        configIter.remove();
        return;
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.Configurable#setConfiguration(java.lang.String, java.lang.String)
   */
  @Override
  public void setConfiguration(String key, String value) {
    if (key == null || configurations == null)
      return;
    for (WorkflowConfiguration config : configurations) {
      if (config.getKey().equals(key)) {
        ((WorkflowConfigurationImpl) config).setValue(value);
        return;
      }
    }
    // No configurations were found, so add a new one
    configurations.add(new WorkflowConfigurationImpl(key, value));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#next()
   */
  @Override
  public WorkflowOperationInstance next() {
    if (operations == null || operations.size() == 0)
      throw new IllegalStateException("Operations list must contain operations");
    if (!initialized)
      init();

    WorkflowOperationInstance currentOperation = getCurrentOperation();
    if (currentOperation == null)
      throw new IllegalStateException("Can't call next on a finished workflow");

    for (Iterator<WorkflowOperationInstance> opIter = operations.iterator(); opIter.hasNext();) {
      WorkflowOperationInstance op = opIter.next();
      if (op.equals(currentOperation) && opIter.hasNext()) {
        currentOperation.setState(OperationState.SKIPPED);
        currentOperation = opIter.next();
        return currentOperation;
      }
    }

    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#hasNext()
   */
  public boolean hasNext() {
    if (!initialized)
      init();
    if (WorkflowState.FAILED.equals(state) || WorkflowState.FAILING.equals(state)
            || WorkflowState.STOPPED.equals(state) || WorkflowState.SUCCEEDED.equals(state))
      return false;
    if (operations == null || operations.size() == 0)
      throw new IllegalStateException("operations list must contain operations");

    WorkflowOperationInstance currentOperation = getCurrentOperation();
    if (currentOperation == null)
      return true;
    return operations.lastIndexOf(currentOperation) < operations.size() - 1;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "Workflow {" + id + "}";
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Long.valueOf(id).hashCode();
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WorkflowInstance) {
      WorkflowInstance other = (WorkflowInstance) obj;
      return id == other.getId();
    }
    return false;
  }

  /**
   * Allows JAXB handling of {@link WorkflowInstance} interfaces.
   */
  static class Adapter extends XmlAdapter<WorkflowInstanceImpl, WorkflowInstance> {
    public WorkflowInstanceImpl marshal(WorkflowInstance instance) throws Exception {
      return (WorkflowInstanceImpl) instance;
    }

    public WorkflowInstance unmarshal(WorkflowInstanceImpl instance) throws Exception {
      instance.init();
      return instance;
    }
  }

  /**
   * Allows JAXB handling of {@link Organization} interfaces.
   */
  static class OrganizationAdapter extends XmlAdapter<JaxbOrganization, Organization> {
    public JaxbOrganization marshal(Organization org) throws Exception {
      if (org == null)
        return null;
      if (org instanceof JaxbOrganization)
        return (JaxbOrganization) org;
      return JaxbOrganization.fromOrganization(org);
    }

    public Organization unmarshal(JaxbOrganization org) throws Exception {
      return org;
    }
  }

  /**
   * Allows JAXB handling of {@link Organization} interfaces.
   */
  static class UserAdapter extends XmlAdapter<JaxbUser, User> {
    public JaxbUser marshal(User user) throws Exception {
      if (user == null)
        return null;
      if (user instanceof JaxbUser)
        return (JaxbUser) user;
      return JaxbUser.fromUser(user);
    }

    public User unmarshal(JaxbUser user) throws Exception {
      return user;
    }
  }

  /**
   * @return the template
   */
  public String getTemplate() {
    return template;
  }

  /**
   * @param template
   *          the template to set
   */
  public void setTemplate(String template) {
    this.template = template;
  }

  /**
   * @return the errorMessages
   */
  public String[] getErrorMessages() {
    return errorMessages;
  }

  /**
   * @param errorMessages
   *          the errorMessages to set
   */
  public void setErrorMessages(String[] errorMessages) {
    this.errorMessages = errorMessages;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#addErrorMessage(java.lang.String)
   */
  @Override
  public void addErrorMessage(String localizedMessage) {
    String[] errors = Arrays.copyOf(this.errorMessages, this.errorMessages.length + 1);
    errors[errors.length - 1] = localizedMessage;
    this.errorMessages = errors;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#extend(org.opencastproject.workflow.api.WorkflowDefinition)
   */
  @Override
  public void extend(WorkflowDefinition workflowDefinition) {
    if (workflowDefinition.getOperations().size() == 0) {
      return;
    }
    List<WorkflowOperationInstance> operations = new ArrayList<WorkflowOperationInstance>(this.operations);
    for (WorkflowOperationDefinition operationDefintion : workflowDefinition.getOperations()) {
      WorkflowOperationInstanceImpl op = new WorkflowOperationInstanceImpl(operationDefintion, -1);
      String cond = op.getExecutionCondition();
      if (cond != null && cond.startsWith("${") && cond.endsWith("}")) {
        String val = this.getConfiguration(cond.substring(2, cond.length() - 1));
        if (val != null) {
          op.setExecutionCondition(val);
        }
      }
      operations.add(op);
    }
    setOperations(operations);
    setTemplate(workflowDefinition.getId());
  }

}
