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
package org.opencastproject.workflow.remote;

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_PRECONDITION_FAILED;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.SolrUtils;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowListener;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowQuery.QueryTerm;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowStatistics;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An implementation of the workflow service that communicates with a remote workflow service via HTTP.
 */
public class WorkflowServiceRemoteImpl extends RemoteBase implements WorkflowService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceRemoteImpl.class);

  public WorkflowServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowDefinitionById(java.lang.String)
   */
  @Override
  public WorkflowDefinition getWorkflowDefinitionById(String id) throws WorkflowDatabaseException, NotFoundException {
    HttpGet get = new HttpGet("/definition/" + id + ".xml");
    HttpResponse response = getResponse(get, SC_NOT_FOUND, SC_OK);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Workflow definition " + id + " does not exist.");
        } else {
          return WorkflowParser.parseWorkflowDefinition(response.getEntity().getContent());
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Unable to connect to a remote workflow service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowById(long)
   */
  @Override
  public WorkflowInstance getWorkflowById(long id) throws WorkflowDatabaseException, NotFoundException {
    HttpGet get = new HttpGet("/instance/" + id + ".xml");
    HttpResponse response = getResponse(get, SC_NOT_FOUND, SC_OK);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Workflow instance " + id + " does not exist.");
        } else {
          return WorkflowParser.parseWorkflowInstance(response.getEntity().getContent());
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Unable to connect to a remote workflow service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService
   *      #getWorkflowInstances(org.opencastproject.workflow.api.WorkflowQuery)
   */
  @Override
  public WorkflowSet getWorkflowInstances(WorkflowQuery query) throws WorkflowDatabaseException {
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();

    if (query.getText() != null)
      queryStringParams.add(new BasicNameValuePair("q", query.getText()));

    if (query.getStates() != null) {
      for (QueryTerm stateQueryTerm : query.getStates()) {
        String value = stateQueryTerm.isInclude() ? stateQueryTerm.getValue() : "-" + stateQueryTerm.getValue();
        queryStringParams.add(new BasicNameValuePair("state", value));
      }
    }

    if (query.getCurrentOperations() != null) {
      for (QueryTerm opQueryTerm : query.getCurrentOperations()) {
        String value = opQueryTerm.isInclude() ? opQueryTerm.getValue() : "-" + opQueryTerm.getValue();
        queryStringParams.add(new BasicNameValuePair("op", value));
      }
    }

    if (query.getSeriesId() != null)
      queryStringParams.add(new BasicNameValuePair("seriesId", query.getSeriesId()));

    if (query.getSeriesTitle() != null)
      queryStringParams.add(new BasicNameValuePair("seriesTitle", query.getSeriesTitle()));

    if (query.getMediaPackageId() != null)
      queryStringParams.add(new BasicNameValuePair("mp", query.getMediaPackageId()));

    if (query.getWorkflowDefinitionId() != null)
      queryStringParams.add(new BasicNameValuePair("workflowdefinition", query.getWorkflowDefinitionId()));

    if (query.getFromDate() != null)
      queryStringParams.add(new BasicNameValuePair("fromdate", SolrUtils.serializeDate(query.getFromDate())));

    if (query.getToDate() != null)
      queryStringParams.add(new BasicNameValuePair("todate", SolrUtils.serializeDate(query.getToDate())));

    if (query.getCreator() != null)
      queryStringParams.add(new BasicNameValuePair("creator", query.getCreator()));

    if (query.getContributor() != null)
      queryStringParams.add(new BasicNameValuePair("contributor", query.getContributor()));

    if (query.getLanguage() != null)
      queryStringParams.add(new BasicNameValuePair("language", query.getLanguage()));

    if (query.getLicense() != null)
      queryStringParams.add(new BasicNameValuePair("license", query.getLicense()));

    if (query.getTitle() != null)
      queryStringParams.add(new BasicNameValuePair("title", query.getTitle()));

    if (query.getSubject() != null)
      queryStringParams.add(new BasicNameValuePair("subject", query.getSubject()));

    if (query.getSort() != null) {
      String sort = query.getSort().toString();
      if (!query.isSortAscending()) {
        sort += "_DESC";
      }
      queryStringParams.add(new BasicNameValuePair("sort", sort));
    }

    if (query.getStartPage() > 0)
      queryStringParams.add(new BasicNameValuePair("startPage", Long.toString(query.getStartPage())));

    if (query.getCount() > 0)
      queryStringParams.add(new BasicNameValuePair("count", Long.toString(query.getCount())));

    StringBuilder url = new StringBuilder();
    url.append("/instances.xml");
    if (queryStringParams.size() > 0)
      url.append("?" + URLEncodedUtils.format(queryStringParams, "UTF-8"));

    HttpGet get = new HttpGet(url.toString());
    HttpResponse response = getResponse(get);
    try {
      if (response != null)
        return WorkflowParser.parseWorkflowSet(response.getEntity().getContent());
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Workflow instances can not be loaded from a remote workflow service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowInstancesForAdministrativeRead(org.opencastproject.workflow.api.WorkflowQuery)
   */
  @Override
  public WorkflowSet getWorkflowInstancesForAdministrativeRead(WorkflowQuery q) throws WorkflowDatabaseException,
          UnauthorizedException {
    return getWorkflowInstances(q);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#getStatistics()
   */
  @Override
  public WorkflowStatistics getStatistics() throws WorkflowDatabaseException {
    HttpGet get = new HttpGet("/statistics.xml");
    HttpResponse response = getResponse(get);
    try {
      if (response != null)
        return WorkflowParser.parseWorkflowStatistics(response.getEntity().getContent());
    } catch (Exception e) {
      throw new WorkflowDatabaseException("Unable to load workflow statistics", e);
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Unable to connect to a remote workflow service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.mediapackage.MediaPackage, java.util.Map)
   */
  @Override
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
          Map<String, String> properties) throws WorkflowDatabaseException {
    try {
      return start(workflowDefinition, mediaPackage, null, properties);
    } catch (NotFoundException e) {
      throw new IllegalStateException("A null parent workflow id should never result in a not found exception ", e);
    }
  }

  /**
   * Converts a Map<String, String> to s key=value\n string, suitable for the properties form parameter expected by the
   * workflow rest endpoint.
   *
   * @param props
   *          The map of strings
   * @return the string representation
   */
  private String mapToString(Map<String, String> props) {
    StringBuilder sb = new StringBuilder();
    for (Entry<String, String> entry : props.entrySet()) {
      sb.append(entry.getKey());
      sb.append("=");
      sb.append(entry.getValue());
      sb.append("\n");
    }
    return sb.toString();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.mediapackage.MediaPackage, Long, java.util.Map)
   */
  @Override
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
          Long parentWorkflowId, Map<String, String> properties) throws WorkflowDatabaseException, NotFoundException {
    HttpPost post = new HttpPost("/start");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      if (workflowDefinition != null)
        params.add(new BasicNameValuePair("definition", WorkflowParser.toXml(workflowDefinition)));
      params.add(new BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediaPackage)));
      if (parentWorkflowId != null)
        params.add(new BasicNameValuePair("parent", parentWorkflowId.toString()));
      if (properties != null)
        params.add(new BasicNameValuePair("properties", mapToString(properties)));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to assemble a remote workflow request", e);
    }
    HttpResponse response = getResponse(post, SC_NOT_FOUND, SC_OK);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Workflow instance " + parentWorkflowId + " does not exist.");
        } else {
          return WorkflowParser.parseWorkflowInstance(response.getEntity().getContent());
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new WorkflowDatabaseException("Unable to build a workflow from xml", e);
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Unable to start a remote workflow. The http response code was unexpected.");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage)
          throws WorkflowDatabaseException {
    try {
      return start(workflowDefinition, mediaPackage, null, null);
    } catch (NotFoundException e) {
      throw new IllegalStateException("A null parent workflow id should never result in a not found exception ", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#countWorkflowInstances()
   */
  @Override
  public long countWorkflowInstances() throws WorkflowDatabaseException {
    return countWorkflowInstances(null, null);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#countWorkflowInstances(org.opencastproject.workflow.api.WorkflowInstance.WorkflowState,
   *      java.lang.String)
   */
  @Override
  public long countWorkflowInstances(WorkflowState state, String operation) throws WorkflowDatabaseException {
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    if (state != null)
      queryStringParams.add(new BasicNameValuePair("state", state.toString()));
    if (operation != null)
      queryStringParams.add(new BasicNameValuePair("operation", operation));

    StringBuilder url = new StringBuilder("/count");
    if (queryStringParams.size() > 0) {
      url.append("?");
      url.append(URLEncodedUtils.format(queryStringParams, "UTF-8"));
    }

    HttpGet get = new HttpGet(url.toString());
    HttpResponse response = getResponse(get);
    try {
      if (response != null) {
        String body = null;
        try {
          body = EntityUtils.toString(response.getEntity());
          return Long.parseLong(body);
        } catch (NumberFormatException e) {
          throw new WorkflowDatabaseException("Unable to parse the response body as a long: " + body);
        }
      }
    } catch (ParseException e) {
      throw new WorkflowDatabaseException("Unable to parse the response body");
    } catch (IOException e) {
      throw new WorkflowDatabaseException("Unable to parse the response body");
    } finally {
      closeConnection(response);
    }

    throw new WorkflowDatabaseException("Unable to count workflow instances");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#stop(long)
   */
  @Override
  public WorkflowInstance stop(long workflowInstanceId) throws WorkflowDatabaseException, NotFoundException {
    HttpPost post = new HttpPost("/stop");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("id", Long.toString(workflowInstanceId)));
    try {
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unable to assemble a remote workflow service request", e);
    }
    HttpResponse response = getResponse(post, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (response.getStatusLine().getStatusCode() == SC_NOT_FOUND) {
          throw new NotFoundException("Workflow instance with id='" + workflowInstanceId + "' not found");
        } else {
          logger.info("Workflow '{}' stopped", workflowInstanceId);
          return WorkflowParser.parseWorkflowInstance(response.getEntity().getContent());
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Unable to stop workflow instance " + workflowInstanceId);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#suspend(long)
   */
  @Override
  public WorkflowInstance suspend(long workflowInstanceId) throws WorkflowDatabaseException, NotFoundException {
    HttpPost post = new HttpPost("/suspend");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("id", Long.toString(workflowInstanceId)));
    try {
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unable to assemble a remote workflow service request", e);
    }
    HttpResponse response = getResponse(post, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (response.getStatusLine().getStatusCode() == SC_NOT_FOUND) {
          throw new NotFoundException("Workflow instance with id='" + workflowInstanceId + "' not found");
        } else {
          logger.info("Workflow '{}' suspended", workflowInstanceId);
          return WorkflowParser.parseWorkflowInstance(response.getEntity().getContent());
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Unable to suspend workflow instance " + workflowInstanceId);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#resume(long)
   */
  @Override
  public WorkflowInstance resume(long workflowInstanceId) throws NotFoundException, UnauthorizedException,
          WorkflowException {
    return resume(workflowInstanceId, null);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#resume(long, java.util.Map)
   */
  @Override
  public WorkflowInstance resume(long workflowInstanceId, Map<String, String> properties) throws NotFoundException,
          UnauthorizedException, WorkflowException {
    HttpPost post = new HttpPost("/resume");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("id", Long.toString(workflowInstanceId)));
    if (properties != null)
      params.add(new BasicNameValuePair("properties", mapToString(properties)));
    try {
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unable to assemble a remote workflow service request", e);
    }
    HttpResponse response = getResponse(post, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (response.getStatusLine().getStatusCode() == SC_NOT_FOUND) {
          throw new NotFoundException("Workflow instance with id='" + workflowInstanceId + "' not found");
        } else if (response.getStatusLine().getStatusCode() == SC_UNAUTHORIZED) {
          throw new UnauthorizedException("You do not have permission to resume");
        } else {
          logger.info("Workflow '{}' resumed", workflowInstanceId);
          return WorkflowParser.parseWorkflowInstance(response.getEntity().getContent());
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new WorkflowException(e);
    } finally {
      closeConnection(response);
    }
    throw new WorkflowException("Unable to resume workflow instance " + workflowInstanceId);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void update(WorkflowInstance workflowInstance) throws WorkflowDatabaseException {
    HttpPost post = new HttpPost("/update");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("workflow", WorkflowParser.toXml(workflowInstance)));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unable to assemble a remote workflow service request", e);
    } catch (Exception e) {
      throw new IllegalStateException("unable to serialize workflow instance to xml");
    }

    HttpResponse response = getResponse(post, SC_NO_CONTENT);
    try {
      if (response != null) {
        logger.info("Workflow '{}' updated", workflowInstance);
        return;
      }
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Unable to update workflow instance " + workflowInstance.getId());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#remove(long)
   */
  @Override
  public void remove(long workflowInstanceId) throws WorkflowDatabaseException, WorkflowParsingException,
          NotFoundException, UnauthorizedException {
    HttpDelete delete = new HttpDelete("/remove/" + Long.toString(workflowInstanceId));
    HttpResponse response = getResponse(delete, SC_NO_CONTENT, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Workflow id not found: " + workflowInstanceId);
        } else {
          logger.info("Workflow '{}' removed", workflowInstanceId);
          return;
        }
      }
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Unable to remove workflow instance " + workflowInstanceId);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#listAvailableWorkflowDefinitions()
   */
  @Override
  public List<WorkflowDefinition> listAvailableWorkflowDefinitions() throws WorkflowDatabaseException {
    HttpGet get = new HttpGet("/definitions.xml");
    HttpResponse response = getResponse(get);
    try {
      if (response != null)
        return WorkflowParser.parseWorkflowDefinitions(response.getEntity().getContent());
    } catch (Exception e) {
      throw new IllegalStateException("Unable to parse workflow definitions");
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException(
            "Unable to read the registered workflow definitions from the remote workflow service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#registerWorkflowDefinition(org.opencastproject.workflow.api.WorkflowDefinition)
   */
  @Override
  public void registerWorkflowDefinition(WorkflowDefinition workflow) throws WorkflowDatabaseException {
    HttpPut put = new HttpPut("/definition");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("workflowDefinition", WorkflowParser.toXml(workflow)));
      put.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unable to assemble a remote workflow service request", e);
    } catch (Exception e) {
      throw new IllegalStateException("unable to serialize workflow definition to xml");
    }

    HttpResponse response = getResponse(put, SC_CREATED, SC_PRECONDITION_FAILED);
    try {
      if (response != null) {
        if (SC_PRECONDITION_FAILED == response.getStatusLine().getStatusCode()) {
          throw new IllegalStateException("A workflow definition with ID '" + workflow.getId()
                  + "' is already registered.");
        } else {
          logger.info("Workflow definition '{}' registered", workflow);
          return;
        }
      }
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Unable to register workflow definition " + workflow.getId());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#unregisterWorkflowDefinition(java.lang.String)
   */
  @Override
  public void unregisterWorkflowDefinition(String workflowDefinitionId) throws NotFoundException,
          WorkflowDatabaseException {
    HttpDelete delete = new HttpDelete("/definition/" + workflowDefinitionId);
    HttpResponse response = getResponse(delete, SC_NO_CONTENT, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Workflow definition '" + workflowDefinitionId + "' not found.");
        } else {
          logger.info("Workflow definition '{}' unregistered", workflowDefinitionId);
          return;
        }
      }
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Unable to delete workflow definition '" + workflowDefinitionId + "'");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#addWorkflowListener(org.opencastproject.workflow.api.WorkflowListener)
   */
  @Override
  public void addWorkflowListener(WorkflowListener listener) {
    throw new UnsupportedOperationException("Adding workflow listeners to a remote workflow service is not supported");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#removeWorkflowListener(org.opencastproject.workflow.api.WorkflowListener)
   */
  @Override
  public void removeWorkflowListener(WorkflowListener listener) {
    throw new UnsupportedOperationException(
            "Removing workflow listeners from a remote workflow service is not supported");
  }

  @Override
  public void moveMissingCapturesFromUpcomingToFailedStatus(long buffer) throws WorkflowDatabaseException {
    if (buffer < 0) {
      throw new IllegalArgumentException("Buffer '" + buffer
              + "' is not a valid value, it must be equal or greater than 0.");
    }

    HttpPost post = new HttpPost("/failMissedCaptures");

    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("buffer", String.valueOf(buffer)));
    try {
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unable to assemble a remote workflow service request", e);
    }

    HttpResponse response = getResponse(post, SC_OK);
    try {
      if (response != null) {
        logger.info("Successful request to workflow cleanup endpoint");
        return;
      }
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Unable to successfully request the workflow cleanup endpoint");
  }

  @Override
  public void moveMissingIngestsFromUpcomingToFailedStatus(long buffer) throws WorkflowDatabaseException {
    if (buffer < 0) {
      throw new IllegalArgumentException("Buffer '" + buffer
              + "' is not a valid value, it must be equal or greater than 0.");
    }

    HttpPost post = new HttpPost("/failMissedIngests");

    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("buffer", String.valueOf(buffer)));
    try {
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unable to assemble a remote workflow service request", e);
    }

    HttpResponse response = getResponse(post, SC_OK);
    try {
      if (response != null) {
        logger.info("Successful request to workflow cleanup endpoint");
        return;
      }
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Unable to successfully request the workflow cleanup endpoint");
  }

  @Override
  public void cleanupWorkflowInstances(int lifetime, WorkflowState state) throws WorkflowDatabaseException,
          UnauthorizedException {
    HttpPost post = new HttpPost("/cleanup");

    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("lifetime", String.valueOf(lifetime)));
    if (state != null)
      params.add(new BasicNameValuePair("state", state.toString()));
    try {
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unable to assemble a remote workflow service request", e);
    }

    HttpResponse response = getResponse(post, SC_OK, HttpStatus.SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (HttpStatus.SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          throw new UnauthorizedException("You do not have permission to cleanup");
        } else {
          logger.info("Successful request to workflow cleanup endpoint");
          return;
        }
      }
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Unable to successfully request the workflow cleanup endpoint");
  }
}
