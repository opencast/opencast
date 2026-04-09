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
package org.opencastproject.speechtotext.async.persistence;

import org.opencastproject.job.jpa.JpaJob;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlAttribute;

@Entity(name = "SpeechToTextControl")
//@formatter:off
@Table(name = "oc_speech_to_text_control", indexes = {
        @Index(name = "IX_oc_stt_status", columnList = ("status")),
        @Index(name = "IX_oc_stt_workflow_id", columnList = ("workflow_id")),
        @Index(name = "IX_oc_stt_job", columnList = ("job"))
})
@NamedQueries({
        @NamedQuery(name = "SpeechToTextControl.findByWorkflowId",
            query = "SELECT stt FROM SpeechToTextControl stt "
                + "WHERE stt.workflowId = :workflowId"),
        @NamedQuery(name = "SpeechToTextControl.findDistinctWorkflowIdByStatus",
            query = "SELECT DISTINCT stt.workflowId FROM SpeechToTextControl stt "
                + "WHERE stt.status IN :status"),
        @NamedQuery(name = "SpeechToTextControl.findDistinctWorkflowIdByMediaPackage",
        query = "SELECT DISTINCT stt.workflowId FROM SpeechToTextControl stt "
            + "WHERE stt.mediaPackageId = :mpId"),
        @NamedQuery(name = "SpeechToTextControl.findByJob",
            query = "SELECT stt FROM SpeechToTextControl stt WHERE stt.job = :job"),
        @NamedQuery(name = "SpeechToTextControl.findByStatus",
            query = "SELECT stt FROM SpeechToTextControl stt WHERE stt.status IN :status"),
        @NamedQuery(name = "SpeechToTextControl.updateStatusByJob",
            query = "UPDATE SpeechToTextControl stt SET stt.status = :status "
                + "WHERE stt.job IN :jobs"),
        @NamedQuery(name = "SpeechToTextControl.deleteByWorkflow",
        query = "DELETE FROM SpeechToTextControl stt WHERE stt.workflowId = :workflowId"),
        @NamedQuery(name = "SpeechToTextControl.updateStatusByStatusAndDate",
            query = "UPDATE SpeechToTextControl stt SET stt.status = :newStatus "
                + "WHERE stt.status IN :oldStatus and stt.dateCreated < :date")})
// @formatter:on

/**
 * Controls the status of speech to text jobs; used by the quartz job that starts a workflow to asynchronously attach
 * the captions to a media package.
 */
public class SpeechToTextControl {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", length = 128)
  @XmlAttribute
  private long id;

  @Column(name = "mediapackage_id", nullable = false, length = 128)
  private String mediaPackageId;

  /*
   * Workflow that started STT. Workflow id is used to group related speech to text jobs; it's possible that more than
   * one workflow ran on the same media package, generating more than one stt job groups.
   */
  @Column(name = "workflow_id", nullable = false)
  private long workflowId;

  @OneToOne(targetEntity = JpaJob.class, optional = false, cascade = { CascadeType.REMOVE })
  @JoinColumn(name = "job", referencedColumnName = "id")
  private JpaJob job = null;

  @Column(name = "date_created", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCreated;

  @Column(name = "date_completed", nullable = true)
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCompleted;

  @Column(name = "status", nullable = false, length = 128)
  @Enumerated(EnumType.STRING)
  private SpeechToTextControl.Status status;

  public enum Status {
    InProgress, // Transcription running or stt job queued
    TranscriptionDone, // Transcription done (ok), captions not attached yet
    TranscriptionError, // Transcription done (error), no action taken yet
    WorkflowInProgress, // Workflow to attach transcripts created by the quartz task
    Canceled, // An error occurred e.g. subtitle generation error occurred and generation rescheduled,
              // subtitle generation error occurred and no retry, subtitle generation never finished,
              // workflow to attach subtitles could not be started after allotted interval
    Done // Done, transcription attached to media package
  }

  public SpeechToTextControl() {
  }

  /**
   * All fields constructor.
   */
  public SpeechToTextControl(long id, String mediaPackageId, long workflowId, JpaJob job, Date dateStarted,
          Date dateCompleted, Status status) {
    super();
    this.id = id;
    this.mediaPackageId = mediaPackageId;
    this.workflowId = workflowId;
    this.job = job;
    this.dateCreated = dateStarted;
    this.dateCompleted = dateCompleted;
    this.status = status;
  }

  /**
   * Creates a stt control.
   *
   * @param mediaPackageId
   *          the media package id
   * @param workflowId
   *          workflow id that created the stt jobs
   * @param job
   *          associated stt job
   * @param dateCreated
   *          date created
   * @param status
   *          status
   */
  public SpeechToTextControl(String mediaPackageId, long workflowId, JpaJob job, Date dateCreated,
          SpeechToTextControl.Status status) {
    super();
    this.mediaPackageId = mediaPackageId;
    this.workflowId = workflowId;
    this.job = job;
    this.dateCreated = dateCreated;
    this.status = status;
  }

  public long getId() {
    return this.id;
  }

  public Date getDateCreated() {
    return this.dateCreated;
  }

  public Date getDateCompleted() {
    return this.dateCompleted;
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public Status getStatus() {
    return this.status;
  }

  public JpaJob getJob() {
    return this.job;
  }

}
