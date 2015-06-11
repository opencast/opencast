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

package org.opencastproject.messages.persistence;

import static org.opencastproject.util.RequireUtil.notEmpty;
import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.comments.Comment;
import org.opencastproject.comments.persistence.CommentDto;
import org.opencastproject.messages.MessageTemplate;
import org.opencastproject.messages.TemplateType;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/** Entity object for message templates. */
@Entity(name = "MessageTemplate")
@Table(name = "mh_message_template", uniqueConstraints = { @UniqueConstraint(columnNames = { "name", "organization" }) })
@NamedQueries({
        @NamedQuery(name = "MessageTemplate.findAll", query = "SELECT m FROM MessageTemplate m WHERE m.organization = :org"),
        @NamedQuery(name = "MessageTemplate.findById", query = "SELECT m FROM MessageTemplate m WHERE m.id = :id AND m.organization = :org"),
        @NamedQuery(name = "MessageTemplate.findByName", query = "SELECT m FROM MessageTemplate m WHERE m.name = :name AND m.organization = :org"),
        @NamedQuery(name = "MessageTemplate.likeName", query = "SELECT m FROM MessageTemplate m WHERE m.name like :name AND m.organization = :org"),
        @NamedQuery(name = "MessageTemplate.clear", query = "DELETE FROM MessageTemplate m WHERE m.organization = :org") })
public class MessageTemplateDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "organization", length = 128)
  private String organization;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "subject", nullable = false)
  private String subject;

  @Column(name = "body", nullable = false)
  private String body;

  @Column(name = "hidden", nullable = false)
  private boolean hidden = false;

  @Enumerated(EnumType.STRING)
  private TemplateType.Type type;

  @Column(name = "creation_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date creationDate;

  @Column(name = "creator_username", nullable = false)
  private String creator;

  @OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.REMOVE })
  private List<CommentDto> comments = new ArrayList<CommentDto>();

  /** Default constructor */
  public MessageTemplateDto() {
  }

  /**
   * Creates a message template
   *
   * @param name
   *          the name
   * @param organization
   *          the organization
   * @param creator
   *          the creator of the template
   * @param subject
   *          the subject
   * @param body
   *          the body
   * @param creationDate
   *          the creation date
   * @param comments
   *          the comments
   */
  public MessageTemplateDto(String name, String organization, String creator, String subject, String body,
          TemplateType.Type type, Date creationDate, List<CommentDto> comments) {
    this.name = notEmpty(name, "name");
    this.organization = notEmpty(organization, "organization");
    this.creator = notNull(creator, "creator");
    this.subject = notEmpty(subject, "subject");
    this.body = notEmpty(body, "body");
    this.type = notNull(type, "type");
    this.creationDate = notNull(creationDate, "creationDate");
    this.comments = notNull(comments, "comments");
  }

  /**
   * Creates a message template with the current time as creation date and an empty list of comments
   *
   * @param name
   *          the name
   * @param organization
   *          the organization
   * @param creator
   *          the creator of the template
   * @param subject
   *          the subject
   * @param body
   *          the body
   */
  public MessageTemplateDto(String name, String organization, String creator, String subject, String body) {
    this.name = notEmpty(name, "name");
    this.organization = notEmpty(organization, "organization");
    this.creator = notNull(creator, "creator");
    this.subject = notEmpty(subject, "subject");
    this.body = notEmpty(body, "body");
    this.type = TemplateType.Type.INVITATION;
    this.creationDate = notNull(creationDate, "creationDate");
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
   * Sets the name
   *
   * @param name
   *          the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the name
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the creator
   *
   * @param creator
   *          the creator of the template
   */
  public void setCreator(String creator) {
    this.creator = creator;
  }

  /**
   * Returns the creator
   *
   * @returns the creator of the template
   */
  public String getCreator() {
    return this.creator;
  }

  /**
   * Sets the subject
   *
   * @param subject
   *          the subject
   */
  public void setSubject(String subject) {
    this.subject = subject;
  }

  /**
   * Returns the subject
   *
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Sets the body
   *
   * @param body
   *          the body
   */
  public void setBody(String body) {
    this.body = body;
  }

  /**
   * Returns the body
   *
   * @return the body
   */
  public String getBody() {
    return body;
  }

  /**
   * Sets whether the template is hidden in the UI
   *
   * @param hidden
   *          whether the template is hidden in the UI
   */
  public void setHidden(boolean hidden) {
    this.hidden = hidden;
  }

  /**
   * Returns whether the template is hidden in the UI
   *
   * @return whether the template is hidden in the UI
   */
  public boolean isHidden() {
    return hidden;
  }

  /**
   * Sets the template type
   *
   * @param type
   *          the template type
   */
  public void setType(TemplateType.Type type) {
    this.type = type;
  }

  /**
   * Returns the template type
   *
   * @return the type
   */
  public TemplateType.Type getType() {
    return type;
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
   * Sets the comment list
   *
   * @param comments
   *          the comment list
   */
  public void setComments(List<CommentDto> comments) {
    this.comments = comments;
  }

  /**
   * Returns the comment list
   *
   * @return the comment list
   */
  public List<CommentDto> getComments() {
    return comments;
  }

  /**
   * Add a comment to the signature
   *
   * @param comment
   *          the comment to add to this signature
   * @return true if this collection changed as a result of the call
   */
  public boolean addComment(CommentDto comment) {
    return comments.add(notNull(comment, "comment"));
  }

  /**
   * Remove a comment from the signature
   *
   * @param comment
   *          the comment to remove from this signature
   * @return true if this collection changed as a result of the call
   */
  public boolean removeComment(Comment comment) {
    return comments.remove(notNull(comment, "comment"));
  }

  /**
   * Returns the business object of the message template
   *
   * @return the business object model of this message template
   */
  public MessageTemplate toMessageTemplate(UserDirectoryService userDirectoryService) {
    User user = userDirectoryService.loadUser(creator);
    MessageTemplate msgTmpl = new MessageTemplate(name, user, subject, body, type.getType(), creationDate,
            new ArrayList<Comment>());
    msgTmpl.setHidden(hidden);
    for (CommentDto c : this.comments) {
      msgTmpl.addComment(c.toComment(userDirectoryService));
    }
    msgTmpl.setId(id);
    return msgTmpl;
  }

}
