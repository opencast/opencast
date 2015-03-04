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
package org.opencastproject.pm.api.util;

import org.opencastproject.util.Crypt;

import java.security.Key;

public final class Security {

  private Security() {
  }

  public static final Key TEACHER_EMAIL_KEY = Crypt.createKey("this_is_secure");

  /**
   * Simple way of encoding a teacher e-mail address to
   * 
   * @param args
   *          the commandline arguments
   */
  public static void main(String[] args) {
    if (args.length == 1) {
      System.out.println(Crypt.encrypt(Security.TEACHER_EMAIL_KEY, args[0]));
    } else {
      System.err.println("Usage: java -jar matterhorn-participation-api.jar <email address>");
    }
  }

}
