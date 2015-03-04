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

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
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
    String mailTransport = StringUtils.trimToNull((String) properties.get(OPT_MAIL_TRANSPORT));
    if (StringUtils.isNotBlank(mailTransport))
      setMailTransport(mailTransport);

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
    MimeMessage message = createMessage();
    message.addRecipient(RecipientType.TO, new InternetAddress(recipient));
    message.setSubject("Test from Matterhorn");
    message.setText("Hello world");
    message.saveChanges();
    send(message);
  }

}
