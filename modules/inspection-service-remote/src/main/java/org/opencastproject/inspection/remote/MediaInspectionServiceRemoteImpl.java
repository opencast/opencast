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

package org.opencastproject.inspection.remote;

import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.inspection.api.util.Options;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Proxies a remote media inspection service for use as a JVM-local service.
 */
@Component(
    property = {
        "service.description=Media Inspection Remote Service Proxy"
    },
    immediate = true,
    service = { MediaInspectionService.class }
)
public class MediaInspectionServiceRemoteImpl extends RemoteBase implements MediaInspectionService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceRemoteImpl.class);

  /**
   * Constructs a new remote media inspection service proxy
   */
  public MediaInspectionServiceRemoteImpl() {
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
    super.setRemoteServiceManager(remoteServiceManager);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.inspection.api.MediaInspectionService#inspect(java.net.URI)
   */
  @Override
  public Job inspect(URI uri) throws MediaInspectionException {
    return inspect(uri, Options.NO_OPTION);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.inspection.api.MediaInspectionService#inspect(java.net.URI)
   */
  @Override
  public Job inspect(URI uri, final Map<String, String> options) throws MediaInspectionException {
    assert (options != null);
    List<NameValuePair> params = new ArrayList<NameValuePair>();
    params.add(new BasicNameValuePair("uri", uri.toString()));
    params.add(new BasicNameValuePair("options", Options.toJson(options)));
    String url = "/inspect?" + URLEncodedUtils.format(params, "UTF-8");
    logger.info("Inspecting media file at {} using a remote media inspection service", uri);
    HttpResponse response = null;
    try {
      HttpGet get = new HttpGet(url);
      response = getResponse(get);
      if (response != null) {
        Job job = JobParser.parseJob(response.getEntity().getContent());
        logger.info("Completing inspection of media file at {} using a remote media inspection service", uri);
        return job;
      }
    } catch (Exception e) {
      throw new MediaInspectionException("Unable to inspect " + uri + " using a remote inspection service", e);
    } finally {
      closeConnection(response);
    }
    throw new MediaInspectionException("Unable to inspect " + uri + " using a remote inspection service");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Job enrich(MediaPackageElement original, boolean override) throws MediaInspectionException {
    return enrich(original, override, Options.NO_OPTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Job enrich(MediaPackageElement original, boolean override, final Map<String, String> options)
          throws MediaInspectionException {
    assert (options != null);
    List<NameValuePair> params = new ArrayList<NameValuePair>();
    try {
      params.add(new BasicNameValuePair("mediaPackageElement", MediaPackageElementParser.getAsXml(original)));
      params.add(new BasicNameValuePair("override", new Boolean(override).toString()));
      params.add(new BasicNameValuePair("options", Options.toJson(options)));
    } catch (Exception e) {
      throw new MediaInspectionException(e);
    }
    logger.info("Enriching {} using a remote media inspection service", original);
    HttpResponse response = null;
    try {
      HttpPost post = new HttpPost("/enrich");
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
      response = getResponse(post);
      if (response != null) {
        Job receipt = JobParser.parseJob(response.getEntity().getContent());
        logger.info("Completing inspection of media file at {} using a remote media inspection service",
                original.getURI());
        return receipt;
      }
    } catch (Exception e) {
      throw new MediaInspectionException("Unable to enrich " + original + " using a remote inspection service", e);
    } finally {
      closeConnection(response);
    }
    throw new MediaInspectionException("Unable to enrich " + original + " using a remote inspection service");
  }

}
