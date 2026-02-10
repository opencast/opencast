/*
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
package org.opencastproject.workflow.handler.matrix;

import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import com.google.gson.Gson;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * The Matrix notification workflow operation handler can send notifications to Matrix rooms.
 *
 * <p>
 * This workflow operation sends a message to a specified Matrix room using the Matrix Client-Server API.
 * It requires the following configuration parameters:
 * </p>
 * <ul>
 *   <li>{@code room-id} - The Matrix room ID to send the notification to</li>
 *   <li>{@code message} - The message body to send</li>
 * </ul>
 *
 * <p>
 * The handler also requires the following configuration properties to be set:
 * </p>
 * <ul>
 *   <li>{@code matrix.server.url} - The Matrix server URL (default: https://matrix.org)</li>
 *   <li>{@code matrix.access.token} - The Matrix access token for authentication</li>
 *   <li>{@code matrix.max.retry} - Maximum number of retry attempts (default: 5)</li>
 *   <li>{@code matrix.timeout} - Request timeout in seconds (default: 10)</li>
 * </ul>
 */
@Component(
    property = {
        "service.description=Matrix Notification Workflow Operation Handler",
        "workflow.operation=matrix-notification",
        "service.pid=org.opencastproject.workflow.handler.matrix.MatrixNotificationWorkflowOperationHandler"
    },
    immediate = true,
    service = WorkflowOperationHandler.class
)
public class MatrixNotificationWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /**
   * Configuration key for the Matrix room ID
   */
  public static final String OPT_ROOM_ID = "room-id";

  /**
   * Configuration key for the Matrix message body
   */
  public static final String OPT_MESSAGE = "message";

  /**
   * The logging facility
   */
  private static final Logger logger = LoggerFactory.getLogger(MatrixNotificationWorkflowOperationHandler.class);

  /**
   * Default value for the number of attempts for a request
   */
  private static final int DEFAULT_MAX_RETRY = 5;

  /**
   * Default maximum time in seconds the client should wait when trying to execute a request
   */
  private static final int DEFAULT_TIMEOUT = 10;

  /**
   * Default time between two request attempts
   */
  public static final int INITIAL_SLEEP_TIME = 10;

  /**
   * The scale factor to the sleep time between two notification attempts
   */
  public static final int SLEEP_SCALE_FACTOR = 2;

  /**
   * Configuration key for the Matrix server URL
   */
  private static final String CFG_SERVER_URL = "matrix.server.url";

  /**
   * Configuration key for the Matrix access token
   */
  private static final String CFG_ACCESS_TOKEN = "matrix.access.token";

  /**
   * Configuration key for the maximal attempts for the notification request
   */
  private static final String CFG_MAX_RETRY = "matrix.max.retry";

  /**
   * Configuration key for the request timeout
   */
  private static final String CFG_TIMEOUT = "matrix.timeout";

  /**
   * The default Matrix server URL
   */
  private static final String DEFAULT_SERVER_URL = "https://matrix.org";

  /**
   * The configured Matrix server URL
   */
  private String serverUrl = DEFAULT_SERVER_URL;

  /**
   * The configured Matrix access token
   */
  private String accessToken = "";

  /**
   * The maximal attempts for the notification request
   */
  private int maxRetry = DEFAULT_MAX_RETRY;

  /**
   * The request timeout in seconds
   */
  private int timeout = DEFAULT_TIMEOUT;

  /**
   * JSON parser
   */
  private final Gson gson = new Gson();

  /**
   * Activates this component and initializes the configuration properties.
   *
   * @param cc the component context
   */
  @Activate
  @Modified
  protected void activate(ComponentContext cc) {
    var properties = cc.getProperties();
    serverUrl = Objects.toString(properties.get(CFG_SERVER_URL), DEFAULT_SERVER_URL);
    accessToken = Objects.toString(properties.get(CFG_ACCESS_TOKEN));
    maxRetry = NumberUtils.toInt((String) properties.get(CFG_MAX_RETRY), DEFAULT_MAX_RETRY);
    timeout = NumberUtils.toInt((String) properties.get(CFG_TIMEOUT), DEFAULT_TIMEOUT);
  }

  /**
   * Starts the Matrix notification workflow operation.
   *
   * @param workflow the workflow instance
   * @param context the job context
   * @return the workflow operation result
   * @throws WorkflowOperationException if the operation fails
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflow, JobContext context)
          throws WorkflowOperationException {

    var mediapackage = workflow.getMediaPackage();
    logger.debug("Running Matrix notification workflow operation on media package {}", mediapackage);

    // Required configuration
    var roomId = getConfig(workflow, OPT_ROOM_ID);
    var message = getConfig(workflow, OPT_MESSAGE);

    // Create the JSON payload
    var payload = gson.toJson(Map.of("msgtype", "m.text", "body", message));

    // Execute the request
    executeMatrixRequest(roomId, payload, maxRetry, INITIAL_SLEEP_TIME);

    // Continue the workflow, passing the possibly modified media package to the next operation
    return createResult(mediapackage, Action.CONTINUE);
  }

  /**
   * Executes a Matrix request to send a notification to a room.
   *
   * @param roomId the Matrix room ID to send the notification to
   * @param payload the JSON payload to send
   * @param attempts the number of remaining attempts
   * @param sleepTime the time to sleep between attempts (in seconds)
   * @throws WorkflowOperationException if the operation fails
   */
  private void executeMatrixRequest(String roomId, String payload, int attempts, int sleepTime)
            throws WorkflowOperationException {

    // Build the full URL for the Matrix API endpoint
    var apiUrl = format("%s/_matrix/client/r0/rooms/%s/send/m.room.message", serverUrl, roomId);

    logger.debug("Executing Matrix notification request on target {}, {} attempts left", apiUrl, maxRetry);

    var config = RequestConfig.custom()
        .setConnectTimeout(timeout * 1000)
        .setConnectionRequestTimeout(timeout * 1000)
        .setSocketTimeout(timeout * 1000)
        .build();

    try (var httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
      var request = new HttpPost(apiUrl);
      request.setHeader("Authorization", "Bearer " + accessToken);
      request.setHeader("Content-Type", "application/json");
      request.setEntity(new StringEntity(payload));

      var response = httpClient.execute(request);

      var statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == SC_OK || statusCode == SC_NO_CONTENT || statusCode == SC_ACCEPTED) {
        logger.debug("Matrix request successfully executed on target {}, status code: {}", apiUrl, statusCode);
        return;

      } else if (attempts > 0) {
        logger.debug("Matrix request to {} failed (status: {}), retry in {} seconds", apiUrl, statusCode, sleepTime);
        Thread.sleep(sleepTime * 1000L);
        executeMatrixRequest(roomId, payload, --attempts, sleepTime * SLEEP_SCALE_FACTOR);
        return;
      }

      throw new WorkflowOperationException(
          format("Matrix request to %s failed (status: %s), no more attempt.", apiUrl, statusCode));
    } catch (IOException | InterruptedException e) {
      throw new WorkflowOperationException(e);
    }
  }

}
