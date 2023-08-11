/*
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

import org.opencastproject.mediapackage.MediaPackage;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/** 1:1 serialization of a {@link WorkflowInstance}. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "workflow", namespace = "http://workflow.opencastproject.org")
@XmlRootElement(name = "workflow", namespace = "http://workflow.opencastproject.org")
public class JaxbWorkflowInstance {

  @XmlAttribute()
  private long id;

  @XmlAttribute()
  private WorkflowInstance.WorkflowState state;

  @XmlElement(name = "template")
  private String template;

  @XmlElement(name = "title")
  private String title;

  @XmlElement(name = "description")
  private String description;

  @XmlElement(name = "creator-id", namespace = "http://org.opencastproject.security")
  private String creatorName;

  @XmlElement(name = "organization-id", namespace = "http://org.opencastproject.security")
  private String organizationId;

  @XmlElement
  private Date dateCreated = null;

  @XmlElement
  private Date dateCompleted = null;

  @XmlElement(name = "mediapackage", namespace = "http://mediapackage.opencastproject.org")
  private MediaPackage mediaPackage;

  @XmlElement(name = "operation")
  @XmlElementWrapper(name = "operations")
  protected List<JaxbWorkflowOperationInstance> operations;

  @XmlElement(name = "configuration")
  @XmlElementWrapper(name = "configurations")
  protected Set<JaxbWorkflowConfiguration> configurations;

  @XmlElement
  protected String mediaPackageId;

  @XmlElement
  protected String seriesId;

  /**
   * Default no-arg constructor needed by JAXB
   */
  public JaxbWorkflowInstance() {
  }

  public JaxbWorkflowInstance(WorkflowInstance workflow) {
    this();
    this.id = workflow.getId();
    this.state = workflow.getState();
    this.template = workflow.getTemplate();
    this.title = workflow.getTitle();
    this.description = workflow.getDescription();
    this.creatorName = workflow.getCreatorName();
    this.organizationId = workflow.getOrganizationId();
    this.dateCreated = workflow.getDateCreated();
    this.dateCompleted = workflow.getDateCompleted();
    this.mediaPackage = workflow.getMediaPackage();
    this.operations = workflow.getOperations()
            .stream()
            .map(JaxbWorkflowOperationInstance::new)
            .collect(Collectors.toList());
    this.configurations = workflow.getConfigurations().entrySet()
            .stream()
            .map(config -> new JaxbWorkflowConfiguration(config.getKey(), config.getValue()))
            .collect(Collectors.toSet());

    this.mediaPackageId = mediaPackage == null ? null : mediaPackage.getIdentifier().toString();
    this.seriesId = mediaPackage == null ? null : mediaPackage.getSeries();
  }

  public WorkflowInstance toWorkflowInstance() {
    return new WorkflowInstance(id, state, template, title, description, creatorName, organizationId, dateCreated,
            dateCompleted, mediaPackage,
            Optional.ofNullable(operations).orElseGet(Collections::emptyList)
                    .stream().map(JaxbWorkflowOperationInstance::toWorkflowOperationInstance).collect(Collectors.toList()),
            Optional.ofNullable(configurations).orElseGet(Collections::emptySet)
                    .stream()
                    .collect(Collectors.toMap(JaxbWorkflowConfiguration::getKey, JaxbWorkflowConfiguration::getValue)),
            mediaPackageId, seriesId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;

    if (o == null || getClass() != o.getClass())
      return false;

    JaxbWorkflowInstance jaxbWorkflow = (JaxbWorkflowInstance) o;

    return new EqualsBuilder()
            .append(id, jaxbWorkflow.id)
            .append(state, jaxbWorkflow.state)
            .append(template, jaxbWorkflow.template)
            .append(title, jaxbWorkflow.title)
            .append(description, jaxbWorkflow.description)
            .append(creatorName, jaxbWorkflow.creatorName)
            .append(organizationId, jaxbWorkflow.organizationId)
            .append(dateCreated, jaxbWorkflow.dateCreated)
            .append(dateCompleted, jaxbWorkflow.dateCompleted)
            .append(mediaPackage, jaxbWorkflow.mediaPackage)
            .append(operations, jaxbWorkflow.operations)
            .append(configurations, jaxbWorkflow.configurations)
            .append(mediaPackageId, jaxbWorkflow.mediaPackageId)
            .append(seriesId, jaxbWorkflow.seriesId)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(id)
        .append(state)
        .append(template)
        .append(title)
        .append(description)
        .append(creatorName)
        .append(organizationId)
        .append(dateCreated)
        .append(dateCompleted)
        .append(mediaPackage)
        .append(operations)
        .append(configurations)
        .append(mediaPackageId)
        .append(seriesId)
        .toHashCode();
  }
}
