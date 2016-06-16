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

package org.opencastproject.pm.api;

import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.EqualsUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Business object for synchronization data.
 */
public class Synchronization {

  /** The synchronization id */
  private Long id;

  /** The synchronization date */
  private Date date;

  /** The list of synchronization errors */
  private List<Error> errors = new ArrayList<Error>();

  /** The list of synchronized recordings */
  private List<SynchronizedRecording> synchronizedRecordings = new ArrayList<SynchronizedRecording>();

  /**
   * Creates a synchronization point without errors
   * 
   * @param date
   *          the synchronization date
   */
  public Synchronization(Date date) {
    this.date = notNull(date, "date");
  }

  /**
   * Creates a synchronization point without errors and with the current time as synchronization date
   */
  public Synchronization() {
    this.date = new Date();
  }

  /**
   * Creates a synchronization point with errors
   * 
   * @param date
   *          the synchronization date
   * @param errors
   *          the error list
   * @param synchronisedRecordings
   *          the synchronized recordings
   */
  public Synchronization(Date date, List<Error> errors, List<SynchronizedRecording> synchronisedRecordings) {
    this.date = notNull(date, "date");
    this.errors = new ArrayList<Error>(notNull(errors, "errors"));
    this.synchronizedRecordings = new ArrayList<SynchronizedRecording>(notNull(synchronisedRecordings,
            "synchronizedRecordings"));
  }

  /**
   * Sets the id
   * 
   * @param id
   *          the synchronization id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the synchronization id
   * 
   * @return the id
   */
  public Long getId() {
    return this.id;
  }

  /**
   * Sets a synchronization error list.
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
   * Add an error to the synchronization
   * 
   * @param error
   *          the error to add to this synchronization
   * 
   * @return true if this collection changed as a result of the call
   */
  public boolean addError(Error error) {
    return errors.add(notNull(error, "error"));
  }

  /**
   * Remove an error from the synchronization
   * 
   * @param error
   *          the error to remove from this synchronization
   * 
   * @return true if this collection changed as a result of the call
   */
  public boolean removeError(Error error) {
    return errors.remove(notNull(error, "error"));
  }

  /**
   * Sets a synchronized recordings list.
   * 
   * @param synchronizedRecordings
   *          the synchronized recordings list
   */
  public void setSynchronizedRecordings(List<SynchronizedRecording> synchronizedRecordings) {
    this.synchronizedRecordings = new ArrayList<SynchronizedRecording>(notNull(synchronizedRecordings,
            "synchronizedRecordings"));
  }

  /**
   * Returns the synchronized recordings list.
   * 
   * @return the synchronized recordings list
   */
  public List<SynchronizedRecording> getSynchronizedRecordings() {
    return synchronizedRecordings;
  }

  /**
   * Add an error to the synchronized recordings list.
   * 
   * @param synchronizedRecording
   *          the synchronized recordings list.
   * 
   * @return true if this collection changed as a result of the call
   */
  public boolean addSynchronizedRecording(SynchronizedRecording synchronizedRecording) {
    return synchronizedRecordings.add(notNull(synchronizedRecording, "synchronizedRecording"));
  }

  /**
   * Remove an error from the synchronized recordings list.
   * 
   * @param synchronizedRecording
   *          the synchronized recordings list.
   * 
   * @return true if this collection changed as a result of the call
   */
  public boolean removeSynchronizedRecording(SynchronizedRecording synchronizedRecording) {
    return synchronizedRecordings.remove(notNull(synchronizedRecording, "synchronizedRecording"));
  }

  /**
   * Sets the synchronization date
   * 
   * @param date
   *          the date
   */
  public void setDate(Date date) {
    this.date = date;
  }

  /**
   * Returns the synchronization date
   * 
   * @return the synchronization date
   */
  public Date getDate() {
    return date;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Synchronization sync = (Synchronization) o;
    return date.equals(sync.getDate()) && errors.equals(sync.getErrors())
            && synchronizedRecordings.equals(sync.getSynchronizedRecordings());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, date, errors, synchronizedRecordings);
  }

  @Override
  public String toString() {
    return "Synchronization:" + DateTimeSupport.toUTC(date.getTime());
  }

}
