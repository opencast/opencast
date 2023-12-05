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

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Ignore;

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
    final Capture<String> inputKey = EasyMock.newCapture();
    final Capture<String> inputValue = EasyMock.newCapture();
    EasyMock.expect(userSettingsService.addUserSetting(EasyMock.capture(inputKey), EasyMock.capture(inputValue)))
            .andAnswer(() -> new UserSetting(19, inputKey.getValue(), inputValue.getValue()));
    userSettingsService.deleteUserSetting(18L);
    EasyMock.expectLastCall();
    EasyMock.expect(userSettingsService.updateUserSetting(18, EXAMPLE_KEY, EXAMPLE_VALUE)).andReturn(
            new UserSetting(18L, EXAMPLE_KEY, EXAMPLE_VALUE));
    EasyMock.replay(userSettingsService);
  }

  public TestUserSettingsEndpoint() throws Exception {
    setupUserSettingsService();
    this.setUserSettingsService(userSettingsService);
  }
}
