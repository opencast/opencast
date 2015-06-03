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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Business object for comment replies.
 */
public final class CommentReply {

  /** comment reply identifier */
  private Option<Long> id;

  /** The comment reply text */
  private String text;

  /** The creation date */
  private Date creationDate;

  /** The modification date */
  private Date modificationDate;

  /** The comment reply author */
  private User author;

  /**
   * Creates a comment reply
   *
   * @param id
   *          the optional reply identifier
   * @param text
   *          the text
   * @param author
   *          the author of the comment reply
   * @throws IllegalArgumentException
   *           if some of the parameters aren't set
   */
  public static CommentReply create(Option<Long> id, String text, User author) {
    Date creationDate = new Date();
    return create(id, text, author, creationDate, creationDate);
  }

  /**
   * Creates a comment reply
   *
   * @param id
   *          the optional reply identifier
   * @param text
   *          the text
   * @param author
   *          the author of the comment reply
   * @param creationDate
   *          the creation date
   * @param modificationDate
   *          the modification date
   * @throws IllegalArgumentException
   *           if some of the parameters aren't set
   */
  public static CommentReply create(Option<Long> id, String text, User author, Date creationDate, Date modificationDate) {
    return new CommentReply(id, text, author, creationDate, modificationDate);
  }

  private CommentReply(Option<Long> id, String text, User author, Date creationDate, Date modificationDate) {
    this.id = notNull(id, "id");
    this.text = notEmpty(text, "text");
    this.author = notNull(author, "author");
    this.creationDate = notNull(creationDate, "creationDate");
    this.modificationDate = notNull(modificationDate, "modificationDate");
  }

  /**
   * Returns the reply id
   *
   * @return the reply id
   */
  public Option<Long> getId() {
    return id;
  }

  /**
   * Returns the reply text
   *
   * @return the reply text
   */
  public String getText() {
    return text;
  }

  /**
   * Returns the reply creation date
   *
   * @return the reply creation date
   */
  public Date getCreationDate() {
    return creationDate;
  }

  /**
   * Returns the reply modification date
   *
   * @return the reply modification date
   */
  public Date getModificationDate() {
    return modificationDate;
  }

  /**
   * Returns the reply author
   *
   * @return the reply author
   */
  public User getAuthor() {
    return author;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    CommentReply reply = (CommentReply) o;
    return text.equals(reply.getText()) && creationDate.equals(reply.getCreationDate())
            && modificationDate.equals(reply.getModificationDate()) && author.equals(reply.getAuthor());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(text, creationDate, modificationDate, author);
  }

  @Override
  public String toString() {
    return "Comment reply:" + id + "|" + StringUtils.abbreviate(text, 25);
  }

  public Obj toJson() {
    Obj authorObj = Jsons.obj(Jsons.p("name", author.getName()), Jsons.p("username", author.getUsername()),
            Jsons.p("email", author.getEmail()));

    Val idValue = Jsons.ZERO_VAL;
    if (id.isSome())
      idValue = Jsons.v(id.get());

    return Jsons.obj(Jsons.p("id", idValue), Jsons.p("text", text), Jsons.p("author", authorObj),
            Jsons.p("creationDate", DateTimeSupport.toUTC(creationDate.getTime())),
            Jsons.p("modificationDate", DateTimeSupport.toUTC(modificationDate.getTime())));
  }

  public JValue toJValue() {
    JValue authorObj = j(f("name", vN(author.getName())), f("username", v(author.getUsername())),
            f("email", vN(author.getEmail())));

    JValue idValue = jz;
    if (id.isSome())
      idValue = v(id.get());

    List<JField> fields = new ArrayList<JField>();
    fields.add(f("id", idValue));
    fields.add(f("text", v(text)));
    fields.add(f("author", authorObj));
    fields.add(f("creationDate", v(DateTimeSupport.toUTC(creationDate.getTime()))));
    fields.add(f("modificationDate", v(DateTimeSupport.toUTC(modificationDate.getTime()))));

    return j(fields);
  }

}
