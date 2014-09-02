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
package org.opencastproject.loadtest.engage;

/** Data class that keeps track of the user/pass for the matterhorn gui as well as the names of those fields. **/
public class GuiSettings {
  /* The default username to enter into the gui. */
  public static String DEFAULT_GUI_USERNAME = "admin";
  /* The default password to enter into the gui. */
  public static String DEFAULT_GUI_PASSWORD = "opencast";
  /* The default name of the username field to find it using selenium. */
  public static String DEFAULT_GUI_USERNAME_FIELD_NAME = "j_username";
  /* The default name of the password field to find it using selenium. */
  public static String DEFAULT_GUI_PASSWORD_FIELD_NAME = "j_password";
  /* The username to enter into the gui. */
  private String username = DEFAULT_GUI_USERNAME;
  /* The password to enter into the gui. */
  private String password = DEFAULT_GUI_PASSWORD;
  /* The name of the username field to find it using selenium. */
  private String usernameFieldName = DEFAULT_GUI_USERNAME_FIELD_NAME;
  /* The name of the password field to find it using selenium. */
  private String passwordFieldName = DEFAULT_GUI_PASSWORD_FIELD_NAME;

  public String getUsername() {
    return username;
  }
  public void setUsername(String username) {
    this.username = username;
  }
  public String getPassword() {
    return password;
  }
  public void setPassword(String password) {
    this.password = password;
  }
  public String getUsernameFieldName() {
    return usernameFieldName;
  }
  public void setUsernameFieldName(String usernameFieldName) {
    this.usernameFieldName = usernameFieldName;
  }
  public String getPasswordFieldName() {
    return passwordFieldName;
  }
  public void setPasswordFieldName(String passwordFieldName) {
    this.passwordFieldName = passwordFieldName;
  }
}
