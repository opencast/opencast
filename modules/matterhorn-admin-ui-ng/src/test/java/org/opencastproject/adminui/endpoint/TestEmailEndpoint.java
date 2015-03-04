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
package org.opencastproject.adminui.endpoint;

import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.messages.MailService;
import org.opencastproject.messages.MessageTemplate;
import org.opencastproject.messages.persistence.MailServiceException;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.util.HashSet;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestEmailEndpoint extends EmailEndpoint {
  private MailService mailService;
  private User user;

  public TestEmailEndpoint() throws Exception {
    setupServices();
    addData();
    this.activate(null);
  }

  private void addData() throws MailServiceException {
    MessageTemplate messageTemplate1 = new MessageTemplate("Template-1", user, "Template subject", "Template body");
    mailService.updateMessageTemplate(messageTemplate1);
    MessageTemplate messageTemplate2 = new MessageTemplate("Template-2", user, "Template subject", "Template body");
    mailService.updateMessageTemplate(messageTemplate2);
    MessageTemplate messageTemplate3 = new MessageTemplate("Template-3", user, "Template subject", "Template body");
    mailService.updateMessageTemplate(messageTemplate3);
  }

  private void setupServices() {
    user = new JaxbUser("test", null, "Test User", "test@test.com", "test", new DefaultOrganization(),
            new HashSet<JaxbRole>());

    UserDirectoryService userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(user).anyTimes();
    EasyMock.replay(userDirectoryService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);

    ParticipationManagementDatabase persistence = EasyMock.createNiceMock(ParticipationManagementDatabase.class);
    EasyMock.replay(persistence);

    mailService = new MailService();
    mailService.setEntityManagerFactory(newTestEntityManagerFactory(MailService.PERSISTENCE_UNIT));
    mailService.setUserDirectoryService(userDirectoryService);
    mailService.setSecurityService(securityService);

    this.setMailService(mailService);
    this.setParticipationDatabase(persistence);
  }
}
