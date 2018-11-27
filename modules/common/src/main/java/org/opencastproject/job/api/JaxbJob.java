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

package org.opencastproject.job.api;

import static java.util.Collections.unmodifiableList;

import com.entwinemedia.fn.Fn;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/** 1:1 serialization of a {@link Job}. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "job", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "job", namespace = "http://job.opencastproject.org")
public class JaxbJob {

  @XmlAttribute
  private long id;

  @XmlElement(name = "creator")
  private String creator;

  @XmlElement(name = "organization")
  private String organization;

  @XmlAttribute
  private long version;

  @XmlAttribute(name = "type")
  private String jobType;

  @XmlElement(name = "url")
  private URI uri;

  @XmlElement
  private String operation;

  @XmlElement(name = "arg")
  @XmlElementWrapper(name = "args")
  private List<String> arguments;

  @XmlElement
  private String createdHost;

  @XmlElement
  private String processingHost;

  @XmlAttribute
  private Job.Status status;

  @XmlElement
  private Date dateCreated;

  @XmlElement
  private Date dateStarted;

  @XmlElement
  private Date dateCompleted;

  @XmlElement
  private Long parentJobId;

  @XmlElement
  private Long rootJobId;

  @XmlElement
  private Long queueTime;

  @XmlElement
  private Long runTime;

  @XmlElement
  private String payload;

  @XmlElement
  private boolean dispatchable;

  @XmlElement(name = "jobLoad")
  private Float jobLoad;

  /** Default constructor needed by jaxb */
  public JaxbJob() {
  }

  public JaxbJob(Job job) {
    this();
    this.id = job.getId();
    this.dateCompleted = job.getDateCompleted();
    this.dateCreated = job.getDateCreated();
    this.dateStarted = job.getDateStarted();
    this.queueTime = job.getQueueTime();
    this.runTime = job.getRunTime();
    this.version = job.getVersion();
    this.payload = job.getPayload();
    this.processingHost = job.getProcessingHost();
    this.createdHost = job.getCreatedHost();
    this.id = job.getId();
    this.jobType = job.getJobType();
    this.operation = job.getOperation();
    if (job.getArguments() != null)
      this.arguments = unmodifiableList(job.getArguments());
    this.status = job.getStatus();
    this.parentJobId = job.getParentJobId();
    this.rootJobId = job.getRootJobId();
    this.dispatchable = job.isDispatchable();
    this.uri = job.getUri();
    this.creator = job.getCreator();
    this.organization = job.getOrganization();
    this.jobLoad = job.getJobLoad();
  }

  public Job toJob() {
    return new JobImpl(id, creator, organization, version, jobType, operation, arguments, status, createdHost,
            processingHost, dateCreated, dateStarted, dateCompleted, queueTime, runTime, payload, parentJobId,
            rootJobId, dispatchable, uri, jobLoad);
  }

  public static Fn<JaxbJob, Job> fnToJob() {
    return new Fn<JaxbJob, Job>() {
      @Override
      public Job apply(JaxbJob jaxbJob) {
        return jaxbJob.toJob();
      }
    };
  }

  public static Fn<Job, JaxbJob> fnFromJob() {
    return new Fn<Job, JaxbJob>() {
      @Override
      public JaxbJob apply(Job job) {
        return  new JaxbJob(job);
      }
    };
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;

    if (o == null || getClass() != o.getClass())
      return false;

    JaxbJob jaxbJob = (JaxbJob) o;

    return new EqualsBuilder().append(id, jaxbJob.id).append(version, jaxbJob.version)
            .append(dispatchable, jaxbJob.dispatchable).append(creator, jaxbJob.creator)
            .append(organization, jaxbJob.organization).append(jobType, jaxbJob.jobType).append(uri, jaxbJob.uri)
            .append(operation, jaxbJob.operation).append(arguments, jaxbJob.arguments)
            .append(createdHost, jaxbJob.createdHost).append(processingHost, jaxbJob.processingHost)
            .append(status, jaxbJob.status).append(dateCreated, jaxbJob.dateCreated)
            .append(dateStarted, jaxbJob.dateStarted).append(dateCompleted, jaxbJob.dateCompleted)
            .append(parentJobId, jaxbJob.parentJobId).append(rootJobId, jaxbJob.rootJobId)
            .append(queueTime, jaxbJob.queueTime).append(runTime, jaxbJob.runTime)
            .append(payload, jaxbJob.payload).append(jobLoad, jaxbJob.jobLoad)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(id).append(creator).append(organization).append(version).append(jobType)
            .append(uri).append(operation).append(arguments).append(createdHost).append(processingHost).append(status)
            .append(dateCreated).append(dateStarted).append(dateCompleted).append(parentJobId).append(rootJobId)
            .append(queueTime).append(runTime).append(payload).append(dispatchable).append(jobLoad)
            .toHashCode();
  }
}
