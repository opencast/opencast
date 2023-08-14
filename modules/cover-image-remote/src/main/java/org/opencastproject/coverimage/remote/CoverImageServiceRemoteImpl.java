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

package org.opencastproject.coverimage.remote;

import org.opencastproject.coverimage.CoverImageException;
import org.opencastproject.coverimage.CoverImageService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Remote implementation for {@link CoverImageService}
 */
@Component(
    immediate = true,
    service = CoverImageService.class,
    property = {
        "service.description=Cover Image Remote Service Proxy"
    }
)
public class CoverImageServiceRemoteImpl extends RemoteBase implements CoverImageService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CoverImageServiceRemoteImpl.class);

  /**
   * Default constructor
   */
  public CoverImageServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  @Override
  public Job generateCoverImage(String xml, String xsl, String width, String height, String posterImageUri,
          String targetFlavor) throws CoverImageException {
    HttpPost post = new HttpPost("/generate");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("xml", xml));
      params.add(new BasicNameValuePair("xsl", xsl));
      params.add(new BasicNameValuePair("width", width));
      params.add(new BasicNameValuePair("height", height));
      params.add(new BasicNameValuePair("posterimage", posterImageUri));
      params.add(new BasicNameValuePair("targetflavor", targetFlavor));
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (Exception e) {
      throw new CoverImageException("Unable to assemble a remote cover image request", e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        String content = EntityUtils.toString(response.getEntity(), "UTF-8");
        Job r = JobParser.parseJob(content);
        logger.info("Cover image generation job {} started on a remote composer", r.getId());
        return r;
      }
    } catch (Exception e) {
      throw new CoverImageException("Unable to generate cover image using a remote generation service", e);
    } finally {
      closeConnection(response);
    }
    throw new CoverImageException("Unable to generate cover image using a remote generation service");
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
