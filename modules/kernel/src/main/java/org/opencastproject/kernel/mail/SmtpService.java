/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.kernel.mail;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * OSGi service that allows to send e-mails using <code>javax.mail</code>.
 */
public class SmtpService extends BaseSmtpService implements ManagedService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SmtpService.class);

  /** Parameter name for the test setting */
  private static final String OPT_MAIL_TEST = "mail.test";

  /** Parameter name for the mode setting */
  private static final String OPT_MAIL_MODE = "mail.mode";

  /**
   * Pattern to split strings containing lists of emails. This pattern matches any number of contiguous spaces or commas
   */
  private static final String SPLIT_PATTERN = "[\\s,]+";

  /** Define the MIME type for HTML mail content */
  private static final String TEXT_HTML = "text/html";

  /**
   * Callback from the OSGi <code>ConfigurationAdmin</code> on configuration changes.
   *
   * @param properties
   *          the configuration properties
   * @throws ConfigurationException
   *           if configuration fails
   */
  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {

    // Production or test mode
    String optMode = StringUtils.trimToNull((String) properties.get(OPT_MAIL_MODE));
    if (optMode != null) {
      try {
        Mode mode = Mode.valueOf(optMode);
        setProductionMode(Mode.production.equals(mode));
      } catch (Exception e) {
        logger.error("Error parsing smtp service mode '{}': {}", optMode, e.getMessage());
        throw new ConfigurationException(OPT_MAIL_MODE, e.getMessage());
      }
    }
    logger.info("Smtp service is in {} mode", isProductionMode() ? "production" : "test");

    // Mail transport protocol
    String optMailTransport = StringUtils.trimToNull((String) properties.get(OPT_MAIL_TRANSPORT));
    if (StringUtils.isNotBlank(optMailTransport))
      setMailTransport(optMailTransport);

    // The mail host is mandatory
    String propName = OPT_MAIL_PREFIX + mailTransport + OPT_MAIL_HOST_SUFFIX;
    String mailHost = (String) properties.get(propName);
    if (StringUtils.isBlank(mailHost))
      throw new ConfigurationException(propName, "is not set");
    setHost(mailHost);

    // Mail port
    propName = OPT_MAIL_PREFIX + mailTransport + OPT_MAIL_PORT_SUFFIX;
    String mailPort = (String) properties.get(propName);
    if (StringUtils.isNotBlank(mailPort))
      setPort(Integer.parseInt(mailPort));

    // TSL over SMTP support
    propName = OPT_MAIL_PREFIX + mailTransport + OPT_MAIL_TLS_ENABLE_SUFFIX;
    setSsl(BooleanUtils.toBoolean((String) properties.get(propName)));

    // Mail user
    String mailUser = (String) properties.get(OPT_MAIL_USER);
    if (StringUtils.isNotBlank(mailUser))
      setUser(mailUser);

    // Mail password
    String mailPassword = (String) properties.get(OPT_MAIL_PASSWORD);
    if (StringUtils.isNotBlank(mailPassword))
      setPassword(mailPassword);

    // Mail sender
    String mailFrom = (String) properties.get(OPT_MAIL_FROM);
    if (StringUtils.isNotBlank(mailFrom))
      setSender(mailFrom);

    // Mail debugging
    setDebug(BooleanUtils.toBoolean((String) properties.get(OPT_MAIL_DEBUG)));

    configure();

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
   * Method to send a test message.
   *
   * @throws MessagingException
   *           if sending the message failed
   */
  private void sendTestMessage(String recipient) throws MessagingException {
    send(recipient, "Test from Opencast", "Hello world");
  }

  /**
   * Method to send a message
   *
   * @param to
   *          Recipient of the message
   * @param subject
   *          Subject of the message
   * @param body
   *          Body of the message
   * @throws MessagingException
   *           if sending the message failed
   */
  public void send(String to, String subject, String body) throws MessagingException {
    send(to, subject, body, false);
  }

  /**
   * Method to send a message
   *
   * @param to
   *          Recipient of the message
   * @param subject
   *          Subject of the message
   * @param body
   *          Body of the message
   * @param isHTML
   *          Is the body of the message in HTML
   * @throws MessagingException
   *           if sending the message failed
   */
  public void send(String to, String subject, String body, Boolean isHTML) throws MessagingException {
    MimeMessage message = createMessage();
    message.addRecipient(RecipientType.TO, new InternetAddress(to));
    message.setSubject(subject);
    if (isHTML) {
        message.setContent(body, TEXT_HTML);
    } else {
        message.setText(body);
    }
    message.saveChanges();
    send(message);
  }

  /**
   * Send a message to multiple recipients
   *
   * @param to
   *          "To:" message recipient(s), separated by commas and/or spaces
   * @param cc
   *          "CC:" message recipient(s), separated by commas and/or spaces
   * @param bcc
   *          "BCC:" message recipient(s), separated by commas and/or spaces
   * @param subject
   *          Subject of the message
   * @param body
   *          Body of the message
   * @throws MessagingException
   *           if sending the message failed
   */
  public void send(String to, String cc, String bcc, String subject, String body) throws MessagingException {
    send(to, cc, bcc, subject, body, false);
  }

  /**
   * Send a message to multiple recipients
   *
   * @param to
   *          "To:" message recipient(s), separated by commas and/or spaces
   * @param cc
   *          "CC:" message recipient(s), separated by commas and/or spaces
   * @param bcc
   *          "BCC:" message recipient(s), separated by commas and/or spaces
   * @param subject
   *          Subject of the message
   * @param body
   *          Body of the message
   * @param isHTML
   *          Is the body of the message in HTML
   * @throws MessagingException
   *           if sending the message failed
   */
  public void send(String to, String cc, String bcc, String subject, String body, Boolean isHTML) throws MessagingException {
    String[] toArray = null;
    String[] ccArray = null;
    String[] bccArray = null;

    if (to != null)
      toArray = to.trim().split(SPLIT_PATTERN, 0);

    if (cc != null)
      ccArray = cc.trim().split(SPLIT_PATTERN, 0);

    if (bcc != null)
      bccArray = bcc.trim().split(SPLIT_PATTERN, 0);

    send(toArray, ccArray, bccArray, subject, body, isHTML);
  }

  /**
   * Send a message to multiple recipients
   *
   * @param to
   *          Array with the "To:" recipients of the message
   * @param cc
   *          Array with the "CC:" recipients of the message
   * @param bcc
   *          Array with the "BCC:" recipients of the message
   * @param subject
   *          Subject of the message
   * @param body
   *          Body of the message
   * @throws MessagingException
   *           if sending the message failed
   */
  public void send(String[] to, String[] cc, String[] bcc, String subject, String body) throws MessagingException {
    send(to, cc, bcc, subject, body, false);
  }

  /**
   * Send a message to multiple recipients
   *
   * @param to
   *          Array with the "To:" recipients of the message
   * @param cc
   *          Array with the "CC:" recipients of the message
   * @param bcc
   *          Array with the "BCC:" recipients of the message
   * @param subject
   *          Subject of the message
   * @param body
   *          Body of the message
   * @param isHTML
   *          Is the body of the message in HTML
   * @throws MessagingException
   *           if sending the message failed
   */
  public void send(String[] to, String[] cc, String[] bcc, String subject, String body, boolean isHTML) throws MessagingException {
    MimeMessage message = createMessage();
    addRecipients(message, RecipientType.TO, to);
    addRecipients(message, RecipientType.CC, cc);
    addRecipients(message, RecipientType.BCC, bcc);
    message.setSubject(subject);
    if (isHTML) {
        message.setContent(body, TEXT_HTML);
    } else {
        message.setText(body);
    }
    message.saveChanges();
    send(message);
  }

  /**
   * Process an array of recipients with the given {@link javax.mail.Message.RecipientType}
   *
   * @param message
   *          The message to add the recipients to
   * @param type
   *          The type of recipient
   * @param addresses
   * @throws MessagingException
   */
  private static void addRecipients(MimeMessage message, RecipientType type, String... strAddresses)
          throws MessagingException {
    if (strAddresses != null) {
      InternetAddress[] addresses = new InternetAddress[strAddresses.length];
      for (int i = 0; i < strAddresses.length; i++)
        addresses[i] = new InternetAddress(strAddresses[i]);
      message.addRecipients(type, addresses);
    }
  }

}
