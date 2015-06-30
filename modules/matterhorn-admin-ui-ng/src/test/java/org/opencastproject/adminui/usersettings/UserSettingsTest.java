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

package org.opencastproject.adminui.usersettings;

import static org.junit.Assert.assertThat;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Option.none;

import org.opencastproject.adminui.endpoint.SeriesEndpointTest;
import org.opencastproject.comments.Comment;
import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.messages.MessageSignature;
import org.opencastproject.security.api.User;
import org.opencastproject.util.data.Option;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import uk.co.datumedge.hamcrest.json.SameJSONAs;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;

public class UserSettingsTest {
  @Test
  public void toJsonInputEmptyExpectedEmptySettingsAndSignatures() throws Exception {
    InputStream stream = SeriesEndpointTest.class.getResourceAsStream("/user_settings_test_empty.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONObject expected = (JSONObject) new JSONParser().parse(reader);

    UserSettings userSetting = new UserSettings();
    assertThat(expected.toJSONString(), SameJSONAs.sameJSONAs(userSetting.toJson().toJson()));
  }

  @Test
  public void toJsonInputSettingAndSignatureExpectedAllInJson() throws Exception {
    InputStream stream = SeriesEndpointTest.class.getResourceAsStream("/user_settings_test_example.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONObject expected = (JSONObject) new JSONParser().parse(reader);

    User creator = EasyMock.createMock(User.class);
    EasyMock.expect(creator.getName()).andReturn("Users Name").anyTimes();
    EasyMock.expect(creator.getUsername()).andReturn("username12").anyTimes();
    EasyMock.expect(creator.getEmail()).andReturn("adam@fake.com").anyTimes();
    EasyMock.replay(creator);
    EmailAddress sender = new EmailAddress("adam@fake.com", "Other Name");
    Option<EmailAddress> replyTo = none();
    DateTime dateTime = new DateTime(1401465634101L);
    dateTime.toDateTime(DateTimeZone.UTC);
    MessageSignature messageSignature = new MessageSignature(10L, "Adam McKenzie", creator, sender, replyTo,
            "This is the signature", dateTime.toDate(), nil(Comment.class));
    Collection<MessageSignature> signatures = new LinkedList<MessageSignature>();
    signatures.add(messageSignature);

    UserSetting userSetting = new UserSetting(98, "Test Key", "Test Value");
    UserSettings userSettings = new UserSettings();
    userSettings.setTotal(1);
    userSettings.addUserSetting(userSetting);
    assertThat(expected.toJSONString(), SameJSONAs.sameJSONAs(userSettings.toJson().toJson()));
  }
}
