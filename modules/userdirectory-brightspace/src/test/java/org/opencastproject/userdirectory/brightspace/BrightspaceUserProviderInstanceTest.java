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

package org.opencastproject.userdirectory.brightspace;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.User;
import org.opencastproject.userdirectory.brightspace.client.BrightspaceClient;
import org.opencastproject.userdirectory.brightspace.client.BrightspaceClientException;
import org.opencastproject.userdirectory.brightspace.client.api.Activation;
import org.opencastproject.userdirectory.brightspace.client.api.BrightspaceUser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;

public class BrightspaceUserProviderInstanceTest {

  private static final String PID = "userProvider";
  private static final int CACHE_SIZE = 60;
  private static final int CACHE_EXPIRATION = 1000;

  private BrightspaceUserProviderInstance brightspaceUserProviderInstance;
  private BrightspaceClient client;
  private Organization organization;

  @Before
  public void setup() {
    client = Mockito.mock(BrightspaceClient.class);
    organization = Mockito.mock(Organization.class);
    brightspaceUserProviderInstance = new BrightspaceUserProviderInstance(PID, client, organization, CACHE_SIZE,
            CACHE_EXPIRATION);
  }

  @Test
  public void shouldCallBrightspaceClient() throws BrightspaceClientException {
    when(client.findUser(anyString())).thenReturn(
            new BrightspaceUser("org-id", "user-id", "user-firstname", null, "user-middlename", "username",
                    "email@orgainisation.org", "org-defined-id", "unique-id", new Activation(true), "dsplay-name"));
    when(client.getURL()).thenReturn("localhost:8080/");
    when(client.findCourseIds(anyString())).thenReturn(new HashSet<>(Arrays.asList("course123", "course456")));

    User user = brightspaceUserProviderInstance.loadUser("org-defined-id");

    verify(client).findUser("org-defined-id");
    verify(client).findCourseIds("user-id");
    verify(client).getURL();
    verifyNoMoreInteractions(client);

    Assert.assertThat(user.getUsername(), is("username"));
  }

  public void shouldReturnProviderName() {
    Assert.assertThat(brightspaceUserProviderInstance.getName(), is("brightspace"));
  }



}
