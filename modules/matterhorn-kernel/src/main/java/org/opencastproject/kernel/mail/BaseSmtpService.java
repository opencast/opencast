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
package org.opencastproject.kernel.mail;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

/**
 * Base implementation that allows to send e-mails using <code>javax.mail</code>.
 */
public class BaseSmtpService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(BaseSmtpService.class);

  /** The mode */
  protected enum Mode {
    production, test
  };

  /** Parameter prefix common to all "mail" properties */
  protected static final String OPT_MAIL_PREFIX = "mail.";

  /** Parameter name for the transport protocol */
  protected static final String OPT_MAIL_TRANSPORT = "mail.transport.protocol";

  /** Parameter suffix for the mail host */
  protected static final String OPT_MAIL_HOST_SUFFIX = ".host";

  /** Parameter suffix for the mail port */
  protected static final String OPT_MAIL_PORT_SUFFIX = ".port";

  /** Parameter suffix for the start tls status */
  protected static final String OPT_MAIL_TLS_ENABLE_SUFFIX = ".starttls.enable";

  /** Parameter suffix for the authentication setting */
  protected static final String OPT_MAIL_AUTH_SUFFIX = ".auth";

  /** Parameter name for the username */
  protected static final String OPT_MAIL_USER = "mail.user";

  /** Parameter name for the password */
  protected static final String OPT_MAIL_PASSWORD = "mail.password";

  /** Parameter name for the recipient */
  protected static final String OPT_MAIL_FROM = "mail.from";

  /** Parameter name for the debugging setting */
  protected static final String OPT_MAIL_DEBUG = "mail.debug";

  /** Default value for the transport protocol */
  private static final String DEFAULT_MAIL_TRANSPORT = "smtp";

  /** Default value for the mail port */
  private static final int DEFAULT_MAIL_PORT = 25;

  /** The current mail transport protocol */
  protected String mailTransport = DEFAULT_MAIL_TRANSPORT;

  /** Flag indicating whether the service is actually sending messages */
  private boolean productionMode = true;

  /** The mail host */
  private String host = null;

  /** The mail port */
  private int port = DEFAULT_MAIL_PORT;

  /** The mail user */
  private String user = null;

  /** The mail password */
  private String password = null;

  /** Whether debug is enabled */
  private boolean debug = false;

  /** Whether SSL/TLS is enabled */
  private boolean ssl = false;

  /** The optional sender */
  private String sender = null;

  /** The mail properties */
  private Properties mailProperties = new Properties();

  /** The default mail session */
  private Session defaultMailSession = null;

  public void setProductionMode(boolean mode) {
    this.productionMode = mode;
  }

  public boolean isProductionMode() {
    return productionMode;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setMailTransport(String mailTransport) {
    this.mailTransport = mailTransport;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public void setSsl(boolean ssl) {
    this.ssl = ssl;
  }

  public void configure() {
    mailProperties.clear();
    defaultMailSession = null;

    if (!("smtp".equals(mailTransport) || "smtps".equals(mailTransport))) {
      if (mailTransport != null)
        logger.warn("'{}' procotol not supported. Reverting to default: '{}'", mailTransport, DEFAULT_MAIL_TRANSPORT);
      logger.debug("Mail transport protocol defaults to '{}'", DEFAULT_MAIL_TRANSPORT);
      mailProperties.put(OPT_MAIL_TRANSPORT, DEFAULT_MAIL_TRANSPORT);
    } else {
      logger.debug("Mail transport protocol is '{}'", mailTransport);
      mailProperties.put(OPT_MAIL_TRANSPORT, mailTransport);
    }

    mailProperties.put(OPT_MAIL_PREFIX + mailTransport + OPT_MAIL_HOST_SUFFIX, host);
    logger.debug("Mail host is {}", host);

    mailProperties.put(OPT_MAIL_PREFIX + mailTransport + OPT_MAIL_PORT_SUFFIX, port);
    logger.debug("Mail server port is '{}'", port);

    // User and Authentication
    String propName = OPT_MAIL_PREFIX + mailTransport + OPT_MAIL_AUTH_SUFFIX;
    if (StringUtils.isNotBlank(user)) {
      mailProperties.put(OPT_MAIL_USER, user);
      mailProperties.put(propName, Boolean.toString(true));
      logger.debug("Mail user is '{}'", user);
    } else {
      mailProperties.put(propName, Boolean.toString(false));
      logger.debug("Sending mails to {} without authentication", host);
    }

    if (StringUtils.isNotBlank(password)) {
      mailProperties.put(OPT_MAIL_PASSWORD, password);
      logger.debug("Mail password set");
    }

    if (StringUtils.isNotBlank(sender)) {
      mailProperties.put(OPT_MAIL_FROM, sender);
      logger.debug("Mail sender is '{}'", sender);
    } else {
      logger.debug("Mail sender defaults not set");
    }

    mailProperties.put(OPT_MAIL_PREFIX + mailTransport + OPT_MAIL_TLS_ENABLE_SUFFIX, ssl);
    if (ssl) {
      logger.debug("TLS over SMTP is enabled");
    } else {
      logger.debug("TLS over SMTP is disabled");
    }

    mailProperties.put(OPT_MAIL_DEBUG, Boolean.toString(debug));
    logger.debug("Mail debugging is {}", debug ? "enabled" : "disabled");

    logger.info("Mail service configured with {}", host);
    Properties props = getSession().getProperties();
    for (String key : props.stringPropertyNames())
      logger.info("{}: {}", key, props.getProperty(key));
  }

  /**
   * Returns the default mail session that can be used to create a new message.
   *
   * @return the default mail session
   */
  public Session getSession() {
    if (defaultMailSession == null) {
      defaultMailSession = Session.getInstance(mailProperties);
    }
    return defaultMailSession;
  }

  /**
   * Creates a new message.
   *
   * @return the new message
   */
  public MimeMessage createMessage() {
    return new MimeMessage(getSession());
  }

  /**
   * Sends <code>message</code> using the configured transport.
   *
   * @param message
   *          the message
   * @throws MessagingException
   *           if sending the message failed
   */
  public void send(MimeMessage message) throws MessagingException {
    if (!productionMode) {
      logger.debug("Skipping sending of message {} due to test mode", message);
      return;
    }
    Transport t = getSession().getTransport(mailTransport);
    try {
      if (user != null)
        t.connect(user, password);
      else
        t.connect();
      t.sendMessage(message, message.getAllRecipients());
    } finally {
      t.close();
    }
  }

}
