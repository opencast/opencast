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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A JAXB-annotated implementation of {@link WorkflowDefinition}
 */
@XmlType(name = "definition", namespace = "http://workflow.opencastproject.org")
@XmlRootElement(name = "definition", namespace = "http://workflow.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowDefinitionImpl implements WorkflowDefinition {

  /**
   * Constructor to be used by JAXB only.
   */
  public WorkflowDefinitionImpl() {
  }

  @XmlID
  @XmlElement(name = "id")
  private String id;

  @XmlElement(name = "title")
  private String title;

  @XmlElement(name = "organization")
  private String organization;

  @XmlElementWrapper(name = "tags")
  @XmlElement(name = "tag")
  protected SortedSet<String> tags = new TreeSet<>();

  @XmlElementWrapper(name = "roles")
  @XmlElement(name = "role")
  protected SortedSet<String> roles = new TreeSet<>();

  @XmlElement(name = "description")
  private String description;

  @XmlElement(name = "displayOrder")
  private int displayOrder = 0;

  @XmlElement(name = "configuration_panel")
  private String configurationPanel;

  @XmlElement(name = "operation")
  @XmlElementWrapper(name = "operations")
  private List<WorkflowOperationDefinition> operations;

  @XmlElement(name = "state-mapping")
  @XmlElementWrapper(name = "state-mappings")
  private Set<WorkflowStateMapping> stateMappings;

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowDefinition#getId()
   */
  public String getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowDefinition#setId(java.lang.String)
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowDefinition#getDescription()
   */
  public String getDescription() {
    return description;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowDefinition#setDescription(java.lang.String)
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowDefinition#getDisplayOrder()
   */
  public int getDisplayOrder() {
    return displayOrder;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowDefinition#setDisplayOrder(int)
   */
  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }

  /**
   * Sets the configuration panel for this workflow.
   *
   * @param panelXML
   *          the xml for the configuration panel
   */
  public void setConfigurationPanel(String panelXML) {
    this.configurationPanel = panelXML;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowDefinition#getConfigurationPanel()
   */
  public String getConfigurationPanel() {
    return this.configurationPanel;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowDefinition#getOperations()
   */
  public List<WorkflowOperationDefinition> getOperations() {
    if (operations == null)
      operations = new ArrayList<>();
    return operations;
  }

  @Override
  public Set<WorkflowStateMapping> getStateMappings() {
    if (stateMappings == null) {
      stateMappings = new HashSet<>();
    }
    return stateMappings;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowDefinition#get(int)
   */
  @Override
  public WorkflowOperationDefinition get(int position) throws IndexOutOfBoundsException {
    if (operations == null)
      operations = new ArrayList<>();
    if (position < 0 || position >= operations.size())
      throw new IndexOutOfBoundsException();
    return operations.get(position);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowDefinition#add(org.opencastproject.workflow.api.WorkflowOperationDefinition)
   */
  @Override
  public void add(WorkflowOperationDefinition operation) {
    if (operations == null)
      operations = new ArrayList<>();
    add(operation, this.operations.size());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowDefinition#add(org.opencastproject.workflow.api.WorkflowOperationDefinition,
   *      int)
   */
  @Override
  public void add(WorkflowOperationDefinition operation, int position) {
    if (operations == null)
      operations = new ArrayList<>();

    if (operation == null)
      throw new IllegalArgumentException("Workflow operation cannot be null");
    if (position < 0 || position > operations.size())
      throw new IndexOutOfBoundsException();

    if (position == operations.size())
      operations.add(operation);
    else
      operations.add(position, operation);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowDefinition#remove(int)
   */
  @Override
  public WorkflowOperationDefinition remove(int position) throws IndexOutOfBoundsException {
    if (operations == null)
      operations = new ArrayList<>();
    return operations.remove(position);
  }

  /**
   * @see org.opencastproject.workflow.api.WorkflowDefinition#addTag(String)
   */
  @Override
  public void addTag(String tag) {
    if (tag == null)
      throw new IllegalArgumentException("Tag must not be null");
    tags.add(tag);
  }

  /**
   * @see org.opencastproject.workflow.api.WorkflowDefinition#removeTag(String)
   */
  @Override
  public void removeTag(String tag) {
    if (tag == null)
      return;
    tags.remove(tag);
  }

  /**
   * @see org.opencastproject.workflow.api.WorkflowDefinition#containsTag(String)
   */
  @Override
  public boolean containsTag(String tag) {
    if (tag == null || tags == null)
      return false;
    return tags.contains(tag);
  }

  /**
   * @see org.opencastproject.workflow.api.WorkflowDefinition#containsTag(Collection)
   */
  @Override
  public boolean containsTag(Collection<String> tags) {
    if (tags.size() == 0)
      return true;
    for (String tag : tags) {
      if (containsTag(tag))
        return true;
    }
    return false;
  }

  /**
   * @see org.opencastproject.workflow.api.WorkflowDefinition#getTags()
   */
  @Override
  public String[] getTags() {
    return tags.toArray(new String[0]);
  }

  /**
   * @see org.opencastproject.workflow.api.WorkflowDefinition#clearTags()
   */
  @Override
  public void clearTags() {
    if (tags != null)
      tags.clear();
  }

  /**
   * Since we are posting workflow definitions as one post parameter in a multi-parameter post, we can not rely on
   * "automatic" JAXB deserialization. We therefore need to provide a static valueOf(String) method to transform an XML
   * string into a WorkflowDefinition.
   *
   * @param xmlString
   *          The xml describing the workflow
   * @return A {@link WorkflowDefinitionImpl} instance based on xmlString
   * @throws Exception
   *           If there is a problem marshalling the {@link WorkflowDefinitionImpl} from XML.
   */
  public static WorkflowDefinitionImpl valueOf(String xmlString) throws Exception {
    return (WorkflowDefinitionImpl) WorkflowParser.parseWorkflowDefinition(xmlString);
  }

  @Override
  public int compareTo(WorkflowDefinition workflowDefinition) {

    if (workflowDefinition == null) {
      throw new NullPointerException("WorkflowDefinition for comparison can't be null");
    }

    // nullsafe comparison where null is lesser than non-null
    // workflows with null title probably aren't for displaying anyway
    return StringUtils.compareIgnoreCase(this.getTitle(), workflowDefinition.getTitle());
  }

  static class Adapter extends XmlAdapter<WorkflowDefinitionImpl, WorkflowDefinition> {
    public WorkflowDefinitionImpl marshal(WorkflowDefinition op) {
      return (WorkflowDefinitionImpl) op;
    }

    public WorkflowDefinition unmarshal(WorkflowDefinitionImpl op) {
      return op;
    }
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param title
   *          the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public String getOrganization() {
    return organization;
  }

  @Override
  public Collection<String> getRoles() {
    if (roles == null) {
      return Collections.emptySet();
    }
    return roles;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  public String toString() {
    if (organization != null) {
      return "Workflow definition {" + id + "/" + organization + "}";
    }
    return "Workflow definition {" + id + "}";
  }

}
