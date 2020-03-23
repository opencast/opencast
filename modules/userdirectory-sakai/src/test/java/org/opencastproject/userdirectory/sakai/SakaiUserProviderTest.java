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

package org.opencastproject.userdirectory.sakai;

import org.opencastproject.security.api.DefaultOrganization;

import org.junit.Before;

import java.util.HashSet;
import java.util.Set;

public class SakaiUserProviderTest {

  protected SakaiUserProviderInstance sakaiProvider = null;

  @Before
  public void setUp() throws Exception {

    Set<String> instructorRoles = new HashSet<String>();
    instructorRoles.add("Site owner");
    instructorRoles.add("Instructor");
    instructorRoles.add("maintain");

    sakaiProvider = new SakaiUserProviderInstance("sample_pid", new DefaultOrganization(), "https://qa11-mysql.nightly.sakaiproject.org",
      "admin", "admin", "^[a-zA-Z0-9-]+$", "^[0-9a-zA-Z]{6,}$", instructorRoles, 100, 10);
  }

}
