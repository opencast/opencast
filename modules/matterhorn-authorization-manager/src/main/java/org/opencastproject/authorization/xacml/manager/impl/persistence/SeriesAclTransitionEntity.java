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

package org.opencastproject.authorization.xacml.manager.impl.persistence;

import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.api.SeriesACLTransition;
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

@Entity(name = "SeriesAclTransition")
@Access(AccessType.FIELD)
@Table(name = "mh_acl_series_transition",
       uniqueConstraints = @UniqueConstraint(columnNames = {"series_id", "organization_id", "application_date"}))
@NamedQueries({
      @NamedQuery(name = "SeriesAcl.findByTransitionId", query = "SELECT s FROM SeriesAclTransition s WHERE s.id = :id AND s.organizationId = :organizationId"),
      @NamedQuery(name = "SeriesAcl.findBySeriesId", query = "SELECT s FROM SeriesAclTransition s WHERE s.seriesId = :id AND s.organizationId = :organizationId ORDER BY s.applicationDate ASC") })
public class SeriesAclTransitionEntity implements SeriesACLTransition {

  /** Transition ID, primary key */
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "pk", length = 128)
  private Long id;

  /** Series ID */
  @Column(name = "series_id", length = 128)
  private String seriesId;

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
  @Column(name = "workflow_id")
  private String workflowId;

  /** The workflow parameters as json */
  @Column(name = "workflow_params")
  private String workflowParams;

  /** Indicates to override the ACL's */
  @Column(name = "override")
  private boolean override;

  /** Indicates if already applied */
  @Column(name = "done")
  private boolean done = false;

  /** No-arg constructor needed by JPA */
  public SeriesAclTransitionEntity() {
  }

  SeriesAclTransitionEntity update(final String seriesId,
                                   final String orgId,
                                   final Date applicationDate,
                                   final ManagedAclEntity managedAcl,
                                   final Option<ConfiguredWorkflowRef> workflow,
                                   final boolean override) {
    final SeriesAclTransitionEntity self = this;
    run(SeriesACLTransition.class, new SeriesACLTransition() {
      @Override public String getSeriesId() {
        self.seriesId = seriesId;
        return null;
      }

      @Override public ManagedAcl getAccessControlList() {
        self.managedAcl = managedAcl;
        return null;
      }

      @Override public boolean isOverride() {
        self.override = override;
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
    return this;
  }

  void setDone(boolean done) {
    this.done = done;
  }

  @Override public String getSeriesId() {
    return seriesId;
  }

  @Override public String getOrganizationId() {
    return organizationId;
  }

  @Override public Date getApplicationDate() {
    return applicationDate;
  }

  @Override public ManagedAcl getAccessControlList() {
    return managedAcl;
  }

  @Override public long getTransitionId() {
    return id;
  }

  @Override public Option<ConfiguredWorkflowRef> getWorkflow() {
    return createConfiguredWorkflowRef(option(workflowId), option(workflowParams));
  }

  @Override public boolean isOverride() {
    return override;
  }

  @Override public boolean isDone() {
    return done;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "{TransitionId=" + id + ", seriesId=" + seriesId + ", orgId=" + organizationId + ", applicationDate="
            + applicationDate + "}";
  }
}
