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

package org.opencastproject.animate.remote;

import org.opencastproject.animate.api.AnimateService;
import org.opencastproject.animate.api.AnimateServiceException;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.serviceregistry.api.RemoteBase;

import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Create video animations using Synfig */
public class AnimateServiceRemoteImpl extends RemoteBase implements AnimateService {

  private static final Logger logger = LoggerFactory.getLogger(AnimateServiceRemoteImpl.class);

  private static final Gson gson = new Gson();

  /** Creates a new animate service instance. */
  public AnimateServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  @Override
  public Job animate(URI animation, Map<String, String> metadata, List<String> options)
          throws AnimateServiceException {

    // serialize arguments and metadata
    String metadataJson = gson.toJson(metadata);
    String optionJson = gson.toJson(options);

    // Build form parameters
    List<NameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("animation", animation.toString()));
    params.add(new BasicNameValuePair("arguments", optionJson));
    params.add(new BasicNameValuePair("metadata", metadataJson));

    logger.info("Animating {}", animation);
    HttpResponse response = null;
    try {
      HttpPost post = new HttpPost("/animate");
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
      response = getResponse(post);
      if (response == null) {
        throw new AnimateServiceException("No response from service");
      }
      Job receipt = JobParser.parseJob(response.getEntity().getContent());
      logger.info("Completed animating {}", animation);
      return receipt;
    } catch (IOException e) {
      throw new AnimateServiceException("Failed building service request", e);
    } finally {
      closeConnection(response);
    }
  }
}
