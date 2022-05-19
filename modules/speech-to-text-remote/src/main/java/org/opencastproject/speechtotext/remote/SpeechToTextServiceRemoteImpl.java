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

package org.opencastproject.speechtotext.remote;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.speechtotext.api.SpeechToTextService;
import org.opencastproject.speechtotext.api.SpeechToTextServiceException;

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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Generates subtitles files from video or audio sources. */
@Component(
    immediate = true,
    service = SpeechToTextService.class,
    property = {
        "service.description=Speech to Text Workflow Operation Handler",
        "workflow.operation=speechtotext"
    }
)
public class SpeechToTextServiceRemoteImpl extends RemoteBase implements SpeechToTextService {

  private static final Logger logger = LoggerFactory.getLogger(SpeechToTextServiceRemoteImpl.class);

  /** Creates a new speech-to-text service instance. */
  public SpeechToTextServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.speechtotext.api.SpeechToTextService#transcribe(URI, String)
   */
  @Override
  public Job transcribe(URI mediaFile, String language) throws SpeechToTextServiceException {

    List<NameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("mediaFilePath", mediaFile.toString()));
    params.add(new BasicNameValuePair("language", language));

    logger.info("Generating subtitle for {}", mediaFile);
    HttpResponse response = null;
    try {
      HttpPost post = new HttpPost("/speechtotext");
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
      response = getResponse(post);
      if (response == null) {
        throw new SpeechToTextServiceException("No response from service");
      }
      Job receipt = JobParser.parseJob(response.getEntity().getContent());
      logger.info("Completed transcription for {}", mediaFile);
      return receipt;
    } catch (IOException e) {
      throw new SpeechToTextServiceException("Failed building service request", e);
    } finally {
      closeConnection(response);
    }
  }

  @Reference
  public void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }

  @Reference
  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }
}
