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
package org.opencastproject.security.api;

/**
 * An exception that indicates that a subject attempted to take an action for which it was not authorized.
 */
public class UnauthorizedException extends Exception {

  /** The java.io.serialization uid */
  private static final long serialVersionUID = 7717178990322180202L;

  /**
   * Constructs an UnauthorizedException using the specified message.
   *
   * @param message
   *          the message describing the reason for this exception
   */
  public UnauthorizedException(String message) {
    super(message);
  }

  /**
   * Constructs an UnauthorizedException for the specified user's attempt to take a specified action.
   *
   * @param user
   *          the current user
   * @param action
   *          the action attempted
   */
  public UnauthorizedException(User user, String action) {
    super(formatMessage(user, action, null));
  }

  /**
   * Constructs an UnauthorizedException for the specified user's attempt to take a specified action.
   *
   * @param user
   *          the current user
   * @param action
   *          the action attempted
   * @param acl
   *          the access control list that prevented the action
   */
  public UnauthorizedException(User user, String action, AccessControlList acl) {
    super(formatMessage(user, action, acl));
  }

  private static String formatMessage(User user, String action, AccessControlList acl) {
    StringBuilder sb = new StringBuilder();
    sb.append(user.toString());
    sb.append(" can not take action ");
    sb.append("'");
    sb.append(action);
    sb.append("'");
    if (acl != null) {
      sb.append(" according to ");
      sb.append(acl);
    }
    return sb.toString();
  }
}
