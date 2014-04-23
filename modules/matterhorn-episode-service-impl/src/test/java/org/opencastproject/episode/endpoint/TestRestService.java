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
package org.opencastproject.episode.endpoint;

import org.junit.Ignore;
import org.opencastproject.episode.EpisodeServiceTestEnv;
import org.opencastproject.episode.api.EpisodeService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.workflow.api.WorkflowService;

import javax.ws.rs.Path;

@Path("/")
// put @Ignore here to prevent maven surefire from complaining about missing test methods
@Ignore
public class TestRestService extends AbstractEpisodeServiceRestEndpoint {
  public static final String BASE_URL = "http://localhost:8090";

  private final EpisodeServiceTestEnv env;

  public TestRestService() {
    env = new EpisodeServiceTestEnv();
  }

  @Override public EpisodeService getEpisodeService() {
    return env.getService();
  }

  @Override public WorkflowService getWorkflowService() {
    return null;
  }

  @Override public SecurityService getSecurityService() {
    return env.getSecurityService();
  }

  @Override public String getServerUrl() {
    return "http://localhost:8090";
  }

  @Override public String getMountPoint() {
    return "test";
  }
}
