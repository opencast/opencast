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
package org.opencastproject.messages.persistence;

import org.opencastproject.messages.EmailConfiguration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entity object for email configuration data.
 */
@Entity(name = "EmailConfiguration")
@Table(name = "mh_email_configuration", uniqueConstraints = { @UniqueConstraint(columnNames = { "organization" }) })
@NamedQueries({
        @NamedQuery(name = "EmailConfiguration.findAll", query = "SELECT e FROM EmailConfiguration e WHERE e.organization = :org"),
        @NamedQuery(name = "EmailConfiguration.clear", query = "DELETE FROM EmailConfiguration e WHERE e.organization = :org") })
public class EmailConfigurationDto {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "organization")
  private String organization;

  @Column(name = "transport")
  private String transport;

  @Column(name = "server", nullable = false)
  private String server;

  @Column(name = "username")
  private String userName;

  @Column(name = "password")
  private String password;

  @Column(name = "ssl_enabled", nullable = false)
  private boolean ssl = false;

  @Column(name = "port")
  private int port;

  /**
   * Default constructor
   */
  public EmailConfigurationDto() {
  }

  /**
   * Create an Email configuration
   * 
   * @param organization
   *          the organization
   * @param transport
   *          the mail transport
   * @param server
   *          the smtp server
   * @param port
   *          the server port
   * @param userName
   *          the username for the server
   * @param password
   *          the password for the server
   * @param ssl
   *          define is ssl has to be used for the connection
   */
  public EmailConfigurationDto(String organization, String transport, String server, int port, String userName,
          String password, boolean ssl) {
    this.organization = organization;
    this.transport = transport;
    this.server = server;
    this.port = port;
    this.userName = userName;
    this.password = password;
    this.ssl = ssl;
  }

  /**
   * Returns the id of this entity
   * 
   * @return the id as long
   */
  public long getId() {
    return id;
  }

  /**
   * Returns the organization
   * 
   * @return the organization
   */
  public String getOrganization() {
    return organization;
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

  /**
   * Returns the business object of this email configuration
   * 
   * @return the business object model of this email configuration
   */
  public EmailConfiguration toEmailConfiguration() {
    EmailConfiguration emailConfiguration = new EmailConfiguration(transport, server, port, userName, password, ssl);
    emailConfiguration.setId(id);
    return emailConfiguration;
  }

}
