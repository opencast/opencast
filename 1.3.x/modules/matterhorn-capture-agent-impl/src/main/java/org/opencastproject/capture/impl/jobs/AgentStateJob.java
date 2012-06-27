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
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is responsible for pushing the agent's state to the remote state service.
 */
public class AgentStateJob implements Job {

  private static int globalStatePushCount = 0;
  private static final Logger logger = LoggerFactory.getLogger(AgentStateJob.class);

  private ConfigurationManager config = null;
  private StateService state = null;
  private TrustedHttpClient client = null;
  private String localAddress = "";
  private int statePushCount = -1; 
  
  /** 
   * Creates a unique identifier that will allow us to track which updates are having errors to when they started.
   * @return An ever increasing int that will wrap around once it hits Interger.MAX_VALUE
   */
  public synchronized int getStatePushCount() {
    if (globalStatePushCount == 0) {
      logger.info("Starting first state push count.");
    } else if (globalStatePushCount == Integer.MAX_VALUE) {
      logger.info("Agent state push count has reached maximum, resetting global state push count to zero.");
      globalStatePushCount = 0;
    }
    return globalStatePushCount++;
  }
  
  /**
   * Pushes the agent's state to the remote state service. {@inheritDoc}
   * 
   * @see org.quartz.Job#execute(JobExecutionContext)
   * @throws JobExecutionException
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    statePushCount = getStatePushCount();
    setConfigManager((ConfigurationManager) ctx.getMergedJobDataMap().get(JobParameters.CONFIG_SERVICE));
    setStateService((StateService) ctx.getMergedJobDataMap().get(JobParameters.STATE_SERVICE));
    setTrustedClient((TrustedHttpClient) ctx.getMergedJobDataMap().get(JobParameters.TRUSTED_CLIENT));
    localAddress = ((String) ctx.getMergedJobDataMap().get("org.opencastproject.server.url"));
    sendAgentState();
    sendRecordingState();
  }

  public void setConfigManager(ConfigurationManager cfg) {
    config = cfg;
  }

  public void setStateService(StateService st) {
    state = st;
  }

  public void setTrustedClient(TrustedHttpClient cl) {
    client = cl;
  }

  /**
   * Sends an agent state update to the capture-admin state service.
   */
  protected void sendAgentState() {
    logger.debug("#" + statePushCount + " - Sending agent " + state.getAgentName() + "'s state: " + state.getAgentState());

    // Figure out where we're sending the data
    String url = config.getItem(CaptureParameters.AGENT_STATE_REMOTE_ENDPOINT_URL);
    if (url == null) {
      logger.warn("#{} - URL for {} is invalid, unable to push state to remote server.",
              statePushCount, CaptureParameters.AGENT_STATE_REMOTE_ENDPOINT_URL);
      return;
    }
    try {
      if (url.charAt(url.length() - 1) == '/') {
        url += config.getItem(CaptureParameters.AGENT_NAME);
      } else {
        url += "/" + config.getItem(CaptureParameters.AGENT_NAME);
      }
    } catch (StringIndexOutOfBoundsException e) {
      logger.warn("#" + statePushCount + " - Unable to build valid state endpoint for agents.");
      return;
    }

    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    // formParams.add(new BasicNameValuePair("agentName", a.getName()));
    formParams.add(new BasicNameValuePair("state", state.getAgentState()));
    formParams.add(new BasicNameValuePair("address", localAddress));

    send(formParams, url);
  }

  /**
   * Sends an update for each of the recordings currently being tracked in the system.
   */
  protected void sendRecordingState() {

    // Figure out where we're sending the data
    String url = config.getItem(CaptureParameters.RECORDING_STATE_REMOTE_ENDPOINT_URL);
    if (url == null) {
      logger.warn("#{} - URL for {} is invalid, unable to push recording state to remote server.",
              statePushCount, CaptureParameters.RECORDING_STATE_REMOTE_ENDPOINT_URL);
      return;
    }
    try {
      if (url.charAt(url.length() - 1) != '/') {
        url += "/";
      }
    } catch (StringIndexOutOfBoundsException e) {
      logger.warn("#{} - Unable to build valid state endpoint for recordings.", statePushCount);
      return;
    }

    // For each recording being tracked by the system send an update
    Map<String, AgentRecording> recordings = state.getKnownRecordings();
    for (Entry<String, AgentRecording> e : recordings.entrySet()) {
      List<NameValuePair> formParams = new ArrayList<NameValuePair>();
      formParams.add(new BasicNameValuePair("state", e.getValue().getState()));
      logger.debug("#" + statePushCount + " - Sending recording {}'s state: {}.", e.getKey(), e.getValue().getState());
      String myURL = url + e.getKey();
      send(formParams, myURL);
    }
  }

  /**
   * Utility method to POST data to a URL. This method encodes the data in UTF-8 as post data, rather than multipart
   * MIME.
   * 
   * @param formParams
   *          The data to send.
   * @param url
   *          The URL to send the data to.
   */
  private void send(List<NameValuePair> formParams, String url) {
    HttpResponse resp = null;
    HttpPost remoteServer = new HttpPost(url);
    try {
      remoteServer.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      logger.error("#" + statePushCount + " - Unable to send data because the URL encoding is not supported.");
      return;
    }
    try {
      if (client == null) {
        logger.error("#" + statePushCount + " - Unable to send data because http client is null.");
        return;
      }

      resp = client.execute(remoteServer);
      if (resp.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
        logger.info("#" + statePushCount + " - State push to " + toString() + " to {} failed with code {}.", url, resp.getStatusLine().getStatusCode());
      }
      else {
        logger.debug("#" + statePushCount + " - State push {} to {} was successful.", toString(), url);
      }
    } catch (TrustedHttpClientException e) {
      logger.warn("#" + statePushCount + " - Unable to communicate with server at {}, message reads: {}.", url, e);
    } finally {
      if (resp != null) {
        client.close(resp);
      }
    }
  }
}
