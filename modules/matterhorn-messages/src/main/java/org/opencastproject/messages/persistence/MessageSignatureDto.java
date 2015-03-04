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
package org.opencastproject.messages.persistence;

import static org.opencastproject.util.RequireUtil.notEmpty;
import static org.opencastproject.util.RequireUtil.notNull;
import static org.opencastproject.util.RequireUtil.nullOrNotEmpty;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.comments.persistence.CommentDto;
import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.messages.MessageSignature;
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
import javax.persistence.UniqueConstraint;

/** Entity object for message signatures. */
@Entity(name = "MessageSignature")
@Table(name = "mh_message_signature", uniqueConstraints = { @UniqueConstraint(columnNames = { "name", "organization" }) })
@NamedQueries({
        @NamedQuery(name = "MessageSignature.countByCreator", query = "SELECT COUNT(m) FROM MessageSignature m WHERE m.creator = :username AND m.organization = :org"),
        @NamedQuery(name = "MessageSignature.findAll", query = "SELECT m FROM MessageSignature m WHERE m.organization = :org"),
        @NamedQuery(name = "MessageSignature.findByCreator", query = "SELECT m FROM MessageSignature m WHERE m.creator = :username AND m.organization = :org"),
        @NamedQuery(name = "MessageSignature.findById", query = "SELECT m FROM MessageSignature m WHERE m.id = :id AND m.organization = :org"),
        @NamedQuery(name = "MessageSignature.findByName", query = "SELECT m FROM MessageSignature m WHERE m.name = :name AND m.organization = :org"),
        @NamedQuery(name = "MessageSignature.clear", query = "DELETE FROM MessageSignature m WHERE m.organization = :org") })
public class MessageSignatureDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "organization", length = 128)
  private String organization;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "creator_username", nullable = false)
  private String creator;

  // email address
  @Column(name = "sender", nullable = false)
  private String sender;

  // display name
  @Column(name = "sender_name", nullable = false)
  private String senderName;

  // email address
  @Column(name = "reply_to")
  private String replyTo;

  // display name
  @Column(name = "reply_to_name")
  private String replyToName;

  @Column(name = "signature", nullable = false)
  private String signature;

  @Column(name = "creation_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date creationDate;

  @OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.REMOVE })
  private List<CommentDto> comments = new ArrayList<CommentDto>();

  /** Default constructor */
  public MessageSignatureDto() {
  }

  /** Creates a message signature */
  public MessageSignatureDto(String name, String organization, String creator, String sender, String senderName,
          String replyTo, String replyToName, String signature, Date creationDate, List<CommentDto> comments) {
    this.name = notEmpty(name, "name");
    this.organization = notEmpty(organization, "organization");
    this.creator = notNull(creator, "creator");
    this.sender = notEmpty(sender, "sender");
    this.senderName = notEmpty(senderName, "senderName");
    this.replyTo = nullOrNotEmpty(replyTo, "replyTo");
    this.replyToName = nullOrNotEmpty(replyToName, "replyToName");
    this.signature = notNull(signature, "signature");
    this.creationDate = notNull(creationDate, "creationDate");
    this.comments = new ArrayList<CommentDto>(notNull(comments, "comments"));
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
   * Returns the organization id
   *
   * @return the organization id
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
   *          the creator of the signature
   */
  public void setCreator(String creator) {
    this.creator = creator;
  }

  /**
   * Returns the creator
   *
   * @returns the creator of the signature
   */
  public String getCreator() {
    return this.creator;
  }

  /**
   * Sets the email
   *
   * @param sender
   *          the email
   */
  public void setSender(String sender) {
    this.sender = sender;
  }

  /**
   * Returns the email
   *
   * @return the email
   */
  public String getSender() {
    return sender;
  }

  public String getSenderName() {
    return senderName;
  }

  public void setSenderName(String senderName) {
    this.senderName = senderName;
  }

  public String getReplyTo() {
    return replyTo;
  }

  public void setReplyTo(String replyTo) {
    this.replyTo = replyTo;
  }

  public String getReplyToName() {
    return replyToName;
  }

  public void setReplyToName(String replyToName) {
    this.replyToName = replyToName;
  }

  /**
   * Sets the signature
   *
   * @param signature
   *          the signature
   */
  public void setSignature(String signature) {
    this.signature = signature;
  }

  /**
   * Returns the signature
   *
   * @return the signature
   */
  public String getSignature() {
    return signature;
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
    this.comments = notNull(comments, "comments");
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
  public boolean removeComment(CommentDto comment) {
    return comments.remove(notNull(comment, "comment"));
  }

  /**
   * Returns the business object of this message signature
   *
   * @return the business object model of this message signature
   */
  public MessageSignature toMessageSignature(UserDirectoryService userDirectoryService) {
    final Option<EmailAddress> reply;
    if (replyTo != null && replyToName != null) {
      reply = some(new EmailAddress(replyTo, replyToName));
    } else {
      reply = none();
    }

    User user = userDirectoryService.loadUser(creator);
    return new MessageSignature(id, name, user, new EmailAddress(sender, senderName), reply, signature, creationDate,
            mlist(comments).map(CommentDto.toComment.curry(userDirectoryService)).value());
  }
}
