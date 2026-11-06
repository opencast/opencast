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


package org.opencastproject.workflow.handler.notification;

import static java.lang.String.format;
import static org.apache.hc.core5.http.HttpStatus.SC_ACCEPTED;
import static org.apache.hc.core5.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.HttpUtil;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.Strings;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Http Notification Operation Handler",
        "workflow.operation=http-notify"
    }
)
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

  /** Name of the mediapackage id HTTP parameter */
  public static final String HTTP_PARAM_MEDIAPACKAGE = "mediaPackageId";

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
    HttpUriRequestBase request = null;
    if (Strings.CI.equals("post", method)) {
      request = new HttpPost(urlPath);
    } else if (Strings.CI.equals("put", method)) {
      request = new HttpPut(urlPath);
    } else {
      throw new WorkflowOperationException("The configuration key '" + OPT_METHOD + "' only supports 'post' and 'put'");
    }
    logger.debug("Request will be sent using the '{}' method", method);

    // Add event parameters as form parameters
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

    // Add the mediapackage id
    params.add(new BasicNameValuePair(HTTP_PARAM_MEDIAPACKAGE,
            workflowInstance.getMediaPackage().getIdentifier().toString()));

    request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

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
  private boolean executeRequest(ClassicHttpRequest request, int maxAttempts, int timeout, int sleepTime) {

    logger.debug("Executing notification request on target {}, {} attempts left", request.getRequestUri(), maxAttempts);

    final int statusCode;
    try (CloseableHttpClient httpClient = HttpUtil.clientBuilder(timeout, timeout).build()) {
      statusCode = httpClient.execute(request, response -> response.getCode());
    } catch (ClientProtocolException e) {
      logger.error("Protocol error during execution of query on target {}: {}", request.getRequestUri(),
          e.getMessage());
      return false;
    } catch (IOException e) {
      logger.error("I/O error during execution of query on target {}: {}", request.getRequestUri(), e.getMessage());
      return false;
    }

    if (statusCode == SC_OK || statusCode == SC_NO_CONTENT || statusCode == SC_ACCEPTED) {
      logger.debug("Request successfully executed on target {}, status code: {}", request.getRequestUri(), statusCode);
      return true;
    } else if (maxAttempts > 1) {
      logger.debug("Request failed on target {}, status code: {}, will retry in {} seconds", request.getRequestUri(),
              statusCode, sleepTime / 1000);
      try {
        Thread.sleep(sleepTime);
        return executeRequest(request, --maxAttempts, timeout, sleepTime * SLEEP_SCALE_FACTOR);
      } catch (InterruptedException e) {
        logger.error("Error during sleep time before new notification request try: {}", e.getMessage());
        return false;
      }
    } else {
      logger.warn("Request failed on target {}, status code: {}, no more attempt.", request.getRequestUri(),
          statusCode);
      return false;
    }
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

}
