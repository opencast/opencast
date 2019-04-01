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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.migration

import java.util.Date

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.Table
import javax.persistence.Temporal
import javax.persistence.TemporalType
import javax.persistence.UniqueConstraint

/**
 * Entity object for storing extended scheduled event information in persistence storage.
 */
/**
 * Default constructor without any import.
 */
@IdClass(EventIdPK::class)
@Entity(name = "ExtendedEvent")
@NamedQueries(NamedQuery(name = "ExtendedEvent.findAll", query = "SELECT e FROM ExtendedEvent e WHERE e.organization = :org"), NamedQuery(name = "ExtendedEvent.countAll", query = "SELECT COUNT(e) FROM ExtendedEvent e"), NamedQuery(name = "ExtendedEvent.findEvents", query = "SELECT e.mediaPackageId FROM ExtendedEvent e WHERE e.organization = :org AND e.captureAgentId = :ca AND e.startDate < :end AND e.endDate > :start ORDER BY e.startDate ASC"), NamedQuery(name = "ExtendedEvent.searchEventsCA", query = "SELECT e FROM ExtendedEvent e WHERE e.organization = :org AND e.captureAgentId = :ca AND e.startDate >= :startFrom AND e.startDate < :startTo AND e.endDate >= :endFrom AND e.endDate < :endTo ORDER BY e.startDate ASC"), NamedQuery(name = "ExtendedEvent.searchEvents", query = "SELECT e FROM ExtendedEvent e WHERE e.organization = :org AND e.startDate >= :startFrom AND e.startDate < :startTo AND e.endDate >= :endFrom AND e.endDate < :endTo ORDER BY e.startDate ASC"), NamedQuery(name = "ExtendedEvent.knownRecordings", query = "SELECT e FROM ExtendedEvent e WHERE e.organization = :org AND e.recordingState IS NOT NULL AND e.recordingLastHeard IS NOT NULL"))
@Table(name = "oc_scheduled_extended_event", uniqueConstraints = [UniqueConstraint(columnNames = ["mediapackage_id", "organization"])])
class ExtendedEventDto {

    /** Event ID, primary key  */
    @Id
    @Column(name = "mediapackage_id", length = 128)
    var mediaPackageId: String? = null

    /** Organization id, primary key  */
    @Id
    @Column(name = "organization", length = 128)
    var organization: String? = null

    /** Capture agent id  */
    @Column(name = "capture_agent_id", length = 128)
    var captureAgentId: String? = null

    /** recording start date  */
    @Column(name = "start_date")
    @Temporal(TemporalType.TIMESTAMP)
    var startDate: Date? = null

    /** recording end date  */
    @Column(name = "end_date")
    @Temporal(TemporalType.TIMESTAMP)
    var endDate: Date? = null

    /** source  */
    @Column(name = "source")
    var source: String? = null

    /** recording state  */
    @Column(name = "recording_state")
    var recordingState: String? = null

    /** recording last heard  */
    @Column(name = "recording_last_heard")
    var recordingLastHeard: Long? = null

    /** presenters  */
    @Column(name = "presenters")
    var presenters: String? = null

    /** last modified date  */
    @Column(name = "last_modified_date")
    @Temporal(TemporalType.TIMESTAMP)
    var lastModifiedDate: Date? = null

    /** capture agent properties  */
    @Column(name = "capture_agent_properties")
    var captureAgentProperties: String? = null

    /** workflow properties  */
    @Column(name = "workflow_properties")
    var workflowProperties: String? = null

    @Column(name = "checksum", length = 64)
    var checksum: String? = null
}
