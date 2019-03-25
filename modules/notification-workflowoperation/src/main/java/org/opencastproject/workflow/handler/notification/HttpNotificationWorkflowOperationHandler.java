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


package org.opencastproject.workflow.handler.notification;

import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Workflow operation handler that will send HTTP POST or PUT requests to a specified address.
 * <p>
 * The request can contain a message type and a message body and automatically includes the workflow instance id. Should
 * the notification fail, a retry strategy is implemented.
 * <p>
 * Requests will be send using the POST method by default, PUT is a supported alternative method.
 *
 */
public class HttpNotificationWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** Configuration key for the target URL of the notification request */
  public static final String OPT_URL_PATH = "url";

  /** Configuration key for the notification event */
  public static final String OPT_NOTIFICATION_SUBJECT = "subject";

  /** Configuration key for the notification message */
  public static final String OPT_NOTIFICATION_MESSAGE = "message";

  /** Configuration key for the HTTP method to use (put or post) */
  public static final String OPT_METHOD = "method";

  /** Configuration key for the maximal attempts for the notification request */
  public static final String OPT_MAX_RETRY = "max-retry";

  /** Configuration key for the request timeout */
  public static final String OPT_TIMEOUT = "timeout";

  /** Name of the subject HTTP parameter */
  public static final String HTTP_PARAM_SUBJECT = "subject";

  /** Name of the message HTTP parameter */
  public static final String HTTP_PARAM_MESSAGE = "message";

  /** Name of the workflow instance id HTTP parameter */
  public static final String HTTP_PARAM_WORKFLOW = "workflowInstanceId";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(HttpNotificationWorkflowOperationHandler.class);

  /** Default value for the number of attempts for a request */
  private static final int DEFAULT_MAX_RETRY = 5;

  /** Default maximum wait time the client when trying to execute a request */
  private static final int DEFAULT_TIMEOUT = 10 * 1000;

  /** Default time between two request attempts */
  public static final int INITIAL_SLEEP_TIME = 10 * 1000;

  /** The scale factor to the sleep time between two notification attempts */
  public static final int SLEEP_SCALE_FACTOR = 2;

  /** The http client to use when connecting to remote servers */
  protected HttpClient client = null;

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running HTTP notification workflow operation on workflow {}", workflowInstance.getId());

    int maxRetry = DEFAULT_MAX_RETRY;
    int timeout = DEFAULT_TIMEOUT;

    // Required configuration
    String urlPath = getConfig(workflowInstance, OPT_URL_PATH);

    // Optional configuration
    String notificationSubject = getConfig(workflowInstance, OPT_NOTIFICATION_SUBJECT, null);
    String notificationMessage = getConfig(workflowInstance, OPT_NOTIFICATION_MESSAGE, null);
    String method = getConfig(workflowInstance, OPT_METHOD, "post");
    String maxRetryOpt = getConfig(workflowInstance, OPT_MAX_RETRY, null);
    String timeoutOpt = getConfig(workflowInstance, OPT_TIMEOUT, null);


    // If set, convert the timeout to milliseconds
    if (timeoutOpt != null) {
      timeout = Integer.parseInt(timeoutOpt) * 1000;
    }

    // Is there a need to retry on failure?
    if (maxRetryOpt != null) {
      maxRetry = Integer.parseInt(maxRetryOpt);
    }

    // Figure out which request method to use
    HttpEntityEnclosingRequestBase request = null;
    if (StringUtils.equalsIgnoreCase("post", method)) {
      request = new HttpPost(urlPath);
    } else if (StringUtils.equalsIgnoreCase("put", method)) {
      request = new HttpPut(urlPath);
    } else {
      throw new WorkflowOperationException("The configuration key '" + OPT_METHOD + "' only supports 'post' and 'put'");
    }
    logger.debug("Request will be sent using the '{}' method", method);

    // Add event parameters as form parameters
    try {
      List<BasicNameValuePair> params = new ArrayList<>();

      // Add the subject (if specified)
      if (notificationSubject != null) {
        params.add(new BasicNameValuePair(HTTP_PARAM_SUBJECT, notificationSubject));
      }

      // Add the message (if specified)
      if (notificationMessage != null) {
        params.add(new BasicNameValuePair(HTTP_PARAM_MESSAGE, notificationMessage));
      }

      // Add the workflow instance id
      params.add(new BasicNameValuePair(HTTP_PARAM_WORKFLOW, Long.toString(workflowInstance.getId())));

      request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new WorkflowOperationException(
              "Error happened during the encoding of the event parameter as form parameter:", e);
    }

    // Execute the request
    if (!executeRequest(request, maxRetry, timeout, INITIAL_SLEEP_TIME)) {
      throw new WorkflowOperationException(format("Notification could not be delivered to %s", urlPath));
    }

    return createResult(workflowInstance.getMediaPackage(), Action.CONTINUE);
  }

  /**
   * Execute the given notification request. If the target is not responding, retry as many time as the maxAttampts
   * parameter with in between each try a sleep time.
   *
   * @param request
   *          The request to execute
   * @param maxAttempts
   *          The number of attempts in case of error
   * @param timeout
   *          The wait time in milliseconds at which a connection attempt will throw
   * @return true if the request has been executed successfully
   */
  private boolean executeRequest(HttpUriRequest request, int maxAttempts, int timeout, int sleepTime) {

    logger.debug(format("Executing notification request on target %s, %d attemps left", request.getURI(), maxAttempts));

    RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .setSocketTimeout(timeout).build();
    CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

    HttpResponse response;
    try {
      response = httpClient.execute(request);
    } catch (ClientProtocolException e) {
      logger.error("Protocol error during execution of query on target {}: {}", request.getURI(), e.getMessage());
      return false;
    } catch (IOException e) {
      logger.error("I/O error during execution of query on target {}: {}", request.getURI(), e.getMessage());
      return false;
    }

    Integer statusCode = response.getStatusLine().getStatusCode();
    if (statusCode == SC_OK || statusCode == SC_NO_CONTENT || statusCode == SC_ACCEPTED) {
      logger.debug(format("Request successfully executed on target %s, status code: %d", request.getURI(), statusCode));
      return true;
    } else if (maxAttempts > 1) {
      logger.debug(format("Request failed on target %s, status code: %d, will retry in %d seconds", request.getURI(),
              statusCode, sleepTime / 1000));
      try {
        Thread.sleep(sleepTime);
        return executeRequest(request, --maxAttempts, timeout, sleepTime * SLEEP_SCALE_FACTOR);
      } catch (InterruptedException e) {
        logger.error("Error during sleep time before new notification request try: {}", e.getMessage());
        return false;
      }
    } else {
      logger.warn("Request failed on target {}, status code: {}, no more attempt.", request.getURI(), statusCode);
      return false;
    }
  }

}
