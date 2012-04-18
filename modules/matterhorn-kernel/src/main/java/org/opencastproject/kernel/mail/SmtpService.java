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

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * OSGi service that allows to send e-mails using <code>javax.mail</code>.
 */
public class SmtpService implements ManagedService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SmtpService.class);

  /** Parameter name for the transport protocol */
  private static final String OPT_MAIL_TRANSPORT = "mail.transport.protocol";

  /** Parameter name for the mail host */
  private static final String OPT_MAIL_HOST = "mail.smtp.host";

  /** Parameter name for the mail port */
  private static final String OPT_MAIL_PORT = "mail.smtp.port";

  /** Parameter name for the start tls status */
  private static final String OPT_MAIL_TLS_ENABLE = "mail.smtp.starttls.enable";

  /** Parameter name for the authentication setting */
  private static final String OPT_MAIL_AUTH = "mail.smtp.auth";

  /** Parameter name for the username */
  private static final String OPT_MAIL_USER = "mail.user";

  /** Parameter name for the password */
  private static final String OPT_MAIL_PASSWORD = "mail.password";

  /** Parameter name for the recipient */
  private static final String OPT_MAIL_FROM = "mail.from";

  /** Parameter name for the debugging setting */
  private static final String OPT_MAIL_DEBUG = "mail.debug";

  /** Parameter name for the test setting */
  private static final String OPT_MAIL_TEST = "mail.test";

  /** Default value for the transport protocol */
  private static final String DEFAULT_MAIL_TRANSPORT = "smtp";

  /** Default value for the mail port */
  private static final String DEFAULT_MAIL_PORT = "587";

  /** The mail properties */
  private Properties mailProperties = new Properties();

  /** Authenticator for a mail session */
  private Authenticator authenticator = null;

  /** The mail host */
  private String mailHost = null;

  /** The mail user */
  private String mailUser = null;

  /** The mail password */
  private String mailPassword = null;
  
  /** The default mail session */
  private Session defaultMailSession = null;

  /**
   * Callback from the OSGi <code>ConfigurationAdmin</code> on configuration changes.
   * 
   * @param properties
   *          the configuration properties
   * @throws ConfigurationException
   *           if configuration fails
   */
  @SuppressWarnings("rawtypes")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {

    // Read the mail server properties
    mailProperties.clear();

    // The mail host is mandatory
    mailHost = StringUtils.trimToNull((String) properties.get(OPT_MAIL_HOST));
    if (mailHost == null)
      throw new ConfigurationException(OPT_MAIL_HOST, "is not set");
    logger.debug("Mail host is {}", mailHost);
    mailProperties.put(OPT_MAIL_HOST, mailHost);

    // Mail port
    String mailPort = StringUtils.trimToNull((String) properties.get(OPT_MAIL_PORT));
    if (mailPort == null) {
      mailPort = DEFAULT_MAIL_PORT;
      logger.debug("Mail server port defaults to '{}'", mailPort);
    } else {
      logger.debug("Mail server port is '{}'", mailPort);
    }
    mailProperties.put(OPT_MAIL_PORT, mailPort);

    // Mail transport protocol
    String mailTransport = StringUtils.trimToNull((String) properties.get(OPT_MAIL_TRANSPORT));
    if (mailTransport == null) {
      mailTransport = DEFAULT_MAIL_TRANSPORT;
      logger.debug("Mail transport protocol defaults to '{}'", mailTransport);
    } else {
      logger.debug("Mail transport protocol is '{}'", mailTransport);
    }
    mailProperties.put(OPT_MAIL_TRANSPORT, mailTransport);

    // TSL over SMTP support
    String smtpStartTLS = StringUtils.trimToNull((String) properties.get(OPT_MAIL_TLS_ENABLE));
    if (smtpStartTLS != null) {
      mailProperties.put(OPT_MAIL_TLS_ENABLE, smtpStartTLS);
      logger.debug("TLS over SMTP is '{}'", smtpStartTLS);
    } else {
      logger.debug("TLS over SMTP is disabled");
    }

    // Mail user
    mailUser = StringUtils.trimToNull((String) properties.get(OPT_MAIL_USER));
    if (mailUser != null) {
      mailProperties.put(OPT_MAIL_USER, mailUser);
      logger.debug("Mail user is '{}'", mailUser);
    } else {
      logger.debug("Sending mails to {} without authentication", mailHost);
    }

    // Mail password
    mailPassword = StringUtils.trimToNull((String) properties.get(OPT_MAIL_PASSWORD));
    if (mailPassword != null) {
      mailProperties.put(OPT_MAIL_PASSWORD, mailPassword);
      logger.debug("Mail password set");
    }

    // Mail sender
    String mailFrom = StringUtils.trimToNull((String) properties.get(OPT_MAIL_FROM));
    if (mailFrom == null) {
      logger.debug("Mail sender defaults to {}", mailFrom);
    } else {
      logger.debug("Mail sender is '{}'", mailFrom);
    }
    mailProperties.put(OPT_MAIL_FROM, mailFrom);

    // Authentication
    mailProperties.put(OPT_MAIL_AUTH, Boolean.toString(mailUser != null));

    // Mail debugging
    String mailDebug = StringUtils.trimToNull((String) properties.get(OPT_MAIL_DEBUG));
    if (mailDebug != null) {
      boolean mailDebugEnabled = Boolean.parseBoolean(mailDebug);
      mailProperties.put(OPT_MAIL_DEBUG, Boolean.toString(mailDebugEnabled));
      logger.info("Mail debugging is {}", mailDebugEnabled ? "enabled" : "disabled");
    }

    // Register the password authenticator
    authenticator = new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(mailUser, mailPassword);
      }
    };

    defaultMailSession = null;
    logger.info("Mail service configured with {}", mailHost);

    // Test
    String mailTest = StringUtils.trimToNull((String) properties.get(OPT_MAIL_TEST));
    if (mailTest != null && Boolean.parseBoolean(mailTest)) {
      logger.info("Sending test message to {}", mailFrom);
      try {
        sendTestMessage(mailFrom);
      } catch (MessagingException e) {
        logger.error("Error sending test message to " + mailFrom + ": " + e.getMessage());
        throw new ConfigurationException(OPT_MAIL_HOST, "Failed to send test message to " + mailFrom);
      }
    }
  }

  /**
   * Returns the default mail session that can be used to create a new message.
   * 
   * @return the default mail session
   */
  public Session getSession() {
    if (defaultMailSession == null) {
      defaultMailSession = Session.getInstance(mailProperties, authenticator);
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
    Transport.send(message);
  }

  /**
   * Method to send a test message.
   * 
   * @throws MessagingException
   *           if sending the message failed
   */
  private void sendTestMessage(String recipient) throws MessagingException {
    MimeMessage message = createMessage();
    message.addRecipient(RecipientType.TO, new InternetAddress(recipient));
    message.setSubject("Test from Matterhorn");
    message.setText("Hello world");
    message.saveChanges();
    send(message);
  }

}
