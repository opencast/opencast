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
package org.opencastproject.comments.persistence;

import static org.opencastproject.util.RequireUtil.notEmpty;
import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.comments.Comment;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.data.Function2;
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

/**
 * Entity object for the comments.
 */
@Entity(name = "Comment")
@Table(name = "mh_comment")
@NamedQueries({ @NamedQuery(name = "Comment.findAll", query = "SELECT c FROM Comment c"),
        @NamedQuery(name = "Comment.clear", query = "DELETE FROM Comment") })
public class CommentDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

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

  @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE })
  private List<CommentReplyDto> replies = new ArrayList<CommentReplyDto>();

  /**
   * Default constructor
   */
  public CommentDto() {
  }

  /**
   * Creates a comment
   * 
   * @param text
   *          the text
   */
  public CommentDto(String text) {
    this.text = notEmpty(text, "text");
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
  public void setReplies(List<CommentReplyDto> replies) {
    this.replies = notNull(replies, "replies");
  }

  /**
   * Returns the replies list
   * 
   * @return the replies list
   */
  public List<CommentReplyDto> getReplies() {
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
  public boolean addReply(CommentReplyDto reply) {
    return replies.add(notNull(reply, "reply"));
  }

  /**
   * Remove a reply from the comment
   * 
   * @param reply
   *          the reply to remove from this comment
   * 
   * @return true if this collection changed as a result of the call
   */
  public boolean removeReply(CommentReplyDto reply) {
    return replies.remove(notNull(reply, "reply"));
  }

  /**
   * Returns the business object of this comment
   * 
   * @return the business object model of this comment
   */
  public Comment toComment(UserDirectoryService userDirectoryService) {
    User user = userDirectoryService.loadUser(author);
    Comment comment = Comment.create(Option.option(id), text, user, reason, resolvedStatus, creationDate,
            modificationDate);
    for (CommentReplyDto reply : replies) {
      comment.addReply(reply.toCommentReply(userDirectoryService));
    }
    return comment;
  }

  public static final Function2<UserDirectoryService, CommentDto, Comment> toComment = new Function2<UserDirectoryService, CommentDto, Comment>() {
    @Override
    public Comment apply(UserDirectoryService userDirectoryService, CommentDto dto) {
      return dto.toComment(userDirectoryService);
    }
  };
}
