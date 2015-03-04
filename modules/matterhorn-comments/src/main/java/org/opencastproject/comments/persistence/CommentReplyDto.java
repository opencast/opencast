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

import org.opencastproject.comments.CommentReply;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entity object for the comment replies.
 */
@Entity(name = "CommentReply")
@Table(name = "mh_comment_reply")
@NamedQueries({ @NamedQuery(name = "CommentReply.findAll", query = "SELECT c FROM CommentReply c"),
        @NamedQuery(name = "CommentReply.clear", query = "DELETE FROM CommentReply") })
public class CommentReplyDto {

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

  /**
   * Default constructor
   */
  public CommentReplyDto() {
  }

  /**
   * Creates a comment reply
   * 
   * @param text
   *          the text
   */
  public CommentReplyDto(String text) {
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
   * Returns the business object of this comment reply
   * 
   * @return the business object model of this comment reply
   */
  public CommentReply toCommentReply(UserDirectoryService userDirectoryService) {
    User user = userDirectoryService.loadUser(author);
    return CommentReply.create(Option.option(id), text, user, creationDate, modificationDate);
  }

  public static final Function2<UserDirectoryService, CommentReplyDto, CommentReply> toCommentReply = new Function2<UserDirectoryService, CommentReplyDto, CommentReply>() {
    @Override
    public CommentReply apply(UserDirectoryService userDirectoryService, CommentReplyDto dto) {
      return dto.toCommentReply(userDirectoryService);
    }
  };
}
