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
package org.opencastproject.capture.impl;

import org.opencastproject.capture.admin.api.AgentState;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Implementation of shell commands for the capture agent.
 */
public class CaptureAgentShellCommands {

  /** The agent that is being controlled */
  private CaptureAgentImpl agent = null;

  /** Current recording identifier */
  private String recordingId = null;

  /**
   * Creates a command object for the given capture agent.
   *
   * @param agent
   *          the agent
   */
  CaptureAgentShellCommands(CaptureAgentImpl agent) {
    this.agent = agent;
  }

  /**
   * Prints the capture agent state to <code>System.out</code>.
   */
  public void status() {
    System.out.println(agent.getAgentState());
  }

  /**
   * Tells the capture agent to start capturing with the default set of properties.
   */
  public void start() {
    if (AgentState.IDLE.equals(agent.getAgentState())) {
      System.err.println("The agent is currently busy (" + recordingId + ")");
      return;
    } else if (recordingId != null) {
      System.err.println("Skipping recording " + recordingId + "");
    }
    recordingId = agent.startCapture();
    System.out.println("Recording " + recordingId + " started");
  }

  /**
   * Tells the capture agent to stop capturing.
   */
  public void stop() {
    agent.stopCapture(recordingId, true);
    System.out.println("Recording " + recordingId + " stopped");
  }

  /**
   * Tells the capture agent to send the recording to the ingest service.
   */
  public void ingest() {
    if (recordingId == null) {
      System.err.println("Nothing has been recorded");
      return;
    }

    try {
      if (!agent.createManifest(recordingId)) {
        System.out.println("Error creating manifest, aborting!");
        return;
      }
    } catch (NoSuchAlgorithmException e1) {
      System.err.println("Unable to create manifest, NoSuchAlgorithmException was thrown: " + e1.getMessage());
      return;
    } catch (IOException e1) {
      System.err.println("Unable to create manifest, IOException was thrown: " + e1.getMessage());
      return;
    }

    System.out.println("Zipping recording (" + recordingId + ")");
    File zip = agent.zipFiles(recordingId);
    if (zip == null) {
      System.out.println("Error zipping files, aborting!");
      return;
    }

    System.out.println("Ingesting recording (" + recordingId + ")");
    int httpCode = agent.ingest(recordingId);
    if (httpCode != 200) {
      System.out.println("Error ingesting, HTTP status code was " + httpCode);
      return;
    }

    recordingId = null;
  }

  /**
   * Tells the capture agent to forget about the current recording
   */
  public void reset() {
    recordingId = null;
  }

  /**
   * Starts and then stops a fake capture, then ingests the "captured" media. Same as calling
   * <ul>
   * <li><code>start()</code></li>
   * <li><code>stop()</code></li>
   * <li><code>ingest()</code></li>
   * </ul>
   */
  public void capture() {
    start();
    stop();
    ingest();
  }

}
