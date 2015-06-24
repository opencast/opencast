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

package org.opencastproject.messages.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.comments.Comment;
import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.messages.EmailConfiguration;
import org.opencastproject.messages.MailService;
import org.opencastproject.messages.MessageSignature;
import org.opencastproject.messages.MessageTemplate;
import org.opencastproject.messages.TemplateMessageQuery;
import org.opencastproject.messages.TemplateType;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

public class MailServiceTest {
  private MailService mailService = null;

  @Before
  public void setUp() throws Exception {
    // Set up the mail service
    User user = new JaxbUser("test", null, "Test User", "test@test.com", "test", new DefaultOrganization(),
            new HashSet<JaxbRole>());

    UserDirectoryService userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyObject(String.class))).andReturn(user).anyTimes();
    EasyMock.replay(userDirectoryService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);

    mailService = new MailService();
    mailService.setUserDirectoryService(userDirectoryService);
    mailService.setSecurityService(securityService);
    mailService.setEntityManagerFactory(newTestEntityManagerFactory(MailService.PERSISTENCE_UNIT));
  }

  @After
  public void tearDown() throws Exception {
    mailService.deactivate(null);
  }

  @Test
  public void testCRUDMessageSignature() {
    User admin = new JaxbUser("george@test.com", null, "George", "george@test.com", "test", new DefaultOrganization(),
            new HashSet<JaxbRole>());
    User help = new JaxbUser("frank@test.com", null, "Frank", "frank@test.com", "test", new DefaultOrganization(),
            new HashSet<JaxbRole>());
    MessageSignature signatureAdmin = MessageSignature.messageSignature("Administrator", admin,
            EmailAddress.emailAddress("admin@test.com", "Dr. Admin"), "Sincerly");
    MessageSignature signatureHelp = MessageSignature.messageSignature("Helpdesk", help,
            EmailAddress.emailAddress("help@test.com", "Mr. Help"), "Sincerly");
    List<MessageSignature> signatures;

    // Create
    try {
      signatureAdmin = mailService.updateMessageSignature(signatureAdmin);
      signatureHelp = mailService.updateMessageSignature(signatureHelp);
    } catch (MailServiceException e) {
      fail("Not able to save a message signature entity: " + e.getMessage());
    }

    // Read
    try {
      signatures = mailService.getMessageSignatures();
      assertEquals(2, signatures.size());
      assertTrue(signatures.contains(signatureAdmin) && signatures.contains(signatureHelp));
    } catch (MailServiceException e) {
      fail("Not able to get the message signatures: " + e.getMessage());
    }

    // Update
    try {
      Comment comment = Comment.create(Option.<Long> none(), "A simple text.", admin);
      signatureHelp.setCreator(admin);
      signatureHelp.addComment(comment);
      signatureHelp = mailService.updateMessageSignature(signatureHelp);
      signatures = mailService.getMessageSignatures();
      assertEquals(2, signatures.size());
      assertTrue(signatures.contains(signatureHelp));
    } catch (MailServiceException e) {
      fail("Not able to update a message signature entity: " + e.getMessage());
    }

    // Delete
    try {
      mailService.deleteMessageSignature(signatureAdmin.getId());
      signatures = mailService.getMessageSignatures();
      assertEquals(1, signatures.size());
      assertTrue(signatures.contains(signatureHelp));
    } catch (MailServiceException e) {
      fail("Not able to delete the message signature " + signatureAdmin.getName() + ": " + e.getMessage());
    } catch (NotFoundException e) {
      fail("Not able to get the message signatures: " + e.getMessage());
    }
  }

  @Test
  public void testCRUDMessageTemplate() {
    User admin = new JaxbUser("george@test.com", null, "George", "george@test.com", "test", new DefaultOrganization(),
            new HashSet<JaxbRole>());
    User student = new JaxbUser("frank@test.com", null, "Frank", "frank@test.com", "test", new DefaultOrganization(),
            new HashSet<JaxbRole>());
    String name = "Invitation";
    MessageTemplate msgTmpl1 = new MessageTemplate(name, student, "Course invitation",
            "Please watch this course recording.");
    MessageTemplate msgTmpl2 = new MessageTemplate("Acknowledge 1", admin, "Recording ready!",
            "The recording of the course XYZ is finished. Please review it.");
    msgTmpl2.setType(TemplateType.Type.ACKNOWLEDGE.getType());
    msgTmpl2.setHidden(true);
    MessageTemplate msgTmpl3 = new MessageTemplate("Acknowledge 2", admin, "Recording ready!",
            "The recording of the course ZYX is finished. Please review it.");
    msgTmpl3.setType(TemplateType.Type.ACKNOWLEDGE.getType());

    // Create
    try {
      msgTmpl1 = mailService.updateMessageTemplate(msgTmpl1);
      msgTmpl2 = mailService.updateMessageTemplate(msgTmpl2);
      msgTmpl3 = mailService.updateMessageTemplate(msgTmpl3);
    } catch (MailServiceException e) {
      fail("Not able to save a message template entity: " + e.getMessage());
    }

    // Read
    TemplateMessageQuery query = new TemplateMessageQuery();

    // Search without hidden
    List<MessageTemplate> savedMsgTemplates = mailService.findMessageTemplates(query);
    assertEquals(2, savedMsgTemplates.size());

    // Search with hidden
    query.withIncludeHidden();
    savedMsgTemplates = mailService.findMessageTemplates(query);
    assertEquals(3, savedMsgTemplates.size());

    // Search with only the creator admin
    query.withCreator(admin.getUsername());
    savedMsgTemplates = mailService.findMessageTemplates(query);
    assertEquals(2, savedMsgTemplates.size());
    assertTrue(savedMsgTemplates.contains(msgTmpl2) && savedMsgTemplates.contains(msgTmpl3));

    // Search with only creator student (no template)
    query.withCreator(student.getUsername());
    savedMsgTemplates = mailService.findMessageTemplates(query);
    assertEquals(1, savedMsgTemplates.size());
    assertEquals(msgTmpl1, savedMsgTemplates.get(0));

    // Search with only the name
    query.withCreator(null);
    query.withName(name);
    savedMsgTemplates = mailService.findMessageTemplates(query);
    assertEquals(1, savedMsgTemplates.size());
    assertTrue(msgTmpl1.equals(savedMsgTemplates.get(0)));

    // Search with only the type
    query.withName(null);
    query.withType(msgTmpl1.getType().getType());
    savedMsgTemplates = mailService.findMessageTemplates(query);
    assertEquals(1, savedMsgTemplates.size());
    assertTrue(msgTmpl1.equals(savedMsgTemplates.get(0)));

    // Search with only fullText
    query.withType(null);
    query.withFullText("ready");
    savedMsgTemplates = mailService.findMessageTemplates(query);
    assertEquals(2, savedMsgTemplates.size());
    assertTrue(savedMsgTemplates.contains(msgTmpl2) && savedMsgTemplates.contains(msgTmpl3));

    // Search with different options
    query.withCreator(admin.getUsername());
    query.withType(TemplateType.Type.ACKNOWLEDGE);
    query.withFullText("ready");
    savedMsgTemplates = mailService.findMessageTemplates(query);
    assertEquals(2, savedMsgTemplates.size());
    assertTrue(savedMsgTemplates.contains(msgTmpl2) && savedMsgTemplates.contains(msgTmpl3));

    // Update
    try {
      Comment comment = Comment.create(Option.<Long> none(), "A simple text.", admin);
      msgTmpl2.addComment(comment);
      msgTmpl2.setSubject("Informations");
      msgTmpl2 = mailService.updateMessageTemplate(msgTmpl2);
      query.withCreator(null);
      query.withType(null);
      query.withFullText(null);
      savedMsgTemplates = mailService.findMessageTemplates(query);
      assertEquals(3, savedMsgTemplates.size());
      assertTrue(savedMsgTemplates.contains(msgTmpl2));
    } catch (MailServiceException e) {
      fail("Not able to save a message template entity: " + e.getMessage());
    }

    // Delete
    try {
      mailService.deleteMessageTemplate(msgTmpl2.getId());
      savedMsgTemplates = mailService.findMessageTemplates(query);
      assertEquals(2, savedMsgTemplates.size());
      assertTrue(savedMsgTemplates.contains(msgTmpl1) && savedMsgTemplates.contains(msgTmpl3));
    } catch (Exception e) {
      fail("Not able to get the message template entity " + msgTmpl2.getName() + " : " + e.getMessage());
    }
  }

  @Test
  public void testCRUEmailConfiguration() {
    EmailConfiguration emailConfig1 = new EmailConfiguration("smtp", "my_mail_server", 25, "admin", "password", false);
    EmailConfiguration emailConfig2 = new EmailConfiguration("smtp", "your_mail_server", 25, "admin", "password", false);

    try {
      assertEquals(EmailConfiguration.DEFAULT, mailService.getEmailConfiguration());
    } catch (MailServiceException e) {
      fail("Unable to get the email configuration: " + e.getMessage());
    }

    try {
      emailConfig1 = mailService.updateEmailConfiguration(emailConfig1);
      emailConfig2 = mailService.updateEmailConfiguration(emailConfig2);
    } catch (MailServiceException e) {
      fail("Unable to save the email configuration: " + e.getMessage());
    }

    try {
      assertEquals(emailConfig2, mailService.getEmailConfiguration());
    } catch (MailServiceException e) {
      fail("Unable to get the email configuration: " + e.getMessage());
    }
  }

}
