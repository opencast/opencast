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
package org.opencastproject.workflow.handler.notification;

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
  private static final String TO_PROPERTY = "to";
  private static final String SUBJECT_PROPERTY = "subject";

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

    // Lookup the name of the to, from, and subject
    String to = operation.getConfiguration(TO_PROPERTY);
    String subject = operation.getConfiguration(SUBJECT_PROPERTY);
    // Set the body of the message to be the ID of the media package
    String body = srcPackage.getTitle() + "(" + srcPackage.getIdentifier().toString() + ")";

    // Create the mail message
    MimeMessage message = smptService.createMessage();

    try {
      message.addRecipient(RecipientType.TO, new InternetAddress(to));
      message.setSubject(subject);
      message.setText(body);
      message.saveChanges();

      logger.debug("Sending e-mail notification to {}", to);
      smptService.send(message);
      logger.info("E-mail notification sent to {}", to);

    } catch (MessagingException e) {
      throw new WorkflowOperationException(e);
    }

    // Return the source mediapackage and tell processing to continue
    return createResult(srcPackage, Action.CONTINUE);
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
