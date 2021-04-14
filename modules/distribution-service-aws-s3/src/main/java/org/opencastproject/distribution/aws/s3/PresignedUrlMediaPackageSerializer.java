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
package org.opencastproject.distribution.aws.s3;

import org.opencastproject.distribution.aws.s3.api.AwsS3DistributionService;
import org.opencastproject.mediapackage.MediaPackageSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Implementation of a {@link MediaPackageSerializer} that will support presigned URL feature for a Mediapackage
 */
public class PresignedUrlMediaPackageSerializer implements MediaPackageSerializer {

  private static final Logger logger = LoggerFactory.getLogger(PresignedUrlMediaPackageSerializer.class);

  public static final int RANKING = 10;

    /** S3 distribution service used for generate presigned URL */
  private AwsS3DistributionService service;

  public PresignedUrlMediaPackageSerializer() {
    logger.info("Init PresignedUrlMediaPackageSerializer");
  }

  public void setService(AwsS3DistributionService service) {
    this.service = service;
  }

    /**
     * {@inheritDoc}
     *
     * Generate a presigned URI for the given URI if AwsS3DistributionService is enabled.
     */
  @Override
    public URI decodeURI(URI uri) throws URISyntaxException {
    URI presignedURI = null;
    if (service instanceof AwsS3DistributionServiceImpl) {
      presignedURI = ((AwsS3DistributionServiceImpl)service).presignedURI(uri);
    }
    logger.debug("Decode in presigned URL serializer: {} -> {}", uri, presignedURI);
    return presignedURI;
  }

    /**
     * {@inheritDoc}
     */
  @Override
    public URI encodeURI(URI uri) throws URISyntaxException {
    URI encodedUri = null;
    logger.debug("Encode in presigned URL serializer: {} -> {}", uri, encodedUri);
    return uri;
  }

    /**
     * {@inheritDoc}
     */
  @Override
    public int getRanking() {
    return RANKING;
  }
}
