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
package org.opencastproject.publication.engage.remote;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.publication.api.EngagePublicationService;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A remote publication service invoker.
 */
@Component(
    immediate = true,
    service = EngagePublicationService.class,
    property = {
        "service.description=Publication (Engage) Remote Service Proxy"
    }
)
public class EngagePublicationServiceRemoteImpl extends RemoteBase implements EngagePublicationService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(EngagePublicationServiceRemoteImpl.class);

  public EngagePublicationServiceRemoteImpl() {
    super(JOB_TYPE);
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

  @Override
  public Job publish(MediaPackage mediaPackage, String checkAvailability, String strategy,
      String downloadSourceFlavors, String downloadSourceTags, String downloadTargetSubflavor,
      String downloadTargetTags, String streamingSourceFlavors, String streamingSourceTags,
      String streamingTargetSubflavor, String streamingTargetTags,
      String mergeForceFlavors, String addForceFlavors) throws PublicationException {

    InputStream publicationResult = publishInternal("publish", mediaPackage, checkAvailability, strategy,
        downloadSourceFlavors, downloadSourceTags, downloadTargetSubflavor, downloadTargetTags, streamingSourceFlavors,
        streamingSourceTags, streamingTargetSubflavor, streamingTargetTags, mergeForceFlavors, addForceFlavors);
    try {
      return JobParser.parseJob(publicationResult);
    } catch (IOException e) {
      throw new PublicationException(
          "Unable to publish media package " + mediaPackage + " using a remote publication service", e);
    }
  }

  @Override
  public Publication publishSync(MediaPackage mediaPackage, String checkAvailability,
      String strategy, String downloadSourceFlavors, String downloadSourceTags, String downloadTargetSubflavor,
      String downloadTargetTags, String streamingSourceFlavors, String streamingSourceTags,
      String streamingTargetSubflavor, String streamingTargetTags, String mergeForceFlavors, String addForceFlavors)
          throws PublicationException {

    InputStream publicationResult = publishInternal("publishsync", mediaPackage, checkAvailability,
        strategy, downloadSourceFlavors, downloadSourceTags, downloadTargetSubflavor, downloadTargetTags,
        streamingSourceFlavors, streamingSourceTags, streamingTargetSubflavor, streamingTargetTags, mergeForceFlavors,
        addForceFlavors);
    try {
      return (Publication) MediaPackageElementParser.getFromXml(IOUtils.toString(publicationResult, UTF_8));
    } catch (IOException | MediaPackageException e) {
      throw new PublicationException(
          "Unable to publish media package " + mediaPackage + " using a remote publication service", e);
    }
  }

  private InputStream publishInternal(String endpoint, MediaPackage mediaPackage, String checkAvailability,
      String strategy, String downloadSourceFlavors, String downloadSourceTags, String downloadTargetSubflavor,
      String downloadTargetTags, String streamingSourceFlavors, String streamingSourceTags,
      String streamingTargetSubflavor, String streamingTargetTags, String mergeForceFlavors, String addForceFlavors)
          throws PublicationException {

    final List<BasicNameValuePair> params = new ArrayList<>(13);
    params.add(new BasicNameValuePair("mediaPackage", MediaPackageParser.getAsXml(mediaPackage)));
    if (checkAvailability != null) {
      params.add(new BasicNameValuePair("checkAvailability", String.valueOf(checkAvailability)));
    }
    if (strategy != null) {
      params.add(new BasicNameValuePair("strategy", strategy));
    }
    if (downloadSourceFlavors != null) {
      params.add(new BasicNameValuePair("downloadSourceFlavors", downloadSourceFlavors));
    }
    if (downloadSourceTags != null) {
      params.add(new BasicNameValuePair("downloadSourceTags", downloadSourceTags));
    }
    if (downloadTargetSubflavor != null) {
      params.add(new BasicNameValuePair("downloadTargetSubflavor", downloadTargetSubflavor));
    }
    if (downloadTargetTags != null) {
      params.add(new BasicNameValuePair("downloadTargetTags", downloadTargetTags));
    }
    if (streamingSourceFlavors != null) {
      params.add(new BasicNameValuePair("streamingSourceFlavors", streamingSourceFlavors));
    }
    if (streamingSourceTags != null) {
      params.add(new BasicNameValuePair("streamingSourceTags", streamingSourceTags));
    }
    if (streamingTargetSubflavor != null) {
      params.add(new BasicNameValuePair("streamingTargetSubflavor", streamingTargetSubflavor));
    }
    if (streamingTargetTags != null) {
      params.add(new BasicNameValuePair("streamingTargetTags", streamingTargetTags));
    }
    if (mergeForceFlavors != null) {
      params.add(new BasicNameValuePair("mergeForceFlavors", mergeForceFlavors));
    }
    if (addForceFlavors != null) {
      params.add(new BasicNameValuePair("addForceFlavors", addForceFlavors));
    }

    final HttpPost post = new HttpPost(endpoint);
    HttpResponse response = null;
    try {
      post.setEntity(new UrlEncodedFormEntity(params, UTF_8));
      response = getResponse(post);
      if (response != null) {
        logger.info("Publishing media package {} to Engage using a remote publication service", mediaPackage);
        return response.getEntity().getContent();
      }
    } catch (final Exception e) {
      throw new PublicationException(
          "Unable to publish media package " + mediaPackage + " using a remote publication service", e);
    } finally {
      closeConnection(response);
    }
    throw new PublicationException(
        "Unable to publish media package " + mediaPackage + " using a remote publication service.");
  }
}
