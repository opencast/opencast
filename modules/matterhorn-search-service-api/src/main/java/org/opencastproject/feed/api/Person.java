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

package org.opencastproject.feed.api;

/**
 * This interface provides the methods for defining a person object contained in a feed like authors and contributors.
 * <p>
 * Note that this interface is heavily inspired and backed by the excellent rss/atom feed library <tt>Rome</tt>
 * (http://https://rome.dev.java.net).
 */
public interface Person {

  /**
   * Returns name of person.
   *
   * @return the name
   */
  String getName();

  /**
   * Sets name of person.
   *
   * @param name
   *          the name
   */
  void setName(String name);

  /**
   * Returns URI of person.
   *
   * @return the uri
   */
  String getUri();

  /**
   * Sets URI of person.
   *
   * @param uri
   *          the uri identifying the person
   */
  void setUri(String uri);

  /**
   * Returns email of person.
   */
  String getEmail();

  /**
   * Sets email of person.
   *
   * @param email
   *          the email address
   */
  void setEmail(String email);

}
