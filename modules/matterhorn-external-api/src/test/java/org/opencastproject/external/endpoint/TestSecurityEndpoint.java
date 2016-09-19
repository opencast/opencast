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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.opencastproject.security.urlsigning.service.UrlSigningService;

import org.joda.time.DateTime;
import org.junit.Ignore;

import javax.ws.rs.Path;

@Path("")
@Ignore
public class TestSecurityEndpoint extends SecurityEndpoint {

  public TestSecurityEndpoint() throws Exception {
    UrlSigningService urlSigningService = createNiceMock(UrlSigningService.class);
    expect(urlSigningService.accepts("http://mycdn.com/path/movie.mp4")).andStubReturn(true);
    expect(
            urlSigningService.sign(eq("http://mycdn.com/path/movie.mp4"), anyObject(DateTime.class),
                    anyObject(DateTime.class), anyObject(String.class))).andStubReturn(
                            "http://mycdn.com/path/movie.mp4?signature");
    replay(urlSigningService);

    setUrlSigningService(urlSigningService);
  }

}
