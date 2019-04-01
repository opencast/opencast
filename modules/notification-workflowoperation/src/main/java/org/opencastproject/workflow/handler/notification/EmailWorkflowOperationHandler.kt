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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.workflow.handler.notification

import org.opencastproject.email.template.api.EmailTemplateService
import org.opencastproject.job.api.JobContext
import org.opencastproject.kernel.mail.SmtpService
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.mail.MessagingException
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress

/**
 * Please describe what this handler does.
 */
class EmailWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The smtp service  */
    private var smtpService: SmtpService? = null

    /** The email template service  */
    private var emailTemplateService: EmailTemplateService? = null

    /** The user directory service  */
    private var userDirectoryService: UserDirectoryService? = null

    /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#activate
   * (org.osgi.service.component.ComponentContext )
   */
    override fun activate(cc: ComponentContext) {
        super.activate(cc)
    }

    /**
     * {@inheritDoc}
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {

        // The current workflow operation instance
        val operation = workflowInstance.currentOperation

        // MediaPackage from previous workflow operations
        val srcPackage = workflowInstance.mediaPackage

        // "To", "CC", "BCC", subject, body can be Freemarker templates
        val to = processDestination(workflowInstance, operation, TO_PROPERTY)
        val cc = processDestination(workflowInstance, operation, CC_PROPERTY)
        val bcc = processDestination(workflowInstance, operation, BCC_PROPERTY)
        val subject = applyTemplateIfNecessary(workflowInstance, operation, SUBJECT_PROPERTY)
        var bodyText: String? = null
        val body = operation.getConfiguration(BODY_PROPERTY)
        val isHTML = BooleanUtils.toBoolean(operation.getConfiguration(IS_HTML))
        // If specified, templateFile is a file that contains the Freemarker template
        val bodyTemplateFile = operation.getConfiguration(BODY_TEMPLATE_FILE_PROPERTY)
        // Body informed? If not, use the default.
        if (body == null && bodyTemplateFile == null) {
            // Set the body of the message to be the ID of the media package
            bodyText = srcPackage.title + "(" + srcPackage.identifier.toString() + ")"
        } else if (body != null) {
            bodyText = applyTemplateIfNecessary(workflowInstance, operation, BODY_PROPERTY)
        } else {
            bodyText = applyTemplateIfNecessary(workflowInstance, operation, BODY_TEMPLATE_FILE_PROPERTY)
        }

        try {
            logger.debug(
                    "Sending e-mail notification with subject {} and body {} to {}, CC addresses {} and BCC addresses {}",
                    subject, bodyText, to, cc, bcc)
            // "To", "CC" and "BCC" can be comma- or space-separated lists of emails
            smtpService!!.send(to, cc, bcc, subject, bodyText, isHTML)
            logger.info("E-mail notification sent to {}, CC addresses {} and BCC addresses {}", to, cc, bcc)
        } catch (e: MessagingException) {
            throw WorkflowOperationException(e)
        }

        // Return the source mediapackage and tell processing to continue
        return createResult(srcPackage, Action.CONTINUE)
    }

    @Throws(WorkflowOperationException::class)
    private fun processDestination(workflowInstance: WorkflowInstance, operation: WorkflowOperationInstance,
                                   configName: String): String? {
        // First apply the template if there's one
        val templateApplied = applyTemplateIfNecessary(workflowInstance, operation, configName) ?: return null

        // Are these valid email addresses?
        val result = StringBuffer()
        for (part in templateApplied.split(",|\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            result.append(if (result.length > 0) "," else "")
            // Is this a user name? Look for that user via user directory service.
            val user = userDirectoryService!!.loadUser(part)
            if (user != null && StringUtils.isNotEmpty(user.email)) {
                // Yes, this is a user name and the user has an email registered. Use it.
                result.append(user.email)
            } else {
                // Either not a user name or user doesn't have an email registered.
                try {
                    // Validate it as an email address
                    val emailAddr = InternetAddress(part)
                    emailAddr.validate()
                    result.append(part)
                } catch (e: AddressException) {
                    // Otherwise, log an error
                    throw WorkflowOperationException(
                            String.format("Email address invalid or user doesn't have an email: %s.", part), e)
                }

            }
        }
        return result.toString()
    }

    private fun applyTemplateIfNecessary(workflowInstance: WorkflowInstance, operation: WorkflowOperationInstance,
                                         configName: String): String? {
        val configValue = operation.getConfiguration(configName)

        // Templates are cached, use as template name: the template name or, if
        // in-line, the
        // workflow name + the operation number + body/to/subject
        var templateName: String? = null
        var templateContent: String? = null

        if (BODY_TEMPLATE_FILE_PROPERTY == configName) {
            templateName = configValue // Use body template file name
        } else if (configValue != null && configValue.indexOf("\${") > -1) {
            // If value contains a "${", it may be a template so apply it
            // Give a name to the inline template
            templateName = workflowInstance.template + "_" + operation.position + "_" + configName
            // Only alphanumeric and _
            templateName = templateName.replace("[^A-Za-z0-9 ]".toRegex(), "_")
            templateContent = configValue
        } else {
            // If value doesn't contain a "${", assume it is NOT a Freemarker
            // template and thus return the value as it is
            return configValue
        }
        // Apply the template
        return emailTemplateService!!.applyTemplate(templateName!!, templateContent!!, workflowInstance)
    }

    /**
     * Callback for OSGi to set the [SmtpService].
     *
     * @param smtpService
     * the smtp service
     */
    internal fun setSmtpService(smtpService: SmtpService) {
        this.smtpService = smtpService
    }

    /**
     * Callback for OSGi to set the [SmtpService].
     *
     * @param service
     * the email template service
     */
    internal fun setEmailTemplateService(service: EmailTemplateService) {
        this.emailTemplateService = service
    }

    /**
     * Callback for OSGi to set the [UserDirectoryService].
     *
     * @param service
     * the user directory service
     */
    internal fun setUserDirectoryService(service: UserDirectoryService) {
        this.userDirectoryService = service
    }

    companion object {

        private val logger = LoggerFactory.getLogger(EmailWorkflowOperationHandler::class.java)

        // Configuration properties used in the workflow definition
        internal val TO_PROPERTY = "to"
        internal val CC_PROPERTY = "cc"
        internal val BCC_PROPERTY = "bcc"
        internal val SUBJECT_PROPERTY = "subject"
        internal val BODY_PROPERTY = "body"
        internal val BODY_TEMPLATE_FILE_PROPERTY = "body-template-file"
        internal val IS_HTML = "use-html"
    }
}
