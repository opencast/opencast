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

package org.opencastproject.adminui.endpoint;

import org.opencastproject.adminui.usersettings.UserSetting;
import org.opencastproject.adminui.usersettings.UserSettings;
import org.opencastproject.adminui.usersettings.UserSettingsService;
import org.opencastproject.adminui.usersettings.persistence.UserSettingsServiceException;
import org.opencastproject.comments.Comment;
import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.messages.MailService;
import org.opencastproject.messages.MessageSignature;
import org.opencastproject.messages.persistence.MailServiceException;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Ignore;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestUserSettingsEndpoint extends UserSettingsEndpoint {
  public static final String EXAMPLE_EMAIL = "example@fake.com";
  public static final String EXAMPLE_KEY = "example_key";
  public static final String EXAMPLE_NAME = "FakeName";
  public static final String EXAMPLE_VALUE = "example_value";
  public static final String EXAMPLE_USERNAME = "fakeuser";

  private UserSettingsService userSettingsService;
  private MailService mailService;
  private SecurityService securityService;

  private User createUser() {
    User user = EasyMock.createMock(User.class);
    EasyMock.expect(user.getUsername()).andReturn(EXAMPLE_USERNAME).anyTimes();
    EasyMock.expect(user.getName()).andReturn(EXAMPLE_NAME).anyTimes();
    EasyMock.expect(user.getEmail()).andReturn(EXAMPLE_EMAIL).anyTimes();
    EasyMock.replay(user);
    return user;
  }

  private UserSettings createUserSettings(int start, int finish, int limit, int offset, int total) {
    UserSettings userSettings = new UserSettings();
    for (int i = 1; i <= 10; i++) {
      UserSetting userSetting = new UserSetting(i, "key-" + i, "value-" + i);
      userSettings.addUserSetting(userSetting);
    }
    userSettings.setTotal(total);
    userSettings.setLimit(limit);
    userSettings.setOffset(offset);
    return userSettings;
  }

  private void setupUserSettingsService() throws UserSettingsServiceException {
    int start = 1;
    int finish = 10;
    int limit = 100;
    int offset = 0;
    int total = 10;
    UserSettings userSettings = createUserSettings(start, finish, limit, offset, total);
    userSettingsService = EasyMock.createNiceMock(UserSettingsService.class);
    EasyMock.expect(userSettingsService.findUserSettings(limit, 0)).andReturn(userSettings);
    final Capture<String> inputKey = new Capture<String>();
    final Capture<String> inputValue = new Capture<String>();
    EasyMock.expect(userSettingsService.addUserSetting(EasyMock.capture(inputKey), EasyMock.capture(inputValue)))
            .andAnswer(new IAnswer<UserSetting>() {
              public UserSetting answer() {
                UserSetting userSetting = new UserSetting(19, inputKey.getValue(), inputValue.getValue());
                return userSetting;
              }
            });
    userSettingsService.deleteUserSetting(18L);
    EasyMock.expectLastCall();
    EasyMock.expect(userSettingsService.updateUserSetting(18, EXAMPLE_KEY, EXAMPLE_VALUE)).andReturn(
            new UserSetting(18L, EXAMPLE_KEY, EXAMPLE_VALUE));
    EasyMock.replay(userSettingsService);
  }

  private void setupMailService() throws MailServiceException, NotFoundException {
    List<MessageSignature> signatures = new LinkedList<MessageSignature>();
    User creator = createUser();
    List<Comment> comments = new LinkedList<Comment>();
    EmailAddress senderEmail = new EmailAddress(EXAMPLE_EMAIL, EXAMPLE_NAME);
    MessageSignature messageSignature;
    for (long i = 0; i < 10; i++) {
      messageSignature = new MessageSignature(i, "signature-" + i, creator, senderEmail, Option.<EmailAddress> none(),
              "The signature", new Date(), comments);
      signatures.add(messageSignature);
    }
    // mailService = EasyMock.createNiceMock(MailService.class);
    mailService = EasyMock.createMock(MailService.class);
    EasyMock.expect(mailService.getMessageSignaturesByUserName()).andReturn(signatures);
    EasyMock.expect(mailService.getSignatureTotalByUserName()).andReturn(10);
    final Capture<MessageSignature> inputSignature = new Capture<MessageSignature>();
    EasyMock.expect(mailService.updateMessageSignature(EasyMock.capture(inputSignature))).andAnswer(new IAnswer<MessageSignature>() {
      public MessageSignature answer() {
        inputSignature.getValue().setId(20L);
        return inputSignature.getValue();
      }
    });
    mailService.deleteMessageSignature(19L);
    EasyMock.expect(mailService.getMessageSignature(19L)).andReturn(signatures.get(0));
    EasyMock.expectLastCall();
    EasyMock.expect(mailService.getCurrentUsersSignature()).andReturn(signatures.get(0));
    EasyMock.replay(mailService);
  }

  public void setupSecurityService() {
    User user = createUser();
    securityService = EasyMock.createMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(user);
    EasyMock.replay(securityService);
  }

  public TestUserSettingsEndpoint() throws Exception {
    setupUserSettingsService();
    setupMailService();
    setupSecurityService();
    this.setMailService(mailService);
    this.setUserSettingsService(userSettingsService);
    this.setSecurityService(securityService);
  }
}
