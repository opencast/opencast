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

package org.opencastproject.comments.events.persistence;

import static org.opencastproject.util.RequireUtil.notEmpty;
import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.comments.persistence.CommentDto;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Entity object for mapping events to comments.
 */
@Entity(name = "EventComment")
@Table(name = "mh_event_mh_comment")
@NamedQueries({
        @NamedQuery(name = "EventComment.countAll", query = "SELECT COUNT(e) FROM EventComment e"),
        @NamedQuery(name = "EventComment.findAll", query = "SELECT e FROM EventComment e"),
        @NamedQuery(name = "EventComment.findReasons", query = "SELECT e.comment.reason FROM EventComment e WHERE e.organization = :org GROUP BY e.comment.reason"),
        @NamedQuery(name = "EventComment.findByEvent", query = "SELECT e.comment FROM EventComment e WHERE e.eventId = :eventId AND e.organization = :org ORDER BY e.comment.creationDate"),
        @NamedQuery(name = "EventComment.findByCommentId", query = "SELECT e FROM EventComment e WHERE e.eventId = :eventId AND e.comment.id = :commentId AND e.organization = :org"),
        @NamedQuery(name = "EventComment.clear", query = "DELETE FROM EventComment e WHERE e.organization = :org") })
public class EventCommentDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "organization", length = 128, nullable = false)
  private String organization;

  @Column(name = "event", length = 128, nullable = false)
  private String eventId;

  @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinColumn(name = "comment", nullable = false)
  private CommentDto comment;

  /**
   * Default constructor
   */
  public EventCommentDto() {
  }

  /**
   * Creates an event comment
   *
   * @param eventId
   *          the event identifier
   * @param comment
   *          the comment
   * @param organization
   *          the organization
   */
  public EventCommentDto(String eventId, CommentDto comment, String organization) {
    this.eventId = notEmpty(eventId, "eventId");
    this.comment = notNull(comment, "comment");
    this.organization = notEmpty(organization, "organization");
  }

  /**
   * Returns the id of this entity
   *
   * @return the id as long
   */
  public long getId() {
    return id;
  }

  /**
   * Returns the event identifier
   *
   * @return the event identifier
   */
  public String getEventId() {
    return eventId;
  }

  /**
   * Sets the event identifier
   *
   * @param eventId
   *          the event identifier
   */
  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  /**
   * Returns the organization
   *
   * @return the organization
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * Sets the organization
   *
   * @param organization
   *          the organization
   */
  public void setOrganization(String organization) {
    this.organization = organization;
  }

  /**
   * Returns the comment
   *
   * @return the comment
   */
  public CommentDto getComment() {
    return comment;
  }

  /**
   * Sets the comment
   *
   * @param comment
   *          the comment
   */
  public void setComment(CommentDto comment) {
    this.comment = comment;
  }

}
