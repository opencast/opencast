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
package org.opencastproject.comments;

import static com.entwinemedia.fn.data.json.Jsons.a;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.j;
import static com.entwinemedia.fn.data.json.Jsons.jz;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static com.entwinemedia.fn.data.json.Jsons.vN;
import static org.opencastproject.util.RequireUtil.notEmpty;
import static org.opencastproject.util.RequireUtil.notNull;

import com.entwinemedia.fn.data.json.JField;
import com.entwinemedia.fn.data.json.JValue;
import org.opencastproject.security.api.User;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;
import org.opencastproject.util.Jsons.Val;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Business object for comments.
 */
public final class Comment {

  /** The comment identifier */
  private Option<Long> id;

  /** The comment text */
  private String text;

  /** The creation date */
  private Date creationDate;

  /** The modification date */
  private Date modificationDate;

  /** The comment author */
  private User author;

  /** The comment reason */
  private String reason;

  /** The comment resolve status */
  private boolean resolvedStatus = false;

  /** The comment replies */
  private List<CommentReply> replies = new ArrayList<CommentReply>();

  /**
   * Creates a simple comment
   *
   * @param id
   *          the optional identifier
   * @param text
   *          the text
   * @param author
   *          the author of the comment
   * @throws IllegalArgumentException
   *           if id, text or author is not set
   */
  public static Comment create(Option<Long> id, String text, User author) {
    return create(id, text, author, null, false);
  }

  /**
   * Creates a complex comment
   *
   * @param id
   *          the optional identifier
   * @param text
   *          the text
   * @param author
   *          the author of the comment
   * @param reason
   *          the comment reason
   * @param resolvedStatus
   *          whether the comment is resolved
   * @throws IllegalArgumentException
   *           if id, text, or author is not set
   */
  public static Comment create(Option<Long> id, String text, User author, String reason, boolean resolvedStatus) {
    Date creationDate = new Date();
    return create(id, text, author, reason, resolvedStatus, creationDate, creationDate, new ArrayList<CommentReply>());
  }

  /**
   * Creates a complex comment
   *
   * @param id
   *          the optional identifier
   * @param text
   *          the text
   * @param author
   *          the author of the comment
   * @param reason
   *          the comment reason
   * @param resolvedStatus
   *          whether the comment is resolved
   * @param resolvedStatus
   *          whether the comment is resolved
   * @param creationDate
   *          the creation date
   * @param modificationDate
   *          the modification date
   * @throws IllegalArgumentException
   *           if id, text, author, creation date or modification date is not set
   */
  public static Comment create(Option<Long> id, String text, User author, String reason, boolean resolvedStatus,
          Date creationDate, Date modificationDate) {
    return new Comment(id, text, author, reason, resolvedStatus, creationDate, modificationDate,
            new ArrayList<CommentReply>());
  }

  /**
   * Creates a complex comment
   *
   * @param id
   *          the optional identifier
   * @param text
   *          the text
   * @param author
   *          the author of the comment
   * @param reason
   *          the comment reason
   * @param resolvedStatus
   *          whether the comment is resolved
   * @param resolvedStatus
   *          whether the comment is resolved
   * @param creationDate
   *          the creation date
   * @param modificationDate
   *          the modification date
   * @param replies
   *          the replies
   * @throws IllegalArgumentException
   *           if id, text, author, creation date, modification date or replies is not set
   */
  public static Comment create(Option<Long> id, String text, User author, String reason, boolean resolvedStatus,
          Date creationDate, Date modificationDate, List<CommentReply> replies) {
    return new Comment(id, text, author, reason, resolvedStatus, creationDate, modificationDate, replies);
  }

  private Comment(Option<Long> id, String text, User author, String reason, boolean resolvedStatus, Date creationDate,
          Date modificationDate, List<CommentReply> replies) {
    this.id = notNull(id, "id");
    this.text = notEmpty(text, "text");
    this.author = notNull(author, "author");
    this.reason = reason;
    this.resolvedStatus = resolvedStatus;
    this.creationDate = notNull(creationDate, "creationDate");
    this.modificationDate = notNull(modificationDate, "modificationDate");
    this.replies = notNull(replies, "replies");
  }

  /**
   * Returns the comment id
   *
   * @return the id
   */
  public Option<Long> getId() {
    return id;
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
   * Returns the creation date
   *
   * @return the creation date
   */
  public Date getCreationDate() {
    return creationDate;
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
   * Returns the author
   *
   * @return the author
   */
  public User getAuthor() {
    return author;
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
   * Returns whether the status is resolved
   *
   * @return whether the status is resolved
   */
  public boolean isResolvedStatus() {
    return resolvedStatus;
  }

  /**
   * Returns the comment replies
   *
   * @return the comment replies
   */
  public List<CommentReply> getReplies() {
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
  public boolean addReply(CommentReply reply) {
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
  public boolean removeReply(CommentReply reply) {
    return replies.remove(notNull(reply, "reply"));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Comment comment = (Comment) o;

    return text.equals(comment.getText()) && creationDate.equals(comment.getCreationDate())
            && modificationDate.equals(comment.getModificationDate()) && author.equals(comment.getAuthor())
            && (reason == null ? comment.getReason() == null : reason.equals(comment.getReason()))
            && resolvedStatus == comment.isResolvedStatus();
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(text, creationDate, modificationDate, author, reason, resolvedStatus);
  }

  @Override
  public String toString() {
    return "Comment:" + id + "|" + StringUtils.abbreviate(text, 25);
  }

  public Obj toJson() {
    Obj authorObj = Jsons.obj(Jsons.p("name", author.getName()), Jsons.p("username", author.getUsername()),
            Jsons.p("email", author.getEmail()));

    List<Val> replyArr = new ArrayList<Val>();
    for (CommentReply reply : replies) {
      replyArr.add(reply.toJson());
    }

    Val idValue = Jsons.ZERO_VAL;
    if (id.isSome())
      idValue = Jsons.v(id.get());

    return Jsons.obj(Jsons.p("id", idValue), Jsons.p("text", text),
            Jsons.p("creationDate", DateTimeSupport.toUTC(creationDate.getTime())),
            Jsons.p("modificationDate", DateTimeSupport.toUTC(modificationDate.getTime())),
            Jsons.p("author", authorObj), Jsons.p("reason", reason), Jsons.p("resolvedStatus", resolvedStatus),
            Jsons.p("replies", Jsons.arr(replyArr)));
  }

  public JValue toJValue() {
    JValue authorObj = j(f("name", vN(author.getName())), f("username", v(author.getUsername())),
            f("email", vN(author.getEmail())));

    List<JValue> replyArr = new ArrayList<JValue>();
    for (CommentReply reply : replies) {
      replyArr.add(reply.toJValue());
    }

    JValue idValue = jz;
    if (id.isSome())
      idValue = v(id.get());

    List<JField> fields = new ArrayList<JField>();
    fields.add(f("id", idValue));
    fields.add(f("text", v(text)));
    fields.add(f("creationDate", v(DateTimeSupport.toUTC(creationDate.getTime()))));
    fields.add(f("modificationDate", v(DateTimeSupport.toUTC(modificationDate.getTime()))));
    fields.add(f("author", authorObj));
    fields.add(f("reason", v(reason)));
    fields.add(f("resolvedStatus", v(resolvedStatus)));
    fields.add(f("replies", a(replyArr)));

    return j(fields);
  }

}
