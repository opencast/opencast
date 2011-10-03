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

  @XmlElement(name = "description")
  private String description;

  @XmlElement(name = "published")
  private boolean published;

  @XmlElement(name = "title")
  private String title;

  @XmlElement(name = "configuration_panel")
  private String configurationPanel;

  @XmlElement(name = "operation")
  @XmlElementWrapper(name = "operations")
  private List<WorkflowOperationDefinition> operations;

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
      operations = new ArrayList<WorkflowOperationDefinition>();
    return operations;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowDefinition#get(int)
   */
  @Override
  public WorkflowOperationDefinition get(int position) throws IndexOutOfBoundsException {
    if (operations == null)
      operations = new ArrayList<WorkflowOperationDefinition>();
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
      operations = new ArrayList<WorkflowOperationDefinition>();
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
      operations = new ArrayList<WorkflowOperationDefinition>();

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
      operations = new ArrayList<WorkflowOperationDefinition>();
    return operations.remove(position);
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

  static class Adapter extends XmlAdapter<WorkflowDefinitionImpl, WorkflowDefinition> {
    public WorkflowDefinitionImpl marshal(WorkflowDefinition op) throws Exception {
      return (WorkflowDefinitionImpl) op;
    }

    public WorkflowDefinition unmarshal(WorkflowDefinitionImpl op) throws Exception {
      return op;
    }
  }

  /**
   * @return the published
   */
  public boolean isPublished() {
    return published;
  }

  /**
   * @param published
   *          the published to set
   */
  public void setPublished(boolean published) {
    this.published = published;
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

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "Workflow definition {" + id + "}";
  }

}
