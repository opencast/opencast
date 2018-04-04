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
package org.opencastproject.publication.oaipmh.remote;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.publication.api.OaiPmhPublicationService;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.serviceregistry.api.RemoteBase;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A remote publication service invoker.
 */
public class OaiPmhPublicationServiceRemoteImpl extends RemoteBase implements OaiPmhPublicationService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(OaiPmhPublicationServiceRemoteImpl.class);

  public OaiPmhPublicationServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  @Override
  public Job publish(MediaPackage mediaPackage, String repository, Set<String> downloadIds, Set<String> streamingIds,
          boolean checkAvailability) throws PublicationException, MediaPackageException {
    final String mediapackageXml = MediaPackageParser.getAsXml(mediaPackage);
    final List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediapackage", mediapackageXml));
    params.add(new BasicNameValuePair("channel", repository));
    params.add(new BasicNameValuePair("downloadElementIds", StringUtils.join(downloadIds, ";;")));
    params.add(new BasicNameValuePair("streamingElementIds", StringUtils.join(streamingIds, ";;")));
    params.add(new BasicNameValuePair("checkAvailability", Boolean.toString(checkAvailability)));
    final HttpPost post = new HttpPost();
    HttpResponse response = null;
    try {
      post.setEntity(new UrlEncodedFormEntity(params, UTF_8));
      response = getResponse(post);
      if (response != null) {
        logger.info("Publishing media package {} to OAI-PMH channel {} using a remote publication service",
                mediaPackage, repository);
        try {
          return JobParser.parseJob(response.getEntity().getContent());
        } catch (Exception e) {
          throw new PublicationException(
                  "Unable to publish media package '" + mediaPackage + "' using a remote OAI-PMH publication service",
                  e);
        }
      }
    } catch (Exception e) {
      throw new PublicationException(
              "Unable to publish media package " + mediaPackage + " using a remote OAI-PMH publication service.", e);
    } finally {
      closeConnection(response);
    }
    throw new PublicationException(
            "Unable to publish mediapackage " + mediaPackage + " using a remote OAI-PMH publication service.");
  }

  @Override
  public Job retract(MediaPackage mediaPackage, String repository) throws PublicationException {
    String mediapackageXml = MediaPackageParser.getAsXml(mediaPackage);
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediapackage", mediapackageXml));
    params.add(new BasicNameValuePair("channel", repository));
    HttpPost post = new HttpPost("/retract");
    HttpResponse response = null;
    post.setEntity(new UrlEncodedFormEntity(params, UTF_8));
    try {
      response = getResponse(post);
      Job receipt = null;
      if (response != null) {
        logger.info("Retracting {} from OAI-PMH channel {} using a remote publication service",
                mediaPackage.getIdentifier().compact(), repository);
        try {
          receipt = JobParser.parseJob(response.getEntity().getContent());
          return receipt;
        } catch (Exception e) {
          throw new PublicationException(format(
                  "Unable to retract media package %s from OAI-PMH channel %s using a remote publication service",
                  mediaPackage.getIdentifier().compact(), repository), e);
        }
      }
    } finally {
      closeConnection(response);
    }
    throw new PublicationException(format(
                    "Unable to retract media package %s from OAI-PMH channel %s using a remote publication service",
                    mediaPackage.getIdentifier().compact(), repository));
  }

  @Override
  public Job updateMetadata(MediaPackage mediaPackage, String repository, Set<String> flavors, Set<String> tags,
          boolean checkAvailability) throws PublicationException {
    final String mediapackageXml = MediaPackageParser.getAsXml(mediaPackage);
    final List<BasicNameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("mediapackage", mediapackageXml));
    params.add(new BasicNameValuePair("channel", repository));
    params.add(new BasicNameValuePair("flavors", StringUtils.join(flavors, ";;")));
    params.add(new BasicNameValuePair("tags", StringUtils.join(tags, ";;")));
    params.add(new BasicNameValuePair("checkAvailability", Boolean.toString(checkAvailability)));
    final HttpPost post = new HttpPost("/updateMetadata");
    HttpResponse response = null;
    try {
      post.setEntity(new UrlEncodedFormEntity(params, UTF_8));
      response = getResponse(post);
      if (response != null) {
        logger.info("Update media package {} metadata in OAI-PMH channel {} using a remote publication service",
                mediaPackage.getIdentifier().compact(), repository);
        return JobParser.parseJob(response.getEntity().getContent());
      }
    } catch (Exception e) {
      throw new PublicationException(format(
              "Unable to update media package %s metadata in OAI-PMH repository %s using a remote publication service.",
              mediaPackage.getIdentifier().compact(), repository), e);
    } finally {
      closeConnection(response);
    }
    throw new PublicationException(format(
              "Unable to update media package %s metadata in OAI-PMH repository %s using a remote publication service.",
              mediaPackage.getIdentifier().compact(), repository));
  }
}
