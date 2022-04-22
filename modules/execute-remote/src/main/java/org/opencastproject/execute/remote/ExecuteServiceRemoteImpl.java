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

package org.opencastproject.execute.remote;

import org.opencastproject.execute.api.ExecuteException;
import org.opencastproject.execute.api.ExecuteService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Remote implementation of the execute service
 */
@Component(
    immediate = true,
    service = ExecuteService.class,
    property = {
        "service.description=Execute Service Remote Service Proxy"
    }
)
public class ExecuteServiceRemoteImpl extends RemoteBase implements ExecuteService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ExecuteServiceRemoteImpl.class);


  /**
   * Constructs a new execute service proxy
   */
  public ExecuteServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  /**
   * @see org.opencastproject.execute.api.ExecuteService#execute(java.lang.String, java.lang.String, org.opencastproject.mediapackage.MediaPackageElement, java.lang.String, org.opencastproject.mediapackage.MediaPackageElement.Type, float)
   */
  public Job execute(String exec, String params, MediaPackageElement inElement, String outFileName, Type type, float load)
          throws ExecuteException {
    HttpPost post = null;
    HttpResponse response = null;

    try {
      String inElementStr = MediaPackageElementParser.getAsXml(inElement);
      List<NameValuePair> formStringParams = new ArrayList<NameValuePair>();
      formStringParams.add(new BasicNameValuePair(EXEC_FORM_PARAM, exec));
      formStringParams.add(new BasicNameValuePair(PARAMS_FORM_PARAM, params));
      formStringParams.add(new BasicNameValuePair(LOAD_FORM_PARAM, String.valueOf(load)));
      formStringParams.add(new BasicNameValuePair(INPUT_ELEM_FORM_PARAM, inElementStr));
      if (outFileName != null)
        formStringParams.add(new BasicNameValuePair(OUTPUT_NAME_FORM_PARAMETER, outFileName));
      if (type != null)
        formStringParams.add(new BasicNameValuePair(TYPE_FORM_PARAMETER, type.toString()));

      logger.info("Executing command {} using a remote execute service", exec);

      post = new HttpPost("/" + ExecuteService.ENDPOINT_NAME);
      post.setEntity(new UrlEncodedFormEntity(formStringParams, "UTF-8"));
      response = getResponse(post);

      if (response != null) {
        Job job = JobParser.parseJob(response.getEntity().getContent());
        logger.info("Completing execution of command {} using a remote execute service", exec);
        return job;
      } else
        throw new ExecuteException(String.format("Failed to execute the command %s using a remote execute service", exec));

    } catch (MediaPackageException e) {
      throw new ExecuteException("Error serializing the MediaPackage element", e);
    } catch (IllegalStateException e) {
      throw new ExecuteException(e);
    } catch (IOException e) {
      throw new ExecuteException(e);
    } finally {
      closeConnection(response);
    }
  }

  /**
   * @see org.opencastproject.execute.api.ExecuteService#execute(java.lang.String, java.lang.String, org.opencastproject.mediapackage.MediaPackage, java.lang.String, org.opencastproject.mediapackage.MediaPackageElement.Type, float)
   */
  @Override
  public Job execute(String exec, String params, MediaPackage mp, String outFileName, Type type, float load)
          throws ExecuteException {
    HttpPost post = null;
    HttpResponse response = null;

    try {
      String mpStr = MediaPackageParser.getAsXml(mp);
      List<NameValuePair> formStringParams = new ArrayList<NameValuePair>();
      formStringParams.add(new BasicNameValuePair(EXEC_FORM_PARAM, exec));
      formStringParams.add(new BasicNameValuePair(PARAMS_FORM_PARAM, params));
      formStringParams.add(new BasicNameValuePair(LOAD_FORM_PARAM, String.valueOf(load)));
      formStringParams.add(new BasicNameValuePair(INPUT_MP_FORM_PARAM, mpStr));
      if (outFileName != null)
        formStringParams.add(new BasicNameValuePair(OUTPUT_NAME_FORM_PARAMETER, outFileName));
      if (type != null)
        formStringParams.add(new BasicNameValuePair(TYPE_FORM_PARAMETER, type.toString()));

      logger.info("Executing command {} using a remote execute service", exec);

      post = new HttpPost("/" + ExecuteService.ENDPOINT_NAME);
      post.setEntity(new UrlEncodedFormEntity(formStringParams, "UTF-8"));
      response = getResponse(post);

      if (response != null) {
        Job job = JobParser.parseJob(response.getEntity().getContent());
        logger.info("Completing execution of command {} using a remote execute service", exec);
        return job;
      } else {
        logger.error("Failed to execute the command {} using a remote execute service", exec);
        throw new ExecuteException(String.format("Failed to execute the command %s using a remote execute service", exec));
      }
    } catch (IllegalStateException e) {
      throw new ExecuteException(e);
    } catch (IOException e) {
      throw new ExecuteException(e);
    } finally {
      closeConnection(response);
    }
  }

  @Reference
  @Override
  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    super.setTrustedHttpClient(trustedHttpClient);
  }

  @Reference
  @Override
  public void setRemoteServiceManager(ServiceRegistry serviceRegistry) {
    super.setRemoteServiceManager(serviceRegistry);
  }

}
