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

package org.opencastproject.sox.remote;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.sox.api.SoxException;
import org.opencastproject.sox.api.SoxService;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Component(
    immediate = true,
    service = SoxService.class,
    property = {
        "service.description=SoX Remote Service Proxy"
    }
)
public class SoxServiceRemoteImpl extends RemoteBase implements SoxService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SoxServiceRemoteImpl.class);

  public SoxServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.sox.api.SoxService#analyze(Track)
   */
  @Override
  public Job analyze(Track sourceAudioTrack) throws MediaPackageException, SoxException {
    HttpPost post = new HttpPost("/analyze");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("sourceAudioTrack", MediaPackageElementParser.getAsXml(sourceAudioTrack)));
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (Exception e) {
      throw new SoxException(e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        try {
          Job receipt = JobParser.parseJob(response.getEntity().getContent());
          logger.info("Analyzing audio {} on a remote analysis server", sourceAudioTrack);
          return receipt;
        } catch (Exception e) {
          throw new SoxException("Unable to analyze audio of element '" + sourceAudioTrack
                  + "' using a remote analysis service", e);
        }
      }
    } finally {
      closeConnection(response);
    }
    throw new SoxException("Unable to analyze audio of element '" + sourceAudioTrack
            + "' using a remote analysis service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.sox.api.SoxService#normalize(Track, Float)
   */
  @Override
  public Job normalize(Track sourceAudioTrack, Float targetRmsLevDb) throws MediaPackageException, SoxException {
    HttpPost post = new HttpPost("/normalize");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("sourceAudioTrack", MediaPackageElementParser.getAsXml(sourceAudioTrack)));
      params.add(new BasicNameValuePair("targetRmsLevDb", targetRmsLevDb.toString()));
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (Exception e) {
      throw new SoxException(e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        try {
          Job receipt = JobParser.parseJob(response.getEntity().getContent());
          logger.info("Normalizing audio {} on a remote audio processing server", sourceAudioTrack);
          return receipt;
        } catch (Exception e) {
          throw new SoxException("Unable to normalize audio of element '" + sourceAudioTrack
                  + "' using a remote audio processing service", e);
        }
      }
    } finally {
      closeConnection(response);
    }
    throw new SoxException("Unable to normalize audio of element '" + sourceAudioTrack
            + "' using a remote audio processing service");
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
