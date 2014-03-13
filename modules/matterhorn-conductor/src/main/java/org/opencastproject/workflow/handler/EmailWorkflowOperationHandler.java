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
package org.opencastproject.workflow.handler;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.kernel.mail.SmtpService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Please describe what this handler does.
 */
public class EmailWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(EmailWorkflowOperationHandler.class);

  /** The smtp service */
  private SmtpService smptService = null;

  // Configuration properties used in the workflow definition
  public static final String TO_PROPERTY = "to";
  public static final String SUBJECT_PROPERTY = "subject";

  public static final String BODY_PROPERTY = "body";
  public static final String BODY_TEMPLATE_FILE_PROPERTY = "body-template-file";

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#activate(org.osgi.service.component.ComponentContext
   * )
   */
  @Override
  protected void activate(ComponentContext cc) {
    super.activate(cc);
    addConfigurationOption(TO_PROPERTY, "The mail address to send to");
    addConfigurationOption(SUBJECT_PROPERTY, "The subject line");
    addConfigurationOption(BODY_PROPERTY, "The email body text (or Freemarker template)");
    addConfigurationOption(BODY_TEMPLATE_FILE_PROPERTY, "The file name of the Freemarker template for the email body");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    // The current workflow operation instance
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();

    // MediaPackage from previous workflow operations
    MediaPackage srcPackage = workflowInstance.getMediaPackage();

    try {
      // To, subject, body can be Freemarker templates
      String to = applyTemplateIfNecessary(workflowInstance, operation, TO_PROPERTY);
      String subject = applyTemplateIfNecessary(workflowInstance, operation, SUBJECT_PROPERTY);

      String bodyText = null;
      String body = operation.getConfiguration(BODY_PROPERTY);
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

      // Create the mail message
      MimeMessage message = smptService.createMessage();

      message.addRecipient(RecipientType.TO, new InternetAddress(to));
      message.setSubject(subject);
      message.setText(bodyText);
      message.saveChanges();

      logger.debug("Sending e-mail notification to {}", to);
      smptService.send(message);
      logger.info("E-mail notification sent to {}", to);

    } catch (MessagingException e) {
      throw new WorkflowOperationException(e);
    } catch (Exception e) {
      // Freemarker exceptions (invalid template, etc)
      throw new WorkflowOperationException(e);
    }

    // Return the source mediapackage and tell processing to continue
    return createResult(srcPackage, Action.CONTINUE);
  }

  private String applyTemplateIfNecessary(WorkflowInstance workflowInstance, WorkflowOperationInstance operation,
          String configName) {
    String configValue = operation.getConfiguration(configName);

    // Templates are cached, use as template name: the template name or, if in-line, the
    // workflow name + the operation number + body/to/subject
    String templateName = null;
    String templateContent = null;

    if (BODY_TEMPLATE_FILE_PROPERTY.equals(configName)) {
      templateName = configValue; // Use body template file name
    } else if (configValue.indexOf("${") > -1) {
      // If value contains a "${", it may be a template so apply it
      templateName = workflowInstance.getTitle() + "_" + operation.getPosition() + "_" + configName;
      templateContent = configValue;
    } else {
      // If value doesn't contain a "${", assume it is NOT a Freemarker template and thus return the value as it is
      return configValue;
    }
    // Apply the template
    return smptService.applyTemplate(templateName, templateContent, workflowInstance);
  }

  /**
   * Callback for OSGi to set the {@link SmtpService}.
   * 
   * @param smtpService
   *          the smtp service
   */
  void setSmtpService(SmtpService smtpService) {
    this.smptService = smtpService;
  }

}
