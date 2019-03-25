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

package org.opencastproject.workflow.handler.notification;

import org.opencastproject.email.template.api.EmailTemplateService;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.kernel.mail.SmtpService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * Please describe what this handler does.
 */
public class EmailWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(EmailWorkflowOperationHandler.class);

  /** The smtp service */
  private SmtpService smtpService = null;

  /** The email template service */
  private EmailTemplateService emailTemplateService = null;

  /** The user directory service */
  private UserDirectoryService userDirectoryService;

  // Configuration properties used in the workflow definition
  static final String TO_PROPERTY = "to";
  static final String CC_PROPERTY = "cc";
  static final String BCC_PROPERTY = "bcc";
  static final String SUBJECT_PROPERTY = "subject";
  static final String BODY_PROPERTY = "body";
  static final String BODY_TEMPLATE_FILE_PROPERTY = "body-template-file";
  static final String IS_HTML = "use-html";

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#activate
   * (org.osgi.service.component.ComponentContext )
   */
  @Override
  protected void activate(ComponentContext cc) {
    super.activate(cc);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    // The current workflow operation instance
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();

    // MediaPackage from previous workflow operations
    MediaPackage srcPackage = workflowInstance.getMediaPackage();

    // "To", "CC", "BCC", subject, body can be Freemarker templates
    String to = processDestination(workflowInstance, operation, TO_PROPERTY);
    String cc = processDestination(workflowInstance, operation, CC_PROPERTY);
    String bcc = processDestination(workflowInstance, operation, BCC_PROPERTY);
    String subject = applyTemplateIfNecessary(workflowInstance, operation, SUBJECT_PROPERTY);
    String bodyText = null;
    String body = operation.getConfiguration(BODY_PROPERTY);
    Boolean isHTML = BooleanUtils.toBoolean(operation.getConfiguration(IS_HTML));
    // If specified, templateFile is a file that contains the Freemarker template
    String bodyTemplateFile = operation.getConfiguration(BODY_TEMPLATE_FILE_PROPERTY);
    // Body informed? If not, use the default.
    if (body == null && bodyTemplateFile == null) {
      // Set the body of the message to be the ID of the media package
      bodyText = srcPackage.getTitle() + "(" + srcPackage.getIdentifier().toString() + ")";
    } else if (body != null) {
      bodyText = applyTemplateIfNecessary(workflowInstance, operation, BODY_PROPERTY);
    } else {
      bodyText = applyTemplateIfNecessary(workflowInstance, operation, BODY_TEMPLATE_FILE_PROPERTY);
    }

    try {
      logger.debug(
              "Sending e-mail notification with subject {} and body {} to {}, CC addresses {} and BCC addresses {}",
              subject, bodyText, to, cc, bcc);
      // "To", "CC" and "BCC" can be comma- or space-separated lists of emails
      smtpService.send(to, cc, bcc, subject, bodyText, isHTML);
      logger.info("E-mail notification sent to {}, CC addresses {} and BCC addresses {}", to, cc, bcc);
    } catch (MessagingException e) {
      throw new WorkflowOperationException(e);
    }

    // Return the source mediapackage and tell processing to continue
    return createResult(srcPackage, Action.CONTINUE);
  }

  private String processDestination(WorkflowInstance workflowInstance, WorkflowOperationInstance operation,
          String configName) throws WorkflowOperationException {
    // First apply the template if there's one
    String templateApplied = applyTemplateIfNecessary(workflowInstance, operation, configName);
    if (templateApplied == null)
      return null;

    // Are these valid email addresses?
    StringBuffer result = new StringBuffer();
    for (String part : templateApplied.split(",|\\s")) {
      result.append(result.length() > 0 ? "," : "");
      // Is this a user name? Look for that user via user directory service.
      User user = userDirectoryService.loadUser(part);
      if (user != null && StringUtils.isNotEmpty(user.getEmail())) {
        // Yes, this is a user name and the user has an email registered. Use it.
        result.append(user.getEmail());
      } else {
        // Either not a user name or user doesn't have an email registered.
        try {
          // Validate it as an email address
          InternetAddress emailAddr = new InternetAddress(part);
          emailAddr.validate();
          result.append(part);
        } catch (AddressException e) {
          // Otherwise, log an error
          throw new WorkflowOperationException(
                  String.format("Email address invalid or user doesn't have an email: %s.", part), e);
        }
      }
    }
    return result.toString();
  }

  private String applyTemplateIfNecessary(WorkflowInstance workflowInstance, WorkflowOperationInstance operation,
          String configName) {
    String configValue = operation.getConfiguration(configName);

    // Templates are cached, use as template name: the template name or, if
    // in-line, the
    // workflow name + the operation number + body/to/subject
    String templateName = null;
    String templateContent = null;

    if (BODY_TEMPLATE_FILE_PROPERTY.equals(configName)) {
      templateName = configValue; // Use body template file name
    } else if (configValue != null && configValue.indexOf("${") > -1) {
      // If value contains a "${", it may be a template so apply it
      // Give a name to the inline template
      templateName = workflowInstance.getTemplate() + "_" + operation.getPosition() + "_" + configName;
      // Only alphanumeric and _
      templateName = templateName.replaceAll("[^A-Za-z0-9 ]", "_");
      templateContent = configValue;
    } else {
      // If value doesn't contain a "${", assume it is NOT a Freemarker
      // template and thus return the value as it is
      return configValue;
    }
    // Apply the template
    return emailTemplateService.applyTemplate(templateName, templateContent, workflowInstance);
  }

  /**
   * Callback for OSGi to set the {@link SmtpService}.
   *
   * @param smtpService
   *          the smtp service
   */
  void setSmtpService(SmtpService smtpService) {
    this.smtpService = smtpService;
  }

  /**
   * Callback for OSGi to set the {@link SmtpService}.
   *
   * @param service
   *          the email template service
   */
  void setEmailTemplateService(EmailTemplateService service) {
    this.emailTemplateService = service;
  }

  /**
   * Callback for OSGi to set the {@link UserDirectoryService}.
   *
   * @param service
   *          the user directory service
   */
  void setUserDirectoryService(UserDirectoryService service) {
    this.userDirectoryService = service;
  }
}
