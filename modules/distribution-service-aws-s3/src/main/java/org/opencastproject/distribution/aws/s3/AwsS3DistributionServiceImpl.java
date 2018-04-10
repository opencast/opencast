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

import static java.lang.String.format;
import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.distribution.api.AbstractDistributionService;
import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.distribution.aws.s3.api.AwsS3DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.data.Option;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.resources.S3ObjectResource;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteVersionRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

public class AwsS3DistributionServiceImpl extends AbstractDistributionService
        implements AwsS3DistributionService, DistributionService {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AwsS3DistributionServiceImpl.class);

  /** Job type */
  public static final String JOB_TYPE = "org.opencastproject.distribution.aws.s3";

  /** List of available operations on jobs */
  public enum Operation {
    Distribute, Retract, Restore
  };

  // Service configuration
  public static final String AWS_S3_DISTRIBUTION_ENABLE = "org.opencastproject.distribution.aws.s3.distribution.enable";
  public static final String AWS_S3_DISTRIBUTION_BASE_CONFIG = "org.opencastproject.distribution.aws.s3.distribution.base";
  public static final String AWS_S3_ACCESS_KEY_ID_CONFIG = "org.opencastproject.distribution.aws.s3.access.id";
  public static final String AWS_S3_SECRET_ACCESS_KEY_CONFIG = "org.opencastproject.distribution.aws.s3.secret.key";
  public static final String AWS_S3_REGION_CONFIG = "org.opencastproject.distribution.aws.s3.region";
  public static final String AWS_S3_BUCKET_CONFIG = "org.opencastproject.distribution.aws.s3.bucket";
  // config.properties
  public static final String OPENCAST_DOWNLOAD_URL = "org.opencastproject.download.url";

  /** The load on the system introduced by creating a distribute job */
  public static final float DEFAULT_DISTRIBUTE_JOB_LOAD = 0.1f;

  /** The load on the system introduced by creating a retract job */
  public static final float DEFAULT_RETRACT_JOB_LOAD = 0.1f;

  /** The load on the system introduced by creating a restore job */
  public static final float DEFAULT_RESTORE_JOB_LOAD = 0.1f;

  /** The keys to look for in the service configuration file to override the defaults */
  public static final String DISTRIBUTE_JOB_LOAD_KEY = "job.load.aws.s3.distribute";
  public static final String RETRACT_JOB_LOAD_KEY = "job.load.aws.s3.retract";
  public static final String RESTORE_JOB_LOAD_KEY = "job.load.aws.s3.restore";

  /** The load on the system introduced by creating a distribute job */
  private float distributeJobLoad = DEFAULT_DISTRIBUTE_JOB_LOAD;

  /** The load on the system introduced by creating a retract job */
  private float retractJobLoad = DEFAULT_RETRACT_JOB_LOAD;

  /** The load on the system introduced by creating a restore job */
  private float restoreJobLoad = DEFAULT_RESTORE_JOB_LOAD;

  /** Maximum number of tries for checking availability of distributed file */
  private static final int MAX_TRIES = 10;

  /** Interval time in millis to sleep between checks of availability */
  private static final long SLEEP_INTERVAL = 30000L;

  /** The AWS client and transfer manager */
  private AmazonS3 s3 = null;
  private TransferManager s3TransferManager = null;

  /** The AWS S3 bucket name */
  private String bucketName = null;

  /** The opencast download distribution url */
  private String opencastDistributionUrl = null;

  private Gson gson = new Gson();

  /**
   * Creates a new instance of the AWS S3 distribution service.
   */
  public AwsS3DistributionServiceImpl() {
    super(JOB_TYPE);
  }

  private String getAWSConfigKey(ComponentContext cc, String key) {
    try {
      return OsgiUtil.getComponentContextProperty(cc, key);
    } catch (RuntimeException e) {
      throw new ConfigurationException(key + " is missing or invalid", e);
    }
  }

  @Override
  public void activate(ComponentContext cc) {

    // Get the configuration
    if (cc != null) {

      if (!Boolean.valueOf(getAWSConfigKey(cc, AWS_S3_DISTRIBUTION_ENABLE))) {
        logger.info("AWS S3 distribution disabled");
        return;
      }

      // AWS S3 bucket name
      bucketName = getAWSConfigKey(cc, AWS_S3_BUCKET_CONFIG);
      logger.info("AWS S3 bucket name is {}", bucketName);

      // AWS region
      String regionStr = getAWSConfigKey(cc, AWS_S3_REGION_CONFIG);
      logger.info("AWS region is {}", regionStr);

      opencastDistributionUrl = getAWSConfigKey(cc, AWS_S3_DISTRIBUTION_BASE_CONFIG);
      if (!opencastDistributionUrl.endsWith("/")) {
        opencastDistributionUrl = opencastDistributionUrl + "/";
      }
      logger.info("AWS distribution url is {}", opencastDistributionUrl);

      distributeJobLoad = LoadUtil.getConfiguredLoadValue(cc.getProperties(), DISTRIBUTE_JOB_LOAD_KEY,
              DEFAULT_DISTRIBUTE_JOB_LOAD, serviceRegistry);
      retractJobLoad = LoadUtil.getConfiguredLoadValue(cc.getProperties(), RETRACT_JOB_LOAD_KEY,
              DEFAULT_RETRACT_JOB_LOAD, serviceRegistry);
      restoreJobLoad = LoadUtil.getConfiguredLoadValue(cc.getProperties(), RESTORE_JOB_LOAD_KEY,
              DEFAULT_RESTORE_JOB_LOAD, serviceRegistry);

      // Explicit credentials are optional.
      AWSCredentialsProvider provider = null;
      Option<String> accessKeyIdOpt = OsgiUtil.getOptCfg(cc.getProperties(), AWS_S3_ACCESS_KEY_ID_CONFIG);
      Option<String> accessKeySecretOpt = OsgiUtil.getOptCfg(cc.getProperties(), AWS_S3_SECRET_ACCESS_KEY_CONFIG);

      // Keys not informed so use default credentials provider chain, which
      // will look at the environment variables, java system props, credential files, and instance
      // profile credentials
      if (accessKeyIdOpt.isNone() && accessKeySecretOpt.isNone())
        provider = new DefaultAWSCredentialsProviderChain();
      else
        provider = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(accessKeyIdOpt.get(), accessKeySecretOpt.get()));

      // Create AWS client.
      s3 = AmazonS3ClientBuilder.standard().withRegion(regionStr).withCredentials(provider).build();

      s3TransferManager = new TransferManager(s3);

      // Create AWS S3 bucket if not there yet
      createAWSBucket();
      distributionChannel = OsgiUtil.getComponentContextProperty(cc, CONFIG_KEY_STORE_TYPE);

      logger.info("AwsS3DistributionService activated!");
    }
  }

  @Override
  public String getDistributionType() {
    return distributionChannel;
  }

  public void deactivate() {
    // Transfer manager is null if service disabled
    if (s3TransferManager != null)
      s3TransferManager.shutdownNow();

    logger.info("AwsS3DistributionService deactivated!");
  }

  @Override
  public Job distribute(String pubChannelId, MediaPackage mediaPackage, Set<String> downloadIds,
    boolean checkAvailability, boolean preserveReference) throws DistributionException, MediaPackageException {
    throw new UnsupportedOperationException("Not supported yet.");
  //stub function
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.distribution.api.DownloadDistributionService#distribute(String,
   *      org.opencastproject.mediapackage.MediaPackage, String, boolean)
   */
  @Override
  public Job distribute(String channelId, MediaPackage mediaPackage, Set<String> elementIds, boolean checkAvailability)
          throws DistributionException, MediaPackageException {
    notNull(mediaPackage, "mediapackage");
    notNull(elementIds, "elementIds");
    notNull(channelId, "channelId");
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Distribute.toString(), Arrays.asList(channelId,
              MediaPackageParser.getAsXml(mediaPackage), gson.toJson(elementIds), Boolean.toString(checkAvailability)),
              distributeJobLoad);
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.distribution.api.DistributionService#distribute(String,
   *      org.opencastproject.mediapackage.MediaPackage, String)
   */
  @Override
  public Job distribute(String channelId, MediaPackage mediapackage, String elementId)
          throws DistributionException, MediaPackageException {
    return distribute(channelId, mediapackage, elementId, true);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.distribution.api.DownloadDistributionService#distribute(String,
   *      org.opencastproject.mediapackage.MediaPackage, String, boolean)
   */
  @Override
  public Job distribute(String channelId, MediaPackage mediaPackage, String elementId, boolean checkAvailability)
          throws DistributionException, MediaPackageException {
    Set<String> elementIds = new HashSet<>();
    elementIds.add(elementId);
    return distribute(channelId, mediaPackage, elementIds, checkAvailability);
  }

  /**
   * Distribute Mediapackage elements to the download distribution service.
   *
   * @param channelId
   *          # The id of the publication channel to be distributed to.
   * @param mediapackage
   *          The media package that contains the elements to be distributed.
   * @param elementIds
   *          The ids of the elements that should be distributed contained within the media package.
   * @param checkAvailability
   *          Check the availability of the distributed element via http.
   * @return A reference to the MediaPackageElements that have been distributed.
   * @throws DistributionException
   *           Thrown if the parent directory of the MediaPackageElement cannot be created, if the MediaPackageElement
   *           cannot be copied or another unexpected exception occurs.
   */
  public MediaPackageElement[] distributeElements(String channelId, MediaPackage mediapackage, Set<String> elementIds,
          boolean checkAvailability) throws DistributionException {
    notNull(mediapackage, "mediapackage");
    notNull(elementIds, "elementIds");
    notNull(channelId, "channelId");

    final Set<MediaPackageElement> elements = getElements(mediapackage, elementIds);
    List<MediaPackageElement> distributedElements = new ArrayList<>();

    for (MediaPackageElement element : elements) {
      MediaPackageElement distributedElement = distributeElement(channelId, mediapackage, element, checkAvailability);
      distributedElements.add(distributedElement);
    }
    return distributedElements.toArray(new MediaPackageElement[distributedElements.size()]);
  }

  private Set<MediaPackageElement> getElements(MediaPackage mediapackage, Set<String> elementIds)
          throws IllegalStateException {
    final Set<MediaPackageElement> elements = new HashSet<>();
    for (String elementId : elementIds) {
      MediaPackageElement element = mediapackage.getElementById(elementId);
      if (element != null) {
        elements.add(element);
      } else {
        throw new IllegalStateException(
                format("No element %s found in mediapackage %s", elementId, mediapackage.getIdentifier()));
      }
    }
    return elements;
  }

  /**
   * Distribute a media package element to AWS S3.
   *
   * @param mediaPackage
   *          The media package that contains the element to distribute.
   * @param element
   *          The element that should be distributed contained within the media package.
   * @param checkAvailability
   *          Checks if the distributed element is available
   * @return A reference to the MediaPackageElement that has been distributed.
   * @throws DistributionException
   */
  public MediaPackageElement distributeElement(String channelId, final MediaPackage mediaPackage,
          MediaPackageElement element, boolean checkAvailability) throws DistributionException {
    notNull(channelId, "channelId");
    notNull(mediaPackage, "mediapackage");
    notNull(element, "element");

    try {
      File source;
      try {
        source = workspace.get(element.getURI());
      } catch (NotFoundException e) {
        throw new DistributionException("Unable to find " + element.getURI() + " in the workspace", e);
      } catch (IOException e) {
        throw new DistributionException("Error loading " + element.getURI() + " from the workspace", e);
      }

      // Use TransferManager to take advantage of multipart upload.
      // TransferManager processes all transfers asynchronously, so this call will return immediately.
      String objectName = buildObjectName(channelId, mediaPackage.getIdentifier().toString(), element);
      logger.info("Uploading {} to bucket {}...", objectName, bucketName);
      Upload upload = s3TransferManager.upload(bucketName, objectName, source);
      long start = System.currentTimeMillis();

      try {
        // Block and wait for the upload to finish
        upload.waitForCompletion();
        logger.info("Upload of {} to bucket {} completed in {} seconds", objectName, bucketName,
                (System.currentTimeMillis() - start) / 1000);
      } catch (AmazonClientException e) {
        throw new DistributionException("AWS error: " + e.getMessage(), e);
      }

      // Create a representation of the distributed file in the media package
      MediaPackageElement distributedElement = (MediaPackageElement) element.clone();
      try {
        distributedElement.setURI(getDistributionUri(objectName));
      } catch (URISyntaxException e) {
        throw new DistributionException("Distributed element produces an invalid URI", e);
      }

      logger.info("Distributed element {}, object {}", element.getIdentifier(), objectName);

      if (checkAvailability) {
        URI uri = distributedElement.getURI();
        int tries = 0;
        CloseableHttpResponse response = null;
        boolean success = false;
        while (tries < MAX_TRIES) {
          try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            logger.trace("Trying to access {}", uri);
            response = httpClient.execute(new HttpHead(uri));
            if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK) {
              logger.trace("Successfully got {}", uri);
              success = true;
              break; // Exit the loop, response is closed
            } else {
              logger.debug("Http status code when checking distributed element {} is {}", objectName,
                      response.getStatusLine().getStatusCode());
            }
          } catch (Exception e) {
            logger.info("Checking availability of {} threw exception {}. Trying again.", objectName, e.getMessage());
            // Just try again
          } finally {
            if (null != response) {
              response.close();
            }
          }
          tries++;
          logger.trace("Sleeping for {} seconds...", SLEEP_INTERVAL / 1000);
          Thread.sleep(SLEEP_INTERVAL);
        }
        if (!success) {
          logger.warn("Could not check availability of distributed file {}", uri);
          // throw new DistributionException("Unable to load distributed file " + uri.toString());
        }
      }

      return distributedElement;
    } catch (Exception e) {
      logger.warn("Error distributing element " + element.getIdentifier() + " of media package " + mediaPackage, e);
      if (e instanceof DistributionException) {
        throw (DistributionException) e;
      } else {
        throw new DistributionException(e);
      }
    }
  }

  @Override
  public Job retract(String channelId, MediaPackage mediapackage, String elementId) throws DistributionException {
    Set<String> elementIds = new HashSet<>();
    elementIds.add(elementId);
    return retract(channelId, mediapackage, elementIds);
  }

  @Override
  public Job retract(String channelId, MediaPackage mediapackage, Set<String> elementIds) throws DistributionException {
    notNull(mediapackage, "mediapackage");
    notNull(elementIds, "elementIds");
    notNull(channelId, "channelId");
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Retract.toString(),
              Arrays.asList(channelId, MediaPackageParser.getAsXml(mediapackage), gson.toJson(elementIds)),
              retractJobLoad);
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  @Override
  public List<MediaPackageElement> distributeSync(String channelId, MediaPackage mediapackage, Set<String> elementIds,
         boolean checkAvailability) throws DistributionException {
    final MediaPackageElement[] distributedElements =
        distributeElements(channelId, mediapackage, elementIds, checkAvailability);
    if (distributedElements == null) {
      return null;
    }
    return Arrays.asList(distributedElements);
  }

  @Override
  public List<MediaPackageElement> retractSync(String channelId, MediaPackage mediaPackage, Set<String> elementIds)
      throws DistributionException {
    final MediaPackageElement[] retractedElements = retractElements(channelId, mediaPackage, elementIds);
    if (retractedElements == null) {
      return null;
    }
    return Arrays.asList(retractedElements);
  }

  /**
   * Retracts the media package element with the given identifier from the distribution channel.
   *
   * @param channelId
   *          the channel id
   * @param mediaPackage
   *          the media package
   * @param element
   *          the element
   * @return the retracted element or <code>null</code> if the element was not retracted
   */
  protected MediaPackageElement retractElement(String channelId, MediaPackage mediaPackage, MediaPackageElement element)
          throws DistributionException {
    notNull(mediaPackage, "mediaPackage");
    notNull(element, "element");

    try {
      String objectName = getDistributedObjectName(element);
      if (objectName != null) {
        s3.deleteObject(bucketName, objectName);
        logger.info("Retracted element {}, object {}", element.getIdentifier(), objectName);
      }
      return element;
    } catch (AmazonClientException e) {
      throw new DistributionException("AWS error: " + e.getMessage(), e);
    }
  }

  /**
   * Retract a media package element from the distribution channel. The retracted element must not necessarily be the
   * one given as parameter <code>elementId</code>. Instead, the element's distribution URI will be calculated. This way
   * you are able to retract elements by providing the "original" element here.
   *
   * @param channelId
   *          the channel id
   * @param mediapackage
   *          the mediapackage
   * @param elementIds
   *          the element identifiers
   * @return the retracted element or <code>null</code> if the element was not retracted
   * @throws org.opencastproject.distribution.api.DistributionException
   *           in case of an error
   */
  protected MediaPackageElement[] retractElements(String channelId, MediaPackage mediapackage, Set<String> elementIds)
          throws DistributionException {
    notNull(mediapackage, "mediapackage");
    notNull(elementIds, "elementIds");
    notNull(channelId, "channelId");

    Set<MediaPackageElement> elements = getElements(mediapackage, elementIds);
    List<MediaPackageElement> retractedElements = new ArrayList<>();

    for (MediaPackageElement element : elements) {
      MediaPackageElement retractedElement = retractElement(channelId, mediapackage, element);
      retractedElements.add(retractedElement);
    }
    return retractedElements.toArray(new MediaPackageElement[retractedElements.size()]);
  }

  @Override
  public Job restore(String channelId, MediaPackage mediaPackage, String elementId) throws DistributionException {
    if (mediaPackage == null)
      throw new IllegalArgumentException("Media package must be specified");
    if (elementId == null)
      throw new IllegalArgumentException("Element ID must be specified");
    if (channelId == null)
      throw new IllegalArgumentException("Channel ID must be specified");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Restore.toString(),
              Arrays.asList(channelId, MediaPackageParser.getAsXml(mediaPackage), elementId), restoreJobLoad);
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  @Override
  public Job restore(String channelId, MediaPackage mediaPackage, String elementId, String fileName)
          throws DistributionException {
    if (mediaPackage == null)
      throw new IllegalArgumentException("Media package must be specified");
    if (elementId == null)
      throw new IllegalArgumentException("Element ID must be specified");
    if (channelId == null)
      throw new IllegalArgumentException("Channel ID must be specified");
    if (fileName == null)
      throw new IllegalArgumentException("Filename must be specified");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Restore.toString(),
              Arrays.asList(channelId, MediaPackageParser.getAsXml(mediaPackage), elementId, fileName), restoreJobLoad);
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  protected MediaPackageElement restoreElement(String channelId, MediaPackage mediaPackage, String elementId,
          String fileName) throws DistributionException {
    String objectName = null;
    if (StringUtils.isNotBlank(fileName)) {
      objectName = buildObjectName(channelId, mediaPackage.getIdentifier().toString(), elementId, fileName);
    } else {
      objectName = buildObjectName(channelId, mediaPackage.getIdentifier().toString(),
              mediaPackage.getElementById(elementId));
    }
    // Get the latest version of the file
    // Note that this should be the delete marker for the file. We'll check, but if there is more than one delete marker
    // we'll have probs
    ListVersionsRequest lv = new ListVersionsRequest().withBucketName(bucketName).withPrefix(objectName)
            .withMaxResults(1);
    VersionListing listing = s3.listVersions(lv);
    if (listing.getVersionSummaries().size() < 1) {
      throw new DistributionException("Object not found: " + objectName);
    }
    String versionId = listing.getVersionSummaries().get(0).getVersionId();
    // Verify that this is in fact a delete marker
    GetObjectMetadataRequest metadata = new GetObjectMetadataRequest(bucketName, objectName, versionId);
    // Ok, so there's no way of asking AWS directly if the object is deleted in this version of the SDK
    // So instead, we ask for its metadata
    // If it's deleted, then there *isn't* any metadata and we get a 404, which throws the exception
    // This, imo, is an incredibly boneheaded omission from the AWS SDK, and implies we should look for something which
    // sucks less
    // FIXME: This section should be refactored with a simple s3.doesObjectExist(bucketName, objectName) once we update
    // the AWS SDK
    boolean isDeleted = false;
    try {
      s3.getObjectMetadata(metadata);
    } catch (AmazonServiceException e) {
      // Note: This exception is actually a 405, not a 404.
      // This is expected, but very confusing if you're thinking it should be a 'file not found', rather than a 'method
      // not allowed on stuff that's deleted'
      // It's unclear what the expected behaviour is for things which have never existed...
      isDeleted = true;
    }
    if (isDeleted) {
      // Delete the delete marker
      DeleteVersionRequest delete = new DeleteVersionRequest(bucketName, objectName, versionId);
      s3.deleteVersion(delete);
    }
    return mediaPackage.getElementById(elementId);
  }

  /**
   * Builds the aws s3 object name.
   *
   * @param channelId
   * @param mpId
   * @param element
   * @return
   */
  protected String buildObjectName(String channelId, String mpId, MediaPackageElement element) {
    // Something like CHANNEL_ID/MP_ID/ELEMENT_ID/FILE_NAME.EXTENSION
    String uriString = element.getURI().toString();
    String fileName = FilenameUtils.getName(uriString);
    return buildObjectName(channelId, mpId, element.getIdentifier(), fileName);
  }

  /**
   * Builds the aws s3 object name using the raw elementID and filename
   *
   * @param channelId
   * @param mpId
   * @param elementId
   * @param fileName
   * @return
   */
  protected String buildObjectName(String channelId, String mpId, String elementId, String fileName) {
    return StringUtils.join(new String[] { channelId, mpId, elementId, fileName }, "/");
  }

  /**
   * Gets the URI for the element to be distributed.
   *
   * @return The resulting URI after distribution
   * @throws URISyntaxException
   *           if the concrete implementation tries to create a malformed uri
   */
  protected URI getDistributionUri(String objectName) throws URISyntaxException {
    // Something like https://OPENCAST_DOWNLOAD_URL/CHANNEL_ID/MP_ID/ELEMENT_ID/FILE_NAME.EXTENSION
    return new URI(opencastDistributionUrl + objectName);
  }

  /**
   * Gets the distributed object's name.
   *
   * @return The distributed object name
   */
  protected String getDistributedObjectName(MediaPackageElement element) {
    // Something like https://OPENCAST_DOWNLOAD_URL/CHANNEL_ID/MP_ID/ORIGINAL_ELEMENT_ID/FILE_NAME.EXTENSION
    String uriString = element.getURI().toString();

    // String directoryName = distributionDirectory.getAbsolutePath();
    if (uriString.startsWith(opencastDistributionUrl) && uriString.length() > opencastDistributionUrl.length()) {
      return uriString.substring(opencastDistributionUrl.length());
    } else {
      // Cannot retract
      logger.warn(
              "Cannot retract {}. Uri must be in the format https://host/bucketName/channelId/mpId/originalElementId/fileName.extension",
              uriString);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    try {
      op = Operation.valueOf(operation);
      String channelId = arguments.get(0);
      MediaPackage mediaPackage = MediaPackageParser.getFromXml(arguments.get(1));
      Set<String> elementIds = gson.fromJson(arguments.get(2), new TypeToken<Set<String>>() {
      }.getType());
      switch (op) {
        case Distribute:
          Boolean checkAvailability = Boolean.parseBoolean(arguments.get(3));
          MediaPackageElement[] distributedElements = distributeElements(channelId, mediaPackage, elementIds,
                  checkAvailability);
          return (distributedElements != null)
                  ? MediaPackageElementParser.getArrayAsXml(Arrays.asList(distributedElements))
                  : null;
        case Retract:
          MediaPackageElement[] retractedElements = retractElements(channelId, mediaPackage, elementIds);
          return (retractedElements != null) ? MediaPackageElementParser.getArrayAsXml(Arrays.asList(retractedElements))
                  : null;
        /*
         * TODO
         * Commented out due to changes in the way the element IDs are passed (ie, a list rather than individual ones
         * per job). This code is still useful long term, but I don't have time to write the necessary wrapper code
         * around it right now.
         * case Restore:
         * String fileName = arguments.get(3);
         * MediaPackageElement restoredElement = null;
         * if (StringUtils.isNotBlank(fileName)) {
         * restoredElement = restoreElement(channelId, mediaPackage, elementIds, fileName);
         * } else {
         * restoredElement = restoreElement(channelId, mediaPackage, elementIds, null);
         * }
         * return (restoredElement != null) ? MediaPackageElementParser.getAsXml(restoredElement) : null;
         */
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  /**
   * Creates the AWS S3 bucket if it doesn't exist yet.
   */
  protected void createAWSBucket() {
    // Does bucket exist?
    try {
      s3.listObjects(bucketName);
    } catch (AmazonServiceException e) {
      if (e.getStatusCode() == 404) {
        // Create the bucket
        try {
          s3.createBucket(bucketName);
          // Allow public read
          Statement allowPublicReadStatement = new Statement(Effect.Allow).withPrincipals(Principal.AllUsers)
                  .withActions(S3Actions.GetObject).withResources(new S3ObjectResource(bucketName, "*"));
          Policy policy = new Policy().withStatements(allowPublicReadStatement);
          s3.setBucketPolicy(bucketName, policy.toJson());
          logger.info("AWS S3 bucket {} created", bucketName);
        } catch (Exception e2) {
          throw new ConfigurationException("Bucket " + bucketName + " cannot be created: " + e2.getMessage(), e2);
        }
      } else {
        throw new ConfigurationException("Bucket " + bucketName + " exists, but we can't access it: " + e.getMessage(),
                e);
      }
    }
  }

  /** The methods below are used by the test class */

  protected void setS3(AmazonS3 s3) {
    this.s3 = s3;
  }

  protected void setS3TransferManager(TransferManager s3TransferManager) {
    this.s3TransferManager = s3TransferManager;
  }

  protected void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  protected void setOpencastDistributionUrl(String distributionUrl) {
    opencastDistributionUrl = distributionUrl;
  }

}
