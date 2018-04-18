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

import static com.entwinemedia.fn.data.json.Jsons.BLANK;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static org.opencastproject.util.RequireUtil.notEmpty;
import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.security.api.User;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;

import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.JValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Business object for message templates.
 */
public class MessageTemplate {
  /** The pattern to input the current date into an email template. **/
  public static final String CURRENT_DATE_TEMPLATE_VARIABLE = "%current_date%";
  /** The pattern to input the instructor's title into the email template. **/
  public static final String INSTRUCTOR_TITLE_TEMPLATE_VARIABLE = "%instructor_title%";
  /** The pattern to input the name of the course into the email template. **/
  public static final String NAME_OF_COURSE_TEMPLATE_VARIABLE = "%name_of_course%";
  /** The pattern to input the sender's name into the email template **/
  public static final String SENDER_NAME_TEMPLATE_VARIABLE = "%sender_name%";

  /** The template id */
  private Long id;

  /** The message template name */
  private String name;

  /** The creator of the template */
  private User creator;

  /** Whether the template is hidden in the UI */
  private boolean hidden = false;

  /** The message template subject */
  private String subject;

  /** The message template body */
  private String body;

  /** The message template type */
  private TemplateType type;

  /** The message template creation date */
  private Date creationDate;

  /**
   * Creates a message template
   *
   * @param name
   *          the name
   * @param creator
   *          the creator of the template
   * @param subject
   *          the subject
   * @param body
   *          the body
   * @param creationDate
   *          the creation date
   */
  public MessageTemplate(String name, User creator, String subject, String body, TemplateType type, Date creationDate) {
    this.name = notEmpty(name, "name");
    this.creator = notNull(creator, "creator");
    this.subject = notEmpty(subject, "subject");
    this.body = notEmpty(body, "body");
    this.type = notNull(type, "type");
    this.creationDate = notNull(creationDate, "creationDate");
  }

  /**
   * Creates a message template with the current time as creation date and an empty list of comments
   *
   * @param name
   *          the name
   * @param creator
   *          the creator of the template
   * @param subject
   *          the subject
   * @param body
   *          the body
   */
  public MessageTemplate(String name, User creator, String subject, String body) {
    this.name = notEmpty(name, "name");
    this.creator = notNull(creator, "creator");
    this.subject = notEmpty(subject, "subject");
    this.body = notEmpty(body, "body");
    this.type = TemplateType.INVITATION;
    this.creationDate = new Date();
  }

  /**
   * Sets the id
   *
   * @param id
   *          the template id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the template id
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
   *          the creator of the template
   */
  public void setCreator(User creator) {
    this.creator = creator;
  }

  /**
   * Returns the creator
   *
   * @return the creator of the template
   */
  public User getCreator() {
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
   * Sets the template type
   *
   * @param type
   *          the template type
   */
  public void setType(TemplateType type) {
    this.type = type;
  }

  /**
   * Returns the template type
   *
   * @return the type
   */
  public TemplateType getType() {
    return type;
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

  public MessageTemplate createAdHocCopy() {
    Date now = new Date();
    MessageTemplate adhocCopy = new MessageTemplate(name, creator, subject, body, type, now);
    adhocCopy.setHidden(true);
    adhocCopy.setName(name.concat(" - ").concat(DateTimeSupport.toUTC(now.getTime())));
    return adhocCopy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    MessageTemplate template = (MessageTemplate) o;
    return name.equals(template.getName()) && subject.equals(template.getSubject()) && body.equals(template.getBody())
            && creationDate.equals(template.getCreationDate())
            && creator.equals(template.getCreator()) && type.equals(template.getType());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, name, subject, body, type, creationDate, creator);
  }

  @Override
  public String toString() {
    return "MessageSignature:" + name + "|" + type;
  }

  private Obj toJson(String subject, String body) {
    Obj creatorObj = Jsons.obj(Jsons.p("name", creator.getName()), Jsons.p("username", creator.getUsername()),
            Jsons.p("email", creator.getEmail()));
    return Jsons.obj(Jsons.p("id", id), Jsons.p("name", name), Jsons.p("subject", subject), Jsons.p("body", body),
            Jsons.p("creator", creatorObj), Jsons.p("hidden", hidden), Jsons.p("type", type.getType().toString()),
            Jsons.p("creationDate", creationDate));
  }

  public Obj toJson() {
    return toJson(getSubject(), getBody());
  }

  public JValue toJValue() {
    JObject creatorObj = obj(f("name", v(creator.getName(), BLANK)), f("username", v(creator.getUsername())),
            f("email", v(creator.getEmail(), BLANK)));

    List<Field> fields = new ArrayList<>();
    fields.add(f("id", v(id)));
    fields.add(f("name", v(name)));
    fields.add(f("subject", v(getSubject())));
    fields.add(f("body", v(body)));
    fields.add(f("creator", creatorObj));
    fields.add(f("hidden", v(hidden)));
    fields.add(f("type", v(type.getType().toString())));
    fields.add(f("creationDate", v(DateTimeSupport.toUTC(creationDate.getTime()))));
    return obj(fields);
  }
}
