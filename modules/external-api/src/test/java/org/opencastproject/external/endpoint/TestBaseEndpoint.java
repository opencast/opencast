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
package org.opencastproject.external.endpoint;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.opencastproject.systems.OpencastConstants.ADMIN_DOC_URL_ORG_PROPERTY;
import static org.opencastproject.systems.OpencastConstants.EXTERNAL_API_URL_ORG_PROPERTY;
import static org.opencastproject.systems.OpencastConstants.FEED_URL_ORG_PROPERTY;

import org.opencastproject.external.impl.index.ExternalIndex;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

import org.junit.Ignore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestBaseEndpoint extends BaseEndpoint {

  public TestBaseEndpoint() {

    // Prepare mocked organization properties
    Map<String, String> orgProperties = new HashMap<String, String>();
    orgProperties.put(FEED_URL_ORG_PROPERTY, "https://feeds.opencast.org");
    orgProperties.put(ADMIN_DOC_URL_ORG_PROPERTY, "https://documentation.opencast.org");
    orgProperties.put(EXTERNAL_API_URL_ORG_PROPERTY, "https://api.opencast.org");
    this.endpointBaseUrl = "https://api.opencast.org";

    // Prepare mocked organization
    Organization org = createNiceMock(Organization.class);
    expect(org.getAdminRole()).andStubReturn("ROLE_ADMIN");
    expect(org.getAnonymousRole()).andStubReturn("ROLE_ANONYMOUS");
    expect(org.getId()).andStubReturn("opencast");
    expect(org.getName()).andStubReturn("Opencast");
    expect(org.getProperties()).andStubReturn(orgProperties);

    Set<Role> roles = new HashSet<Role>();
    Role roleStudent = createNiceMock(Role.class);
    expect(roleStudent.getName()).andStubReturn("ROLE_STUDENT");
    roles.add(roleStudent);
    Role roleUser = createNiceMock(Role.class);
    expect(roleUser.getName()).andStubReturn("ROLE_USER_92623987_OPENCAST_ORG");
    roles.add(roleUser);

    // Prepare mocked user
    User user = createNiceMock(User.class);
    expect(user.getOrganization()).andStubReturn(org);
    expect(user.getEmail()).andStubReturn("nowhere@opencast.org");
    expect(user.getName()).andStubReturn("Opencast Student");
    expect(user.getProvider()).andStubReturn("opencast");
    expect(user.getUsername()).andStubReturn("92623987@opencast.org");
    expect(user.getRoles()).andStubReturn(roles);

    // Prepare mocked security service
    SecurityService securityService = createNiceMock(SecurityService.class);
    expect(securityService.getOrganization()).andStubReturn(org);
    expect(securityService.getUser()).andStubReturn(user);

    // Replay mocked objects
    replay(org, roleStudent, roleUser, user, securityService);

    setSecurityService(securityService);
    setExternalIndex(new ExternalIndex());
  }

}
