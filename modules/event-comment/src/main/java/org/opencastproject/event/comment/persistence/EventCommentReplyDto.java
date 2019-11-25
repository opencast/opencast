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

import org.opencastproject.event.comment.EventCommentReply;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.data.Option;

import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entity object for the comment replies.
 */
@Entity(name = "EventCommentReply")
@Access(AccessType.FIELD)
@Table(name = "oc_event_comment_reply")
@NamedQueries({ @NamedQuery(name = "EventCommentReply.findAll", query = "SELECT c FROM EventCommentReply c"),
        @NamedQuery(name = "EventCommentReply.clear", query = "DELETE FROM EventCommentReply") })
public class EventCommentReplyDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @ManyToOne(targetEntity = EventCommentDto.class)
  @JoinColumn(name = "event_comment_id", referencedColumnName = "id", nullable = false)
  private EventCommentDto eventComment;

  @Lob
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

  /**
   * Default constructor
   */
  public EventCommentReplyDto() {
  }

  public static EventCommentReplyDto from(EventCommentReply reply) {
    EventCommentReplyDto dto = new EventCommentReplyDto();
    if (reply.getId().isSome())
      dto.id = reply.getId().get().longValue();
    dto.text = reply.getText();
    dto.creationDate = reply.getCreationDate();
    dto.modificationDate = reply.getModificationDate();
    dto.author = reply.getAuthor().getUsername();

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

  EventCommentDto getEventComment() {
    return eventComment;
  }

  void setEventComment(EventCommentDto eventComment) {
    this.eventComment = eventComment;
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
   * Returns the business object of this comment reply
   *
   * @return the business object model of this comment reply
   */
  public EventCommentReply toCommentReply(UserDirectoryService userDirectoryService) {
    User user = userDirectoryService.loadUser(author);
    return EventCommentReply.create(Option.option(id), text, user, creationDate, modificationDate);
  }

}
