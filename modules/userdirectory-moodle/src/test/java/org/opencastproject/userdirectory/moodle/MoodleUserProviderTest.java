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

package org.opencastproject.userdirectory.moodle;

import org.opencastproject.security.api.DefaultOrganization;

import org.junit.Before;

import java.net.URI;

public class MoodleUserProviderTest {
  private MoodleUserProviderInstance moodleProvider;

  @Before
  public void setUp() throws Exception {
    moodleProvider = new MoodleUserProviderInstance("sample_pid",
            new MoodleWebServiceImpl(new URI("http://moodle/webservice/rest/server.php"), "myToken"),
            new DefaultOrganization(), "^[0-9]+$", "^[0-9a-zA-Z_]+$", "^[0-9]+$", true, 100, 10, "admin");
  }

}
