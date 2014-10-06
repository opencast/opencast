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
package org.opencastproject.authorization.xacml.manager.impl.persistence;

import javax.persistence.RollbackException;

/**
 * Utility class for ACL Manager persistence
 */
public final class Util {

  /**
   * Private constructor to disable clients from instantiating this class.
   */
  private Util() {
  }

  /** DB exception handler that checks for unique constraint violation or duplicated entry. */
  public static boolean isConstraintViolationException(Exception e) {
    if (e instanceof RollbackException) {
      final Throwable cause = e.getCause();
      String message = cause.getMessage().toLowerCase();
      if (message.contains("unique") || message.contains("duplicate"))
        return true;
    }
    return false;
  }

}
