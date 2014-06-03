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

package org.opencastproject.feed.impl;

import org.opencastproject.feed.api.Person;

/**
 * TODO: Comment me
 */
public class PersonImpl implements Person {

  /** The person's name */
  private String name = null;

  /** Uri for that person */
  private String uri = null;

  /** The person's e-mail */
  private String email = null;

  /**
   * Creates a new person.
   *
   * @param name
   *          the person's name
   * @param email
   *          the person's e-mail address
   * @param uri
   *          the person's uri
   */
  public PersonImpl(String name, String email, String uri) {
    this.name = name;
    this.email = email;
    this.uri = uri;
  }

  /**
   * Creates a new person.
   *
   * @param name
   *          the person's name
   * @param email
   *          the person's e-mail address
   */
  public PersonImpl(String name, String email) {
    this(name, email, null);
  }

  /**
   * Creates a new person.
   *
   * @param name
   *          the person's name
   */
  public PersonImpl(String name) {
    this(name, null, null);
  }

  /**
   * @see org.opencastproject.feed.api.Person#getEmail()
   */
  public String getEmail() {
    return email;
  }

  /**
   * @see org.opencastproject.feed.api.Person#getName()
   */
  public String getName() {
    return name;
  }

  /**
   * @see org.opencastproject.feed.api.Person#getUri()
   */
  public String getUri() {
    return uri;
  }

  /**
   * @see org.opencastproject.feed.api.Person#setEmail(java.lang.String)
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * @see org.opencastproject.feed.api.Person#setName(java.lang.String)
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @see org.opencastproject.feed.api.Person#setUri(java.lang.String)
   */
  public void setUri(String uri) {
    this.uri = uri;
  }

}
