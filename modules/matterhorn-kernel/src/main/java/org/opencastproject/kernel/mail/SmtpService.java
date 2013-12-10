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

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
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

  /** Parameter prefix common to all "mail" properties */
  private static final String OPT_MAIL_PREFIX = "mail.";

  /** Parameter name for the transport protocol */
  private static final String OPT_MAIL_TRANSPORT = "mail.transport.protocol";

  /** Parameter suffix for the mail host */
  private static final String OPT_MAIL_HOST_SUFFIX = ".host";

  /** Parameter suffix for the mail port */
  private static final String OPT_MAIL_PORT_SUFFIX = ".port";

  /** Parameter suffix for the start tls status */
  private static final String OPT_MAIL_TLS_ENABLE_SUFFIX = ".starttls.enable";

  /** Parameter suffix for the authentication setting */
  private static final String OPT_MAIL_AUTH_SUFFIX = ".auth";

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
  private static final String DEFAULT_MAIL_PORT = "25";

  /** The mail properties */
  private Properties mailProperties = new Properties();

  /** The mail host */
  private String mailHost = null;

  /** The mail user */
  private String mailUser = null;

  /** The mail password */
  private String mailPassword = null;

  /** The default mail session */
  private Session defaultMailSession = null;

  /** The current mail transport protocol */
  private String mailTransport = null;

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

    // Mail transport protocol
    mailTransport = StringUtils.trimToNull((String) properties.get(OPT_MAIL_TRANSPORT));
    if (!("smtp".equals(mailTransport) || "smtps".equals(mailTransport))) {
      if (mailTransport != null)
        logger.warn("'{}' procotol not supported. Reverting to default: '{}'", mailTransport, DEFAULT_MAIL_TRANSPORT);
      mailTransport = DEFAULT_MAIL_TRANSPORT;
      logger.debug("Mail transport protocol defaults to '{}'", mailTransport);
    } else {
      logger.debug("Mail transport protocol is '{}'", mailTransport);
    }
    logger.info("Mail transport protocol is '{}'", mailTransport);
    mailProperties.put(OPT_MAIL_TRANSPORT, mailTransport);

    // The mail host is mandatory
    String propName = OPT_MAIL_PREFIX + mailTransport + OPT_MAIL_HOST_SUFFIX;
    mailHost = StringUtils.trimToNull((String) properties.get(propName));
    if (mailHost == null)
      throw new ConfigurationException(propName, "is not set");
    logger.debug("Mail host is {}", mailHost);
    mailProperties.put(propName, mailHost);

    // Mail port
    propName = OPT_MAIL_PREFIX + mailTransport + OPT_MAIL_PORT_SUFFIX;
    String mailPort = StringUtils.trimToNull((String) properties.get(propName));
    if (mailPort == null) {
      mailPort = DEFAULT_MAIL_PORT;
      logger.debug("Mail server port defaults to '{}'", mailPort);
    } else {
      logger.debug("Mail server port is '{}'", mailPort);
    }
    mailProperties.put(propName, mailPort);

    // TSL over SMTP support
    propName = OPT_MAIL_PREFIX + mailTransport + OPT_MAIL_TLS_ENABLE_SUFFIX;
    String smtpStartTLSStr = StringUtils.trimToNull((String) properties.get(propName));
    boolean smtpStartTLS = Boolean.parseBoolean(smtpStartTLSStr);
    if (smtpStartTLS) {
      mailProperties.put(propName, "true");
      logger.debug("TLS over SMTP is enabled");
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
    propName = OPT_MAIL_PREFIX + mailTransport + OPT_MAIL_AUTH_SUFFIX;
    mailProperties.put(propName, Boolean.toString(mailUser != null));

    // Mail debugging
    String mailDebug = StringUtils.trimToNull((String) properties.get(OPT_MAIL_DEBUG));
    if (mailDebug != null) {
      boolean mailDebugEnabled = Boolean.parseBoolean(mailDebug);
      mailProperties.put(OPT_MAIL_DEBUG, Boolean.toString(mailDebugEnabled));
      logger.info("Mail debugging is {}", mailDebugEnabled ? "enabled" : "disabled");
    }

    defaultMailSession = null;
    logger.info("Mail service configured with {}", mailHost);

    Properties props = getSession().getProperties();
    for (String key : props.stringPropertyNames())
      logger.info("{}: {}", key, props.getProperty(key));

    // Test
    String mailTest = StringUtils.trimToNull((String) properties.get(OPT_MAIL_TEST));
    if (mailTest != null && Boolean.parseBoolean(mailTest)) {
      logger.info("Sending test message to {}", mailFrom);
      try {
        sendTestMessage(mailFrom);
      } catch (MessagingException e) {
        logger.error("Error sending test message to " + mailFrom + ": " + e.getMessage());
        while (e.getNextException() != null) {
          Exception ne = e.getNextException();
          logger.error("Error sending test message to " + mailFrom + ": " + ne.getMessage());
          if (ne instanceof MessagingException)
            e = (MessagingException) ne;
          else
            break;
        }
        throw new ConfigurationException(OPT_MAIL_PREFIX + mailTransport + OPT_MAIL_HOST_SUFFIX,
                "Failed to send test message to " + mailFrom);
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
    Transport t = getSession().getTransport(mailTransport);
    try {
      if (mailUser != null)
        t.connect(mailUser, mailPassword);
      else
        t.connect();
      t.sendMessage(message, message.getAllRecipients());
    } finally {
      t.close();
    }
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
