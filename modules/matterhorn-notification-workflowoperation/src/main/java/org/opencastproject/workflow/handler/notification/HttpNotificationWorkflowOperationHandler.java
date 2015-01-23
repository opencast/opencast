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

package org.opencastproject.workflow.handler.notification;

import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

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

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(OPT_URL_PATH, "Notification request target");
    CONFIG_OPTIONS.put(OPT_NOTIFICATION_SUBJECT, "Notification title");
    CONFIG_OPTIONS.put(OPT_NOTIFICATION_MESSAGE, "Notification");
    CONFIG_OPTIONS.put(OPT_METHOD, "HTTP Method");
    CONFIG_OPTIONS.put(OPT_MAX_RETRY, "Maximum attempts for the notification request");
    CONFIG_OPTIONS.put(OPT_TIMEOUT, "Request timeout");
  }

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
   *
   * @see org.opencastproject.workflow.handler.workflow.ResumableWorkflowOperationHandlerBase#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running HTTP notification workflow operation on workflow {}", workflowInstance.getId());

    String urlPath = null;
    int maxRetry = DEFAULT_MAX_RETRY;
    int timeout = DEFAULT_TIMEOUT;

    Option<String> notificationSubjectOpt = getCfg(workflowInstance, OPT_NOTIFICATION_SUBJECT);
    Option<String> notificationMessageOpt = getCfg(workflowInstance, OPT_NOTIFICATION_MESSAGE);
    Option<String> methodOpt = getCfg(workflowInstance, OPT_METHOD);
    Option<String> urlPathOpt = getCfg(workflowInstance, OPT_URL_PATH);
    Option<String> maxRetryOpt = getCfg(workflowInstance, OPT_MAX_RETRY);
    Option<String> timeoutOpt = getCfg(workflowInstance, OPT_TIMEOUT);

    // Make sure that at least the url has been set.
    try {
      urlPath = urlPathOpt.get();
    } catch (IllegalStateException e) {
      throw new WorkflowOperationException(format(
              "The %s configuration key is a required parameter for the HTTP notification workflow operation.",
              OPT_URL_PATH));
    }

    // If set, convert the timeout to milliseconds
    if (timeoutOpt.isSome())
      timeout = Integer.parseInt(StringUtils.trimToNull(timeoutOpt.get())) * 1000;

    // Is there a need to retry on failure?
    if (maxRetryOpt.isSome())
      maxRetry = Integer.parseInt(StringUtils.trimToNull(maxRetryOpt.get()));

    // Figure out which request method to use
    HttpEntityEnclosingRequestBase request = null;
    if (methodOpt.isSome()) {
      if ("post".equalsIgnoreCase(methodOpt.get())) {
        logger.debug("Request will be sent using the 'post' method");
        request = new HttpPost(urlPath);
      } else if ("put".equalsIgnoreCase(methodOpt.get())) {
        logger.debug("Request will be sent using the 'put' method");
        request = new HttpPut(urlPath);
      } else
        throw new WorkflowOperationException("The configuration key '" + OPT_METHOD
                + "' only supports 'post' and 'put'");
    } else {
      logger.debug("Request will be sent using the default method 'post'");
      request = new HttpPost(urlPath);
    }

    // Add event parameters as form parameters
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();

      // Add the subject (if specified)
      if (notificationSubjectOpt.isSome())
        params.add(new BasicNameValuePair(HTTP_PARAM_SUBJECT, notificationSubjectOpt.get()));

      // Add the message (if specified)
      if (notificationMessageOpt.isSome())
        params.add(new BasicNameValuePair(HTTP_PARAM_MESSAGE, notificationMessageOpt.get()));

      // Add the workflow instance id
      params.add(new BasicNameValuePair(HTTP_PARAM_WORKFLOW, Long.toString(workflowInstance.getId())));

      request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new WorkflowOperationException(
              "Error happened during the encoding of the event parameter as form parameter: {}", e);
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

    DefaultHttpClient httpClient = new DefaultHttpClient();
    httpClient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);

    HttpResponse response;
    try {
      response = httpClient.execute(request);
    } catch (ClientProtocolException e) {
      logger.error(format("Protocol error during execution of query on target %s: %s", request.getURI(), e.getMessage()));
      return false;
    } catch (IOException e) {
      logger.error(format("I/O error during execution of query on target %s: %s", request.getURI(), e.getMessage()));
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
      logger.warn(format("Request failed on target %s, status code: %d, no more attempt.", request.getURI(), statusCode));
      return false;
    }
  }

}
