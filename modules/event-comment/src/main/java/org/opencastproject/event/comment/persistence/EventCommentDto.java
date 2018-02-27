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

package org.opencastproject.event.comment.persistence;

import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.event.comment.EventComment;
import org.opencastproject.event.comment.EventCommentReply;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.data.Option;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity(name = "EventComment")
@Table(name = "oc_event_comment")
@NamedQueries({ @NamedQuery(name = "EventComment.countAll", query = "SELECT COUNT(e) FROM EventComment e"),
        @NamedQuery(name = "EventComment.findAll", query = "SELECT e FROM EventComment e"),
        @NamedQuery(name = "EventComment.findReasons", query = "SELECT e.reason FROM EventComment e WHERE e.organization = :org GROUP BY e.reason"),
        @NamedQuery(name = "EventComment.findByEvent", query = "SELECT e FROM EventComment e WHERE e.eventId = :eventId AND e.organization = :org ORDER BY e.creationDate"),
        @NamedQuery(name = "EventComment.findByCommentId", query = "SELECT e FROM EventComment e WHERE e.id = :commentId"),
        @NamedQuery(name = "EventComment.findAllWIthOrg", query = "SELECT e.organization, e.eventId FROM EventComment e ORDER BY e.organization ASC"),
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

  @Column(name = "text", nullable = false)
  private String text;

  @Column(name = "creation_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date creationDate;

  @Column(name = "modification_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date modificationDate;

  @Column(name = "author", nullable = false)
  private String author;

  @Column(name = "reason")
  private String reason;

  @Column(name = "resolved_status", nullable = false)
  private boolean resolvedStatus = false;

  @OneToMany(targetEntity = EventCommentReplyDto.class, fetch = FetchType.LAZY, cascade = {
          CascadeType.REMOVE }, mappedBy = "eventComment", orphanRemoval = true)
  private List<EventCommentReplyDto> replies = new ArrayList<EventCommentReplyDto>();

  /**
   * Default constructor
   */
  public EventCommentDto() {
  }

  public static EventCommentDto from(EventComment comment) {
    EventCommentDto dto = new EventCommentDto();

    if (comment.getId().isSome())
      dto.id = comment.getId().get().longValue();
    dto.organization = comment.getOrganization();
    dto.eventId = comment.getEventId();
    dto.text = comment.getText();
    dto.creationDate = comment.getCreationDate();
    dto.modificationDate = comment.getModificationDate();
    dto.author = comment.getAuthor().getUsername();
    dto.reason = comment.getReason();
    dto.resolvedStatus = comment.isResolvedStatus();

    for (final EventCommentReply reply : comment.getReplies()) {
      dto.addReply(EventCommentReplyDto.from(reply));
    }

    return dto;
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
   * Sets the text
   *
   * @param text
   *          the text
   */
  public void setText(String text) {
    this.text = text;
  }

  /**
   * Returns the text
   *
   * @return the text
   */
  public String getText() {
    return text;
  }

  /**
   * Sets the creation date
   *
   * @param creationDate
   *          the creation date
   */
  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  /**
   * Returns the creation date
   *
   * @return the creation date
   */
  public Date getCreationDate() {
    return creationDate;
  }

  /**
   * Sets the modification date
   *
   * @param modificationDate
   *          the modification date
   */
  public void setModificationDate(Date modificationDate) {
    this.modificationDate = modificationDate;
  }

  /**
   * Returns the modification date
   *
   * @return the modification date
   */
  public Date getModificationDate() {
    return modificationDate;
  }

  /**
   * Sets the author
   *
   * @param author
   *          the author
   */
  public void setAuthor(String author) {
    this.author = author;
  }

  /**
   * Returns the author
   *
   * @return the author
   */
  public String getAuthor() {
    return author;
  }

  /**
   * Sets the reason
   *
   * @param reason
   *          the reason
   */
  public void setReason(String reason) {
    this.reason = reason;
  }

  /**
   * Returns the reason
   *
   * @return the reason
   */
  public String getReason() {
    return reason;
  }

  /**
   * Sets whether the status is resolved
   *
   * @param resolvedStatus
   *          whether the status is resolved
   */
  public void setResolvedStatus(boolean resolvedStatus) {
    this.resolvedStatus = resolvedStatus;
  }

  /**
   * Returns whether the status is resolved
   *
   * @return whether the status is resolved
   */
  public boolean isResolvedStatus() {
    return resolvedStatus;
  }

  /**
   * Sets a comment replies list.
   *
   * @param replies
   *          the replies list
   */
  public void setReplies(List<EventCommentReplyDto> replies) {
    this.replies = notNull(replies, "replies");
  }

  /**
   * Returns the replies list
   *
   * @return the replies list
   */
  public List<EventCommentReplyDto> getReplies() {
    return replies;
  }

  /**
   * Add a reply to the comment
   *
   * @param reply
   *          the reply to add to this comment
   *
   * @return true if this collection changed as a result of the call
   */
  public boolean addReply(EventCommentReplyDto reply) {
    notNull(reply, "reply").setEventComment(this);
    return replies.add(reply);
  }

  /**
   * Remove a reply from the comment
   *
   * @param reply
   *          the reply to remove from this comment
   *
   * @return true if this collection changed as a result of the call
   */
  public boolean removeReply(EventCommentReplyDto reply) {
    return replies.remove(notNull(reply, "reply"));
  }

  /**
   * Returns the business object of this comment
   *
   * @return the business object model of this comment
   */
  public EventComment toComment(UserDirectoryService userDirectoryService) {
    User user = userDirectoryService.loadUser(author);
    EventComment comment = EventComment.create(Option.option(id), eventId, organization, text, user, reason,
            resolvedStatus, creationDate, modificationDate);
    for (EventCommentReplyDto reply : replies) {
      comment.addReply(reply.toCommentReply(userDirectoryService));
    }
    return comment;
  }

}
