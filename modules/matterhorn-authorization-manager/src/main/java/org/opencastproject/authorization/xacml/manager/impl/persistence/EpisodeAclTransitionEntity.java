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
package org.opencastproject.authorization.xacml.manager.impl.persistence;

import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.ConfiguredWorkflowRef;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import java.util.Date;

import static org.opencastproject.authorization.xacml.manager.impl.Util.createConfiguredWorkflowRef;
import static org.opencastproject.authorization.xacml.manager.impl.Util.splitConfiguredWorkflowRef;
import static org.opencastproject.util.ReflectionUtil.run;
import static org.opencastproject.util.data.Option.option;

/** JPA link of {@link org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition}. */
@Entity(name = "EpisodeAclTransition")
@Access(AccessType.FIELD)
@Table(name = "mh_acl_episode_transition",
       uniqueConstraints = @UniqueConstraint(columnNames = {"episode_id", "organization_id", "application_date"}))
@NamedQueries({
      @NamedQuery(name = "EpisodeAcl.findByTransitionId",
                  query = "SELECT e FROM EpisodeAclTransition e WHERE e.id = :id AND e.organizationId = :organizationId"),
      @NamedQuery(name = "EpisodeAcl.findByEpisodeId",
                  query = "SELECT e FROM EpisodeAclTransition e WHERE e.episodeId = :id AND e.organizationId = :organizationId ORDER BY e.applicationDate ASC") })
public class EpisodeAclTransitionEntity implements EpisodeACLTransition {
  /** Transition id, primary key */
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "pk")
  private Long id;

  /** Media package ID, */
  @Column(name = "episode_id", length = 128)
  private String episodeId;

  /** Organization ID */
  @Column(name = "organization_id", length = 128)
  protected String organizationId;

  /** Start date */
  @Column(name = "application_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date applicationDate;

  /** Managed acl id */
  @OneToOne
  @JoinColumn(name = "managed_acl_fk")
  private ManagedAclEntity managedAcl;

  /** The workflow definition id */
  @Column(name = "workflow_id", length = 128)
  private String workflowId;

  /** The workflow parameters as json */
  @Column(name = "workflow_params")
  private String workflowParams;

  /** Indicates if already applied */
  @Column(name = "done")
  private boolean done = false;

  /** No-arg constructor needed by JPA */
  public EpisodeAclTransitionEntity() {
  }

  EpisodeAclTransitionEntity update(final String episodeId,
                                    final String orgId,
                                    final Date applicationDate,
                                    final Option<ManagedAclEntity> managedAcl,
                                    final Option<ConfiguredWorkflowRef> workflow) {
    final EpisodeAclTransitionEntity self = this;
    run(EpisodeACLTransition.class, new EpisodeACLTransition() {
      @Override public String getEpisodeId() {
        self.episodeId = episodeId;
        return null;
      }

      @Override public Option<ManagedAcl> getAccessControlList() {
        self.managedAcl = managedAcl.getOrElseNull();
        return null;
      }

      @Override public boolean isDelete() {
        return false;
      }

      @Override public long getTransitionId() {
        return 0;
      }

      @Override public String getOrganizationId() {
        self.organizationId = orgId;
        return null;
      }

      @Override public Date getApplicationDate() {
        self.applicationDate = applicationDate;
        return null;
      }

      @Override public Option<ConfiguredWorkflowRef> getWorkflow() {
        final Tuple<Option<String>, Option<String>> s = splitConfiguredWorkflowRef(workflow);
        self.workflowId = s.getA().getOrElseNull();
        self.workflowParams = s.getB().getOrElseNull();
        return null;
      }

      @Override public boolean isDone() {
        self.done = done;
        return false;
      }
    });
    return self;
  }

  void setDone(boolean done) {
    this.done = done;
  }

  @Override public String getEpisodeId() {
    return episodeId;
  }

  @Override public Option<ManagedAcl> getAccessControlList() {
    return option((ManagedAcl) managedAcl);
  }

  @Override public boolean isDelete() {
    return managedAcl == null;
  }

  @Override public long getTransitionId() {
    return id;
  }

  @Override public String getOrganizationId() {
    return organizationId;
  }

  @Override public Date getApplicationDate() {
    return applicationDate;
  }

  @Override public Option<ConfiguredWorkflowRef> getWorkflow() {
    return createConfiguredWorkflowRef(option(workflowId), option(workflowParams));
  }

  @Override public boolean isDone() {
    return done;
  }

  @Override
  public String toString() {
    return "{TransitionId=" + id + ", episodeId=" + episodeId + ", orgId=" + organizationId + ", applicationDate="
            + applicationDate + "}";
  }

}
