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
package org.opencastproject.messages;

import static org.opencastproject.util.RequireUtil.notEmpty;

import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;

/**
 * Business object for email configuration data.
 */
public class EmailConfiguration {

  /** The configuration identifier */
  private Long id;

  /** The mail transport */
  private String transport;

  /** The server address */
  private String server;

  /** The server port */
  private int port;

  /** The user name */
  private String userName;

  /** The password */
  private String password;

  /** The SSL flag */
  private boolean ssl;

  /** The default email configuration */
  public static final EmailConfiguration DEFAULT = new EmailConfiguration("smtp", "localhost", 8080, "user",
          "password", false);

  /**
   * Creates and email configuration.
   * 
   * @param transport
   *          the mail transport
   * @param server
   *          the server address
   * @param port
   *          the server port
   * @param userName
   *          the user name
   * @param password
   *          the password
   * @param ssl
   *          whether SSL is activate or not
   */
  public EmailConfiguration(String transport, String server, int port, String userName, String password, boolean ssl) {
    this.server = notEmpty(server, "server");
    this.transport = transport;
    this.port = port;
    this.userName = userName;
    this.password = password;
    this.ssl = ssl;
  }

  /**
   * Sets the id
   * 
   * @param id
   *          the configuration id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the configuration id
   * 
   * @return the id
   */
  public Long getId() {
    return this.id;
  }

  /**
   * Sets the mail transport
   * 
   * @param transport
   *          the mail transport
   */
  public void setTransport(String transport) {
    this.transport = transport;
  }

  /**
   * Returns the mail transport
   * 
   * @return the mail transport
   */
  public String getTransport() {
    return transport;
  }

  /**
   * Sets the server address
   * 
   * @param server
   *          the server address
   */
  public void setServer(String server) {
    this.server = server;
  }

  /**
   * Returns the server address
   * 
   * @return the server address
   */
  public String getServer() {
    return server;
  }

  /**
   * Sets the user name
   * 
   * @param userName
   *          the user name
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * Returns the user name
   * 
   * @return the user name
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Sets the password
   * 
   * @param password
   *          the password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Returns the password
   * 
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the SSL flag
   * 
   * @param ssl
   *          the SSL flag
   */
  public void setSsl(boolean ssl) {
    this.ssl = ssl;
  }

  /**
   * Returns the SSL flag
   * 
   * @return the SSL flag
   */
  public boolean isSsl() {
    return ssl;
  }

  /**
   * Sets the port
   * 
   * @param port
   *          the port
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Returns the port
   * 
   * @return the port
   */
  public int getPort() {
    return port;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    EmailConfiguration config = (EmailConfiguration) o;
    return transport.equals(config.getTransport()) && server.equals(config.getServer()) && port == config.getPort()
            && userName.equals(config.getUserName()) && password.equals(config.getPassword()) && ssl == config.isSsl();
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, transport, server, port, userName, password, ssl);
  }

  @Override
  public String toString() {
    return "EmailConfiguration:" + server + port + "/username=" + userName;
  }

  public Obj toJson() {
    return Jsons.obj(Jsons.p("id", id), Jsons.p("transport", transport), Jsons.p("server", server),
            Jsons.p("port", port), Jsons.p("username", userName), Jsons.p("password", password), Jsons.p("ssl", ssl));
  }

}
