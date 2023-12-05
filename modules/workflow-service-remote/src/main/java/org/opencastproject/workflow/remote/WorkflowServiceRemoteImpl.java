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

package org.opencastproject.workflow.remote;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowListener;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.XmlWorkflowParser;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.parser.JSONParser;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * An implementation of the workflow service that communicates with a remote workflow service via HTTP.
 */
@Component(
  property = {
    "service.description=Workflow Remote Service Proxy"
  },
  immediate =  true,
  service = { WorkflowService.class }
)
public class WorkflowServiceRemoteImpl extends RemoteBase implements WorkflowService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceRemoteImpl.class);

  public WorkflowServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  /**
   * Sets the trusted http client
   *
   * @param client
   */
  @Override
  @Reference
  public void setTrustedHttpClient(TrustedHttpClient client) {
    super.setTrustedHttpClient(client);
  }

  /**
   * Sets the remote service manager.
   *
   * @param remoteServiceManager
   */
  @Override
  @Reference
  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
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
          return XmlWorkflowParser.parseWorkflowDefinition(response.getEntity().getContent());
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
          return XmlWorkflowParser.parseWorkflowInstance(response.getEntity().getContent());
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

  @Override
  public List<WorkflowInstance> getWorkflowInstancesByMediaPackage(String mediaPackageId)
          throws WorkflowDatabaseException {
    HttpGet get = new HttpGet("/mediaPackage/" + mediaPackageId + "/instances.xml");
    HttpResponse response = getResponse(get);
    try {
      if (response != null)
        return XmlWorkflowParser.parseWorkflowSet(response.getEntity().getContent()).getItems();
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Workflow instances can not be loaded from a remote workflow service");
  }

  @Override
  public Optional<WorkflowInstance> getRunningWorkflowInstanceByMediaPackage(String mediaPackageId, String action)
          throws WorkflowException {

    HttpGet get = new HttpGet("/mediaPackage/" + mediaPackageId + "/instances.xml");
    HttpResponse response = getResponse(get, SC_NOT_FOUND, SC_OK);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          return Optional.empty();
        }
        return Optional.ofNullable(
                XmlWorkflowParser.parseWorkflowInstance(response.getEntity().getContent())
        );
      }
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Workflow instances can not be loaded from a remote workflow service");
  }

  @Override
  public boolean mediaPackageHasActiveWorkflows(String mediaPackageId) throws WorkflowDatabaseException {
    HttpGet get = new HttpGet("/mediaPackage/" + mediaPackageId + "/hasActiveWorkflows");
    HttpResponse response = getResponse(get);
    try {
      if (response != null)
        return Boolean.parseBoolean(response.getEntity().getContent().toString());
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    } finally {
      closeConnection(response);
    }
    throw new WorkflowDatabaseException("Workflow instances can not be loaded from a remote workflow service");
  }

  @Override
  public boolean userHasActiveWorkflows(String userId) throws WorkflowDatabaseException {
    HttpGet get = new HttpGet("/user/" + userId + "/hasActiveWorkflows");
    HttpResponse response = getResponse(get);
    try {
      if (response != null)
        return Boolean.parseBoolean(response.getEntity().getContent().toString());
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
      List<BasicNameValuePair> params = new ArrayList<>();
      if (workflowDefinition != null)
        params.add(new BasicNameValuePair("definition", XmlWorkflowParser.toXml(workflowDefinition)));
      params.add(new BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediaPackage)));
      if (parentWorkflowId != null)
        params.add(new BasicNameValuePair("parent", parentWorkflowId.toString()));
      if (properties != null)
        params.add(new BasicNameValuePair("properties", mapToString(properties)));
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to assemble a remote workflow request", e);
    }
    HttpResponse response = getResponse(post, SC_NOT_FOUND, SC_OK);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Workflow instance " + parentWorkflowId + " does not exist.");
        } else {
          return XmlWorkflowParser.parseWorkflowInstance(response.getEntity().getContent());
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
    return countWorkflowInstances(null);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#countWorkflowInstances(org.opencastproject.workflow.api.WorkflowInstance.WorkflowState)
   */
  @Override
  public long countWorkflowInstances(WorkflowState state) throws WorkflowDatabaseException {
    List<NameValuePair> queryStringParams = new ArrayList<>();
    if (state != null)
      queryStringParams.add(new BasicNameValuePair("state", state.toString()));

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
    } catch (ParseException | IOException e) {
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
    List<BasicNameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("id", Long.toString(workflowInstanceId)));
    try {
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
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
          return XmlWorkflowParser.parseWorkflowInstance(response.getEntity().getContent());
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
    List<BasicNameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("id", Long.toString(workflowInstanceId)));
    try {
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
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
          return XmlWorkflowParser.parseWorkflowInstance(response.getEntity().getContent());
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
          WorkflowException, IllegalStateException {
    return resume(workflowInstanceId, null);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#resume(long, java.util.Map)
   */
  @Override
  public WorkflowInstance resume(long workflowInstanceId, Map<String, String> properties) throws NotFoundException,
          UnauthorizedException, WorkflowException, IllegalStateException {
    HttpPost post = new HttpPost("/resume");
    List<BasicNameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("id", Long.toString(workflowInstanceId)));
    if (properties != null)
      params.add(new BasicNameValuePair("properties", mapToString(properties)));
    post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
    HttpResponse response = getResponse(post, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED, SC_CONFLICT);
    try {
      if (response != null) {
        if (response.getStatusLine().getStatusCode() == SC_NOT_FOUND) {
          throw new NotFoundException("Workflow instance with id='" + workflowInstanceId + "' not found");
        } else if (response.getStatusLine().getStatusCode() == SC_UNAUTHORIZED) {
          throw new UnauthorizedException("You do not have permission to resume");
        } else if (response.getStatusLine().getStatusCode() == SC_CONFLICT) {
          throw new IllegalStateException("Can not resume a workflow where the current state is not in paused");
        } else {
          logger.info("Workflow '{}' resumed", workflowInstanceId);
          return XmlWorkflowParser.parseWorkflowInstance(response.getEntity().getContent());
        }
      }
    } catch (NotFoundException | UnauthorizedException | IllegalStateException e) {
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
      List<BasicNameValuePair> params = new ArrayList<>();
      params.add(new BasicNameValuePair("workflow", XmlWorkflowParser.toXml(workflowInstance)));
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
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
  public void remove(long workflowInstanceId) throws WorkflowDatabaseException, NotFoundException {
    remove(workflowInstanceId, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowService#remove(long, boolean)
   */
  @Override
  public void remove(long workflowInstanceId, boolean force) throws WorkflowDatabaseException, NotFoundException {
    String deleteString = "/remove/" + Long.toString(workflowInstanceId);

    if (force) {
      List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
      queryStringParams.add(new BasicNameValuePair("force", "true"));
      deleteString = deleteString + "?" + URLEncodedUtils.format(queryStringParams, "UTF_8");
    }

    HttpDelete delete = new HttpDelete(deleteString);

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
      if (response != null) {
        List<WorkflowDefinition> list = XmlWorkflowParser.parseWorkflowDefinitions(response.getEntity().getContent());
        Collections.sort(list); //sorts by title
        return list;
      }
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
  public void cleanupWorkflowInstances(int lifetime, WorkflowState state) throws WorkflowDatabaseException,
          UnauthorizedException {
    HttpPost post = new HttpPost("/cleanup");

    List<BasicNameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("lifetime", String.valueOf(lifetime)));
    if (state != null)
      params.add(new BasicNameValuePair("state", state.toString()));
    try {
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
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

  @Override
  public Map<String, Map<String, String>> getWorkflowStateMappings() {
    HttpGet get = new HttpGet("/statemappings.json");
    HttpResponse response = getResponse(get);
    try {
      if (response != null) {
        return (Map<String, Map<String, String>>) new JSONParser().parse(IOUtils.toString(response.getEntity().getContent(), "utf-8"));
      }
    } catch (Exception e) {
      throw new IllegalStateException("Unable to parse workflow state mappings");
    } finally {
      closeConnection(response);
    }
    return new HashMap<>();
  }
}
