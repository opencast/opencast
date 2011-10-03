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
package org.opencastproject.capture.impl.jobs;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.api.AgentRecording;
import org.opencastproject.capture.api.StateService;
import org.opencastproject.capture.impl.ConfigurationManager;
import org.opencastproject.capture.impl.RecordingImpl;
import org.opencastproject.capture.impl.XProperties;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.security.api.TrustedHttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

@Ignore()
public class AgentStateJobTest {
  private AgentStateJob job = null;

  @Before
  public void setUp() throws Exception {
    ConfigurationManager config = new ConfigurationManager();
    config.setItem(CaptureParameters.AGENT_STATE_REMOTE_ENDPOINT_URL, "http://localhost:8080/");
    config.setItem(CaptureParameters.RECORDING_STATE_REMOTE_ENDPOINT_URL, "http://localhost:8080/");
    config.setItem(CaptureParameters.AGENT_NAME, "testAgent");

    StateService state = EasyMock.createMock(StateService.class);

    EasyMock.expect(state.getAgentName()).andReturn("testAgent");
    EasyMock.expectLastCall().anyTimes();

    EasyMock.expect(state.getAgentState()).andReturn("idle");
    EasyMock.expectLastCall().anyTimes();

    Map<String, AgentRecording> recordingMap = new HashMap<String, AgentRecording>();
    recordingMap.put("test", new RecordingImpl(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .createNew(), new XProperties()));
    EasyMock.expect(state.getKnownRecordings()).andReturn(recordingMap);
    EasyMock.expectLastCall().anyTimes();

    StatusLine statusline = EasyMock.createMock(StatusLine.class);
    EasyMock.expect(statusline.getStatusCode()).andReturn(200);
    EasyMock.expectLastCall().anyTimes();

    HttpResponse response = EasyMock.createMock(HttpResponse.class);
    EasyMock.expect(response.getStatusLine()).andReturn(statusline);
    EasyMock.expectLastCall().anyTimes();

    TrustedHttpClient client = EasyMock.createMock(TrustedHttpClient.class);
    EasyMock.expect(client.execute(EasyMock.isA(HttpPost.class))).andReturn(response);
    EasyMock.expectLastCall().anyTimes();

    EasyMock.replay(state);
    EasyMock.replay(client);

    job = new AgentStateJob();
    job.setConfigManager(config);
    job.setStateService(state);
    job.setTrustedClient(client);
  }

  @Test
  public void test() {
    job.sendAgentState();
    job.sendRecordingState();
  }
}
