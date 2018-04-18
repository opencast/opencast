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
package org.opencastproject.scheduler.conflict.notifier.email;

import static org.opencastproject.scheduler.impl.SchedulerUtil.eventOrganizationFilter;
import static org.opencastproject.scheduler.impl.SchedulerUtil.toHumanReadableString;
import static org.opencastproject.scheduler.impl.SchedulerUtil.uiAdapterToFlavor;
import static org.opencastproject.util.OsgiUtil.getContextProperty;

import org.opencastproject.kernel.mail.SmtpService;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.scheduler.api.ConflictNotifier;
import org.opencastproject.scheduler.api.ConflictResolution.Strategy;
import org.opencastproject.scheduler.api.ConflictingEvent;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.systems.MatterhornConstants;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Stream;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Email implementation of a scheduler conflict notifier
 */
public class EmailSchedulerConflictNotifier implements ConflictNotifier, ManagedService {

  private static final Logger logger = LoggerFactory.getLogger(EmailSchedulerConflictNotifier.class);

  // Configuration properties used in the workflow definition
  private static final String TO_PROPERTY = "to";
  private static final String SUBJECT_PROPERTY = "subject";
  private static final String TEMPLATE_PROPERTY = "template";

  // Configuration defaults
  private static final String DEFAULT_SUBJECT = "Scheduling conflict";
  private static final String DEFAULT_TEMPLATE = "Dear Administrator,\n"
          + "the following scheduled recordings are conflicting with existing ones:\n\n"
          + "${recordings}";

  /** The SMTP service */
  private SmtpService smptService;

  /** The security service */
  private SecurityService securityService;

  /** The workspace */
  private Workspace workspace;

  /** The list of registered event catalog UI adapters */
  private List<EventCatalogUIAdapter> eventCatalogUIAdapters = new ArrayList<>();

  /** OSGi callback to add {@link SmtpService} instance. */
  void setSmtpService(SmtpService smtpService) {
    this.smptService = smtpService;
  }

  /** OSGi callback to add {@link SecurityService} instance. */
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback to add {@link Workspace} instance. */
  void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /** OSGi callback to add {@link EventCatalogUIAdapter} instance. */
  void addCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    eventCatalogUIAdapters.add(catalogUIAdapter);
  }

  /** OSGi callback to remove {@link EventCatalogUIAdapter} instance. */
  void removeCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    eventCatalogUIAdapters.remove(catalogUIAdapter);
  }

  private String recipient;
  private String subject;
  private String template;
  private String serverUrl;

  /** OSGi callback. */
  public void activate(ComponentContext cc) {
    serverUrl = getContextProperty(cc, MatterhornConstants.SERVER_URL_PROPERTY);
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    // Lookup the name of the recipient and subject
    recipient = StringUtils.trimToNull((String) properties.get(TO_PROPERTY));
    subject = StringUtils.defaultString((String) properties.get(SUBJECT_PROPERTY), DEFAULT_SUBJECT);
    template = StringUtils.defaultString((String) properties.get(TEMPLATE_PROPERTY), DEFAULT_TEMPLATE);
    if (StringUtils.isNotBlank(recipient)) {
      logger.info("Updated email scheduler conflict notifier with recipient '{}'", recipient);
    }
  }

  @Override
  public void notifyConflicts(List<ConflictingEvent> conflicts) {
    if (StringUtils.isBlank(recipient)) {
      // Abort if the recipient is not properly configured
      return;
    }
    String adminBaseUrl = securityService.getOrganization().getProperties()
            .get(MatterhornConstants.ADMIN_URL_ORG_PROPERTY);
    if (StringUtils.isBlank(adminBaseUrl))
      adminBaseUrl = serverUrl;

    String eventDetailsUrl = UrlSupport.concat(adminBaseUrl,
            "admin-ng/index.html#/events/events?modal=event-details&tab=general&resourceId=");

    StringBuilder sb = new StringBuilder();
    int i = 1;
    for (ConflictingEvent c : conflicts) {
      sb.append(i).append(". ");
      if (Strategy.OLD.equals(c.getConflictStrategy())) {
        sb.append(
                "This scheduled event has been overwritten with conflicting (data from the scheduling source OR manual changes). Find below the new version:")
                .append(CharUtils.LF);
        sb.append(CharUtils.LF);
        String humanReadableString = toHumanReadableString(workspace, getEventCatalogUIAdapterFlavors(),
                c.getNewEvent());
        sb.append(humanReadableString.replaceFirst(c.getNewEvent().getEventId(),
                eventDetailsUrl.concat(c.getNewEvent().getEventId())));
        sb.append(CharUtils.LF);
      } else {
        sb.append(
                "This scheduled event has been overwritten with conflicting (data from the scheduling source OR manual changes). Find below the preceding version:")
                .append(CharUtils.LF);
        sb.append(CharUtils.LF);
        String humanReadableString = toHumanReadableString(workspace, getEventCatalogUIAdapterFlavors(),
                c.getOldEvent());
        sb.append(humanReadableString.replaceFirst(c.getOldEvent().getEventId(),
                eventDetailsUrl.concat(c.getOldEvent().getEventId())));
        sb.append(CharUtils.LF);
      }
      i++;
    }
    // Create the mail message
    try {
      MimeMessage message = smptService.createMessage();
      message.addRecipient(RecipientType.TO, new InternetAddress(recipient));
      message.setSubject(subject);
      message.setText(template.replace("${recordings}", sb.toString()));
      message.saveChanges();

      smptService.send(message);
      logger.info("E-mail scheduler conflict notification sent to {}", recipient);
    } catch (MessagingException e) {
      logger.error("Unable to send email scheduler conflict notification: {}", ExceptionUtils.getStackTrace(e));
    }
  }

  private List<MediaPackageElementFlavor> getEventCatalogUIAdapterFlavors() {
    final String organization = securityService.getOrganization().getId();
    return Stream.$(eventCatalogUIAdapters).filter(eventOrganizationFilter._2(organization)).map(uiAdapterToFlavor)
            .toList();
  }

}
