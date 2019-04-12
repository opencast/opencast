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
package org.opencastproject.workflow.impl.jpa;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.UserParser;
import org.opencastproject.workflow.api.WorkflowConfigurationSetImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstancesListImpl;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowParsingException;

import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlAttribute;

@Entity(name = "Workflow")
@Access(AccessType.FIELD)
@Table(name = "oc_workflow")
@NamedQueries({
        @NamedQuery(name = "Workflow.all", query = "SELECT w FROM Workflow w order by w.dateCreated"),
        @NamedQuery(name = "Workflow.byId", query = "SELECT w FROM Workflow w where w.id = :id "
                + "and w.organizationId = :organizationId"),
        @NamedQuery(name = "Workflow.toCleanup", query = "SELECT w FROM Workflow w where w.state = :state "
                + "and w.organizationId = :organizationId and w.dateCreated < :dateCreated"),
        @NamedQuery(name = "Workflow.count", query = "SELECT COUNT(w) FROM Workflow w where "
                + "(:state is null or w.state = :state) "
                + "and (:currentOperation is null or w.currentOperation = :currentOperation) "
                + "and w.organizationId = :organizationId"),

        // for media packages
        @NamedQuery(name = "Workflow.byMediaPackage", query = "SELECT w FROM Workflow w where "
                + "w.mediaPackageId = :mediaPackageId and w.organizationId = :organizationId order by w.dateCreated"),
        @NamedQuery(name = "Workflow.countActiveByMediaPackage", query = "SELECT COUNT(w) FROM Workflow w where "
                + "w.mediaPackageId = :mediaPackageId and w.organizationId = :organizationId and "
                + "(w.state = 'INSTANTIATED' or w.state = 'PAUSED' or w.state = 'RUNNING')"),

        // for workflow statistics
        @NamedQuery(name = "Workflow.countByState", query = "SELECT w.state, COUNT(w) FROM Workflow w where "
                + "w.organizationId = :organizationId group by w.state"),
        @NamedQuery(name = "Workflow.countByStateAndDefinition", query = "SELECT w.state, COUNT(w) FROM "
                + "Workflow w where w.template = :template and w.organizationId = :organizationId group by w.state"),
        @NamedQuery(name = "Workflow.countByStateDefinitionAndOperation", query = "SELECT w.state, COUNT(w) FROM "
                + "Workflow w where w.currentOperation = :currentOperation and w.template = :template and "
                + "w.organizationId = :organizationId group by w.state"),

})
public final class JpaWorkflow {

  @Id
  @JoinColumn(name = "id", referencedColumnName = "id")
  private long id;

  @XmlAttribute(name = "state")
  private String state;

  @Column(name = "template")
  private String template;

  @Column(name = "title")
  private String title;

  @Column(name = "description")
  private String description;

  @Column(name = "parentId")
  private Long parentId;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "creator", length = 16777215)
  private String creator;

  @Column(name = "creatorName")
  private String creatorName;

  @Column(name = "organizationId")
  private String organizationId;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "mediapackage", length = 16777215)
  private String mediaPackage;

  @Column(name = "mediaPackageId")
  private String mediaPackageId;

  @Column(name = "seriesId")
  private String seriesId;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "configurations", length = 16777215)
  private String configurations;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "operations", length = 16777215)
  private String operations;

  @Column(name = "currentOperation")
  private String currentOperation;

  @Column(name = "dateCreated")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCreated;

  @Column(name = "dateCompleted")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCompleted;

  public JpaWorkflow() {
  }

  public static JpaWorkflow from(WorkflowInstance instance) throws WorkflowParsingException {
    JpaWorkflow workflow = new JpaWorkflow();
    workflow.id = instance.getId();
    workflow.template = instance.getTemplate();
    workflow.description = instance.getDescription();
    workflow.title = instance.getTitle();
    workflow.parentId = instance.getParentId();
    workflow.organizationId = instance.getOrganizationId();
    workflow.dateCreated = instance.getDateCreated();
    workflow.dateCompleted = instance.getDateCompleted();

    workflow.state = instance.getState().toString();

    MediaPackage mp = instance.getMediaPackage();
    workflow.mediaPackage = MediaPackageParser.getAsXml(mp);
    workflow.mediaPackageId = mp.getIdentifier().toString();

    workflow.seriesId = mp.getSeries();

    workflow.creator = UserParser.toXml(instance.getCreator());
    workflow.creatorName = instance.getCreator().getUsername();

    workflow.operations = WorkflowParser.workflowOperationInstancesToXml(
            new WorkflowOperationInstancesListImpl(instance.getOperations()));
    workflow.configurations = WorkflowParser.workflowConfigurationToXml(
            new WorkflowConfigurationSetImpl(instance.getCompleteConfiguration()));
    WorkflowOperationInstance op = instance.getCurrentOperation();
    if (op != null) {
      workflow.currentOperation = op.getTemplate();
    }

    return workflow;
  }

  public WorkflowInstance toWorkflow() throws MediaPackageException, WorkflowParsingException {
    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    instance.setId(id);
    instance.setTemplate(template);
    instance.setDescription(description);
    instance.setDateCreated(dateCreated);
    instance.setDateCompleted(dateCompleted);

    MediaPackage mp = MediaPackageParser.getFromXml(mediaPackage);
    instance.setMediaPackage(mp);

    instance.setState(WorkflowInstance.WorkflowState.valueOf(state));
    instance.setTitle(title);
    instance.setParentId(parentId);

    instance.setOrganizationId(organizationId);
    instance.setCreator(UserParser.fromXml(creator));
    instance.setCompleteConfiguration(WorkflowParser.parseWorkflowConfigurationSet(configurations).get());
    instance.setOperations(WorkflowParser.parseWorkflowOperationInstancesList(operations).get());
    return instance;
  }
}
