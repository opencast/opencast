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

package org.opencastproject.messages;

import static org.opencastproject.util.RequireUtil.notEmpty;
import static org.opencastproject.util.RequireUtil.notNull;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Option.none;

import org.opencastproject.comments.Comment;
import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.security.api.User;
import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;
import org.opencastproject.util.Jsons.Val;
import org.opencastproject.util.data.Option;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Business object for message signatures. */
public class MessageSignature {
  /** The signature id */
  private Long id;

  /** The message signature name */
  private String name;

  /** The creation date */
  private Date creationDate;

  /** The comment list */
  private List<Comment> comments = new ArrayList<Comment>();

  /** The creator of the signature */
  private User creator;

  /** The sender's email address */
  private EmailAddress sender;

  /** The reply to address */
  private Option<EmailAddress> replyTo;

  /** The signature */
  private String signature;

  /**
   * Creates a message signature
   *
   * @param name
   *          the name
   * @param creator
   *          the creator of the signature
   * @param sender
   *          the sender
   * @param signature
   *          the signature
   * @param creationDate
   *          the creation date
   * @param comments
   *          the comment list
   */
  public MessageSignature(Long id, String name, User creator, EmailAddress sender, Option<EmailAddress> replyTo,
          String signature, Date creationDate, List<Comment> comments) {
    this.id = id;
    this.name = notEmpty(name, "name");
    this.creator = notNull(creator, "creator");
    this.sender = sender;
    this.replyTo = replyTo;
    this.signature = notNull(signature, "signature");
    this.creationDate = notNull(creationDate, "creationDate");
    this.comments = new ArrayList<Comment>(notNull(comments, "comments"));
  }

  public static MessageSignature messageSignature(String name, User creator, EmailAddress sender, String signature) {
    return new MessageSignature(null, name, creator, sender, none(EmailAddress.class), signature, new Date(),
            nil(Comment.class));
  }

  /**
   * Sets the id
   *
   * @param id
   *          the signature id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the signature id
   *
   * @return the id
   */
  public Long getId() {
    return this.id;
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
  public void setCreator(User creator) {
    this.creator = creator;
  }

  /**
   * Returns the creator
   *
   * @returns the creator of the signature
   */
  public User getCreator() {
    return this.creator;
  }

  /**
   * Sets the sender
   *
   * @param sender
   *          the sender
   */
  public void setSender(EmailAddress sender) {
    this.sender = sender;
  }

  /**
   * Returns the sender
   *
   * @return the sender
   */
  public EmailAddress getSender() {
    return sender;
  }

  public void setReplyTo(Option<EmailAddress> replyTo) {
    this.replyTo = replyTo;
  }

  public Option<EmailAddress> getReplyTo() {
    return replyTo;
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
  public void setComments(List<Comment> comments) {
    this.comments = notNull(comments, "comments");
  }

  /**
   * Returns the comment list
   *
   * @return the comment list
   */
  public List<Comment> getComments() {
    return comments;
  }

  /**
   * Add a comment to the signature
   *
   * @param comment
   *          the comment to add to this signature
   * @return true if this collection changed as a result of the call
   */
  public boolean addComment(Comment comment) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    MessageSignature msgSig = (MessageSignature) o;
    return name.equals(msgSig.getName()) && sender.equals(msgSig.getSender())
            && signature.equals(msgSig.getSignature()) && creationDate.equals(msgSig.getCreationDate())
            && comments.equals(msgSig.getComments()) && creator.equals(msgSig.getCreator());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, name, sender, signature, creationDate, comments, creator);
  }

  @Override
  public String toString() {
    return "MessageSignature:" + name;
  }

  public Obj toJson() {
    List<Val> commentsArr = new ArrayList<Val>();
    for (Comment c : comments) {
      commentsArr.add(c.toJson());
    }

    Obj replyJson = Jsons.ZERO_OBJ;
    if (replyTo.isSome())
      replyJson = replyTo.get().toJson();

    Obj creatorObj = Jsons.obj(Jsons.p("name", creator.getName()), Jsons.p("username", creator.getUsername()),
            Jsons.p("email", creator.getEmail()));
    return Jsons.obj(Jsons.p("id", id), Jsons.p("name", name), Jsons.p("creationDate", creationDate),
            Jsons.p("creator", creatorObj), Jsons.p("signature", signature), Jsons.p("sender", sender.toJson()),
            Jsons.p("replyTo", replyJson), Jsons.p("comments", Jsons.arr(commentsArr)));
  }

}
