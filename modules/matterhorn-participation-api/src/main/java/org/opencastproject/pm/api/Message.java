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
package org.opencastproject.pm.api;

import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.messages.MessageSignature;
import org.opencastproject.messages.MessageTemplate;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;
import org.opencastproject.util.Jsons.Val;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Business object for a messages.
 */
public class Message {

  /** The message identifier */
  private Long id;

  /** The message creation date */
  private Date creationDate;

  /** The creator of the message */
  private Person creator;

  /** The list of message errors */
  private List<Error> errors = new ArrayList<Error>();

  /** The message template */
  private MessageTemplate template;

  /** The message signature */
  private MessageSignature signature;

  /** Whether to insert the signature body to the template body */
  private boolean insertSignature = false;

  /**
   * Creates a message
   * 
   * @param creationDate
   *          the creationDate
   * @param creator
   *          the creator of the message
   * @param template
   *          the template
   * @param signature
   *          the signature
   * @param errors
   *          the list of errors related to the message
   */
  public Message(Date creationDate, Person creator, MessageTemplate template, MessageSignature signature,
          List<Error> errors) {
    this.creationDate = notNull(creationDate, "creationDate");
    this.creator = notNull(creator, "creator");
    this.template = notNull(template, "template");
    this.signature = notNull(signature, "signature");
    this.errors = errors;
  }

  /**
   * Creates a message with the current time as creation date
   * 
   * @param creator
   *          the creator of the message
   * @param template
   *          the template
   * @param signature
   *          the signature
   */
  public Message(Person creator, MessageTemplate template, MessageSignature signature) {
    this.creationDate = new Date();
    this.creator = notNull(creator, "creator");
    this.template = notNull(template, "template");
    this.signature = notNull(signature, "signature");
  }

  /**
   * Creates a message with the current time as creation date
   * 
   * @param creator
   *          the creator of the message
   * @param template
   *          the template
   * @param signature
   *          the signature
   */
  public Message(Person creator, MessageTemplate template, MessageSignature signature, boolean insertSignature) {
    this(creator, template, signature);
    this.insertSignature = insertSignature;
  }

  /**
   * Sets the id
   * 
   * @param id
   *          the message id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the message id
   * 
   * @return the id
   */
  public Long getId() {
    return this.id;
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
   * Sets the creator
   * 
   * @param creator
   *          the creator of the message
   */
  public void setCreator(Person creator) {
    this.creator = creator;
  }

  /**
   * Returns the creator
   * 
   * @returns the creator of the message
   */
  public Person getCreator() {
    return this.creator;
  }

  /**
   * Sets the message template
   * 
   * @param template
   *          the message template
   */
  public void setTemplate(MessageTemplate template) {
    this.template = template;
  }

  /**
   * Returns the message template
   * 
   * @return the message template
   */
  public MessageTemplate getTemplate() {
    return template;
  }

  /**
   * Sets the message signature
   * 
   * @param signature
   *          the message signature
   */
  public void setSignature(MessageSignature signature) {
    this.signature = signature;
  }

  /**
   * Returns the message signature
   * 
   * @return the message signature
   */
  public MessageSignature getSignature() {
    return signature;
  }

  /**
   * Returns whether the message should insert the signature body
   * 
   * @return whether the message should insert the signature body
   */
  public boolean isInsertSignature() {
    return insertSignature;
  }

  /**
   * Sets a message error list.
   * 
   * @param errors
   *          the error list
   */
  public void setErrors(List<Error> errors) {
    this.errors = notNull(errors, "errors");
  }

  /**
   * Returns the error list
   * 
   * @return the error list
   */
  public List<Error> getErrors() {
    return errors;
  }

  /**
   * Add an error to the message
   * 
   * @param error
   *          the error to add to this message
   * 
   * @return true if this collection changed as a result of the call
   */
  public boolean addError(Error error) {
    return errors.add(notNull(error, "error"));
  }

  /**
   * Remove an error from the message
   * 
   * @param error
   *          the error to remove from this message
   * 
   * @return true if this collection changed as a result of the call
   */
  public boolean removeError(Error error) {
    return errors.remove(notNull(error, "error"));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Message msg = (Message) o;
    return creationDate.equals(msg.getCreationDate()) && template.equals(msg.getTemplate())
            && creator.equals(msg.getCreator()) && errors.equals(msg.getErrors())
            && signature.equals(msg.getSignature());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, creationDate, creator, template, signature, errors);
  }

  @Override
  public String toString() {
    return "Message:" + id + "|" + DateTimeSupport.toUTC(creationDate.getTime());
  }

  public Obj toJson() {
    List<Val> errorArr = new ArrayList<Val>();
    for (Error e : errors) {
      errorArr.add(e.toJson());
    }
    return Jsons.obj(Jsons.p("id", id), Jsons.p("creationDate", creationDate), Jsons.p("person", creator.toJson()),
            Jsons.p("template", template.toJson()), Jsons.p("signature", signature.toJson()),
            Jsons.p("errors", Jsons.arr(errorArr)));
  }
}
