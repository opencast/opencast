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

package org.opencastproject.assetmanager.aws.s3;

import static java.lang.String.format;

import org.opencastproject.assetmanager.api.storage.AssetStore;
import org.opencastproject.assetmanager.api.storage.AssetStoreException;
import org.opencastproject.assetmanager.api.storage.RemoteAssetStore;
import org.opencastproject.assetmanager.api.storage.StoragePath;
import org.opencastproject.assetmanager.aws.AwsAbstractArchive;
import org.opencastproject.assetmanager.aws.AwsUploadOperationResult;
import org.opencastproject.assetmanager.aws.persistence.AwsAssetDatabase;
import org.opencastproject.assetmanager.aws.persistence.AwsAssetDatabaseException;
import org.opencastproject.assetmanager.aws.persistence.AwsAssetMapping;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.RestoreObjectRequest;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component(
    property = {
    "service.description=Amazon S3 based asset store",
    "store.type=aws-s3"
    },
    immediate = true,
    service = { RemoteAssetStore.class, AwsS3AssetStore.class }
)
public class AwsS3AssetStore extends AwsAbstractArchive implements RemoteAssetStore {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(AwsS3AssetStore.class);

  private static final Tag freezable = new Tag("Freezable", "true");
  private static final Integer RESTORE_MIN_WAIT = 1080000; // 3h
  private static final Integer RESTORE_POLL = 900000; // 15m


  // Service configuration
  public static final String AWS_S3_ENABLED = "org.opencastproject.assetmanager.aws.s3.enabled";
  public static final String AWS_S3_ACCESS_KEY_ID_CONFIG = "org.opencastproject.assetmanager.aws.s3.access.id";
  public static final String AWS_S3_SECRET_ACCESS_KEY_CONFIG = "org.opencastproject.assetmanager.aws.s3.secret.key";
  public static final String AWS_S3_REGION_CONFIG = "org.opencastproject.assetmanager.aws.s3.region";
  public static final String AWS_S3_BUCKET_CONFIG = "org.opencastproject.assetmanager.aws.s3.bucket";
  public static final String AWS_S3_BUCKET_CONFIG_PREFIX = "org.opencastproject.assetmanager.aws.s3.bucket.";
  public static final String AWS_S3_ENDPOINT_CONFIG = "org.opencastproject.assetmanager.aws.s3.endpoint";
  public static final String AWS_S3_PATH_STYLE_CONFIG = "org.opencastproject.assetmanager.aws.s3.path.style";
  public static final String AWS_S3_MAX_CONNECTIONS = "org.opencastproject.assetmanager.aws.s3.max.connections";
  public static final String AWS_S3_CONNECTION_TIMEOUT = "org.opencastproject.assetmanager.aws.s3.connection.timeout";
  public static final String AWS_S3_MAX_RETRIES = "org.opencastproject.assetmanager.aws.s3.max.retries";
  public static final String AWS_GLACIER_RESTORE_DAYS = "org.opencastproject.assetmanager.aws.s3.glacier.restore.days";

  public static final Integer AWS_S3_GLACIER_RESTORE_DAYS_DEFAULT = 2;

  // defaults
  public static final int DEFAULT_MAX_CONNECTIONS = 50;
  public static final int DEFAULT_CONNECTION_TIMEOUT = 10000;
  public static final int DEFAULT_MAX_RETRIES = 100;

  public static final long DOWNLOAD_URL_EXPIRATION_MS = 30 * 60 * 1000; // 30 min

  public static final String DEFAULT_ORG_KEY = "*";

  /** The AWS client and transfer manager */
  private AmazonS3 s3 = null;
  private TransferManager s3TransferManager = null;

  /** The AWS S3 org to bucket name mapping */
  private Map<String, String> orgBucketNameMap = new HashMap<>();

  private String endpoint = null;

  private boolean pathStyle = false;

  /** The Glacier storage class, restore period **/
  private Integer restorePeriod;

  protected boolean bucketCreated = false;

  /** OSGi Di */
  @Override
  @Reference
  public void setWorkspace(Workspace workspace) {
    super.setWorkspace(workspace);
  }

  /** OSGi Di */
  @Override
  @Reference
  public void setDatabase(AwsAssetDatabase db) {
    super.setDatabase(db);
  }

  /**
   * Service activator, called via declarative services configuration.
   *
   * @param cc
   *          the component context
   */
  @Activate
  public void activate(final ComponentContext cc) throws IllegalStateException, ConfigurationException {
    // Get the configuration
    if (cc != null) {
      @SuppressWarnings("rawtypes")
      Dictionary properties = cc.getProperties();

      boolean enabled = Boolean.parseBoolean(StringUtils.trimToEmpty((String) properties.get(AWS_S3_ENABLED)));
      if (!enabled) {
        logger.info("AWS S3 asset store is disabled");
        return;
      }

      // Store type: "aws-s3"
      storeType = StringUtils.trimToEmpty((String) properties.get(AssetStore.STORE_TYPE_PROPERTY));
      if (StringUtils.isEmpty(storeType)) {
        throw new ConfigurationException("Invalid store type value");
      }
      logger.info("{} is: {}", AssetStore.STORE_TYPE_PROPERTY, storeType);

      // AWS S3 default bucket name
      Option<String> defaultBucketNameOpt = OsgiUtil.getOptCfg(cc.getProperties(), AWS_S3_BUCKET_CONFIG);
      if (defaultBucketNameOpt.isSome()) {
        orgBucketNameMap.put(DEFAULT_ORG_KEY, defaultBucketNameOpt.get());
        logger.info("AWS S3 default bucket name is {}", defaultBucketNameOpt.get());
      }

      // AWS S3 org bucket name mapping
      Collections.list(cc.getProperties().keys()).stream()
          .filter(s -> s.startsWith(AWS_S3_BUCKET_CONFIG_PREFIX))
          .forEach(s -> {
            String orgId = s.substring(AWS_S3_BUCKET_CONFIG_PREFIX.length());
            String bucketName = OsgiUtil.getComponentContextProperty(cc, s);
            orgBucketNameMap.put(orgId, bucketName);
          });

      // AWS region
      regionName = getAWSConfigKey(cc, AWS_S3_REGION_CONFIG);
      logger.info("AWS region is {}", regionName);

      endpoint = OsgiUtil.getComponentContextProperty(
          cc, AWS_S3_ENDPOINT_CONFIG, "s3." + regionName + ".amazonaws.com");
      logger.info("AWS endpoint is {}", endpoint);

      pathStyle = BooleanUtils.toBoolean(OsgiUtil.getComponentContextProperty(cc, AWS_S3_PATH_STYLE_CONFIG, "false"));
      logger.info("AWS path style is {}", pathStyle);

      // Glacier storage class restore period
      restorePeriod = OsgiUtil.getOptCfgAsInt(cc.getProperties(), AWS_GLACIER_RESTORE_DAYS)
          .getOrElse(AWS_S3_GLACIER_RESTORE_DAYS_DEFAULT);

      // Explicit credentials are optional.
      AWSCredentialsProvider provider = null;
      Option<String> accessKeyIdOpt = OsgiUtil.getOptCfg(cc.getProperties(), AWS_S3_ACCESS_KEY_ID_CONFIG);
      Option<String> accessKeySecretOpt = OsgiUtil.getOptCfg(cc.getProperties(), AWS_S3_SECRET_ACCESS_KEY_CONFIG);

      // Keys not informed so use default credentials provider chain, which
      // will look at the environment variables, java system props, credential files, and instance
      // profile credentials
      if (accessKeyIdOpt.isNone() && accessKeySecretOpt.isNone()) {
        provider = new DefaultAWSCredentialsProviderChain();
      } else {
        provider = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(accessKeyIdOpt.get(), accessKeySecretOpt.get()));
      }

      // S3 client configuration
      ClientConfiguration clientConfiguration = new ClientConfiguration();

      int maxConnections = OsgiUtil.getOptCfgAsInt(cc.getProperties(), AWS_S3_MAX_CONNECTIONS)
              .getOrElse(DEFAULT_MAX_CONNECTIONS);
      logger.debug("Max Connections: {}", maxConnections);
      clientConfiguration.setMaxConnections(maxConnections);

      int connectionTimeout = OsgiUtil.getOptCfgAsInt(cc.getProperties(), AWS_S3_CONNECTION_TIMEOUT)
              .getOrElse(DEFAULT_CONNECTION_TIMEOUT);
      logger.debug("Connection Output: {}", connectionTimeout);
      clientConfiguration.setConnectionTimeout(connectionTimeout);

      int maxRetries = OsgiUtil.getOptCfgAsInt(cc.getProperties(), AWS_S3_MAX_RETRIES)
              .getOrElse(DEFAULT_MAX_RETRIES);
      logger.debug("Max Retry: {}", maxRetries);
      clientConfiguration.setMaxErrorRetry(maxRetries);

      // Create AWS client.
      s3 = AmazonS3ClientBuilder.standard()
              .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint
              , regionName))
              .withClientConfiguration(clientConfiguration)
              .withPathStyleAccessEnabled(pathStyle)
              .withCredentials(provider)
              .build();

      s3TransferManager = TransferManagerBuilder.standard().withS3Client(s3).build();

      logger.info("AwsS3ArchiveAssetStore activated!");
    }

  }

  /**
   * Creates the AWS S3 bucket if it doesn't exist yet.
   */
  void createAWSBucket() {
    orgBucketNameMap.forEach((org, bucketName) -> {
      // Does bucket exist?
      try {
        s3.listObjects(bucketName);
      } catch (AmazonServiceException e) {
        if (e.getStatusCode() == 404) {
          // Create the bucket
          try {
            s3.createBucket(bucketName);
            // Enable versioning
            BucketVersioningConfiguration configuration = new BucketVersioningConfiguration().withStatus("Enabled");
            SetBucketVersioningConfigurationRequest configRequest = new SetBucketVersioningConfigurationRequest(
                    bucketName, configuration);
            s3.setBucketVersioningConfiguration(configRequest);
            logger.info("AWS S3 ARCHIVE bucket {} created and versioning enabled", bucketName);
          } catch (Exception e2) {
            throw new IllegalStateException(
                "ARCHIVE bucket " + bucketName + " cannot be created: " + e2.getMessage(), e2);
          }
        } else {
          throw new IllegalStateException("ARCHIVE bucket " + bucketName + " exists, but we can't access it: "
                  + e.getMessage(), e);
        }
      }
      // Bucket already existed or was just created
      bucketCreated = true;
    });
  }

  /**
   * Returns the aws s3 object id created by aws
   */
  @Override
  protected AwsUploadOperationResult uploadObject(String orgId, File origin, String objectName,
          Optional<MimeType> mimeType) throws AssetStoreException {
    // Check first if bucket is there.
    if (!bucketCreated) {
      createAWSBucket();
    }

    String bucketName = getBucketName(orgId);

    // Upload file to AWS S3
    // Use TransferManager to take advantage of multipart upload.
    // TransferManager processes all transfers asynchronously, so this call will return immediately.
    logger.info("Uploading {} to archive bucket {}...", objectName, bucketName);

    try {
      Upload upload = s3TransferManager.upload(bucketName, objectName, origin);
      long start = System.currentTimeMillis();
      // Block and wait for the upload to finish
      upload.waitForCompletion();
      logger.info("Upload of {} to archive bucket {} completed in {} seconds",
              new Object[] { objectName, bucketName, (System.currentTimeMillis() - start) / 1000 });
      ObjectMetadata objMetadata = s3.getObjectMetadata(bucketName, objectName);
      logger.trace("Got object metadata for: {}, version is {}", objectName, objMetadata.getVersionId());

      // Tag objects that are suitable for Glacier storage class
      // NOTE: Use of S3TransferManager means that tagging has to be done as a separate request
      if (mimeType.isPresent()) {
        switch (mimeType.get().getType()) {
          case "audio":
          case "image":
          case "video":
            logger.debug("Tagging S3 object {} as Freezable", objectName);
            List<Tag> tags = new ArrayList<>();
            tags.add(freezable);
            s3.setObjectTagging(new SetObjectTaggingRequest(bucketName, objectName, new ObjectTagging(tags)));
            break;
          default:
            break;
        }
      }

      // If bucket versioning is disabled the versionId is null, so return a -1 to indicate no version
      String versionId = objMetadata.getVersionId();
      if (null == versionId) {
        return new AwsUploadOperationResult(objectName, "-1");
      }
      return new AwsUploadOperationResult(objectName, versionId);
    } catch (InterruptedException e) {
      throw new AssetStoreException("Operation interrupted", e);
    } catch (Exception e) {
      throw new AssetStoreException("Upload failed", e);
    }
  }

  /**
   * Return the object key of the asset in S3
   * @param storagePath asset storage path
   */
  public String getAssetObjectKey(StoragePath storagePath) throws AssetStoreException {
    try {
      AwsAssetMapping map = database.findMapping(storagePath);
      return map.getObjectKey();
    } catch (AwsAssetDatabaseException e) {
      throw new AssetStoreException(e);
    }
  }

  /**
   * Return the storage class of the asset in S3
   * @param storagePath asset storage path
   */
  public String getAssetStorageClass(StoragePath storagePath) throws AssetStoreException {
    if (!contains(storagePath)) {
      return "NONE";
    }
    return getObjectStorageClass(storagePath.getOrganizationId(), getAssetObjectKey(storagePath));
  }

  private String getObjectStorageClass(String orgId, String objectName) throws AssetStoreException {
    try {
      String bucketName = getBucketName(orgId);
      String storageClass = s3.getObjectMetadata(bucketName, objectName).getStorageClass();
      return storageClass == null ? StorageClass.Standard.toString() : storageClass;
    } catch (SdkClientException e) {
      throw new AssetStoreException(e);
    }
  }

  /**
   * Change the storage class of the object if possible
   * @param storagePath asset storage path
   * @param storageClassId metadata storage class id
   * @see <a href="https://aws.amazon.com/s3/storage-classes/">The S3 storage class docs</a>
   */
  public String modifyAssetStorageClass(StoragePath storagePath, String storageClassId) throws AssetStoreException {
    try {
      StorageClass storageClass = StorageClass.fromValue(storageClassId);
      AwsAssetMapping map = database.findMapping(storagePath);
      return modifyObjectStorageClass(map.getOrganizationId(), map.getObjectKey(), storageClass).toString();
    } catch (AwsAssetDatabaseException | IllegalArgumentException e) {
      throw new AssetStoreException(e);
    }
  }

  private StorageClass modifyObjectStorageClass(String orgId, String objectName, StorageClass storageClass)
          throws AssetStoreException {
    try {
      StorageClass objectStorageClass = StorageClass.fromValue(getObjectStorageClass(orgId, objectName));
      String bucketName = getBucketName(orgId);

      if (storageClass != objectStorageClass) {
        /* objects can only be retrieved from Glacier not moved */
        if (objectStorageClass == StorageClass.Glacier || objectStorageClass == StorageClass.DeepArchive) {
          boolean isRestoring = isRestoring(orgId, objectName);
          boolean isRestored = null != s3.getObjectMetadata(bucketName, objectName).getRestoreExpirationTime();
          if (!isRestoring && !isRestored) {
            logger.warn("S3 Object {} can not be moved from storage class {} to {} without restoring the object first",
                objectName, objectStorageClass, storageClass);
            return objectStorageClass;
          }
        }

        /* Only put suitable objects in Glacier */
        if (storageClass == StorageClass.Glacier || objectStorageClass == StorageClass.DeepArchive) {
          GetObjectTaggingRequest gotr = new GetObjectTaggingRequest(bucketName, objectName);
          GetObjectTaggingResult objectTaggingRequest = s3.getObjectTagging(gotr);
          if (!objectTaggingRequest.getTagSet().contains(freezable)) {
            logger.info("S3 object {} is not suitable for storage class {}", objectName, storageClass);
            return objectStorageClass;
          }
        }

        CopyObjectRequest copyRequest = new CopyObjectRequest(bucketName, objectName, bucketName, objectName)
                                            .withStorageClass(storageClass);
        s3.copyObject(copyRequest);
        logger.info("S3 object {} moved to storage class {}", objectName, storageClass);
      } else {
        logger.info("S3 object {} already in storage class {}", objectName, storageClass);
      }

      return storageClass;
    } catch (SdkClientException e) {
      throw new AssetStoreException(e);
    }
  }

  /**
   *
   */
  @Override
  protected InputStream getObject(AwsAssetMapping map) {
    String storageClassId = getObjectStorageClass(map.getOrganizationId(), map.getObjectKey());

    if (StorageClass.Glacier.name().equals(storageClassId) || StorageClass.DeepArchive.name().equals(storageClassId)) {
      // restore object and wait until available if necessary
      restoreGlacierObject(map.getOrganizationId(), map.getObjectKey(), restorePeriod, true);
    }

    try {
      // Do not use S3 object stream anymore because the S3 object needs to be closed to release
      // the http connection so create the stream using the object url (signed).
      String bucketName = getBucketName(map.getOrganizationId());
      String objectKey = map.getObjectKey();
      Date expiration = new Date(System.currentTimeMillis() + DOWNLOAD_URL_EXPIRATION_MS);
      GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, objectKey)
              .withMethod(HttpMethod.GET).withExpiration(expiration);
      URL signedUrl = s3.generatePresignedUrl(generatePresignedUrlRequest);
      logger.debug("Returning pre-signed URL stream for '{}': {}", map, signedUrl);
      return signedUrl.openStream();
    } catch (IOException e) {
      throw new AssetStoreException(e);
    }
  }

  public String getAssetRestoreStatusString(StoragePath storagePath) {
    try {
      AwsAssetMapping map = database.findMapping(storagePath);
      String bucketName = getBucketName(map.getOrganizationId());

      Date expirationTime = s3.getObjectMetadata(bucketName, map.getObjectKey()).getRestoreExpirationTime();
      if (expirationTime != null) {
        return format("RESTORED, expires in %s", expirationTime.toString());
      }

      Boolean prevOngoingRestore = s3.getObjectMetadata(bucketName, map.getObjectKey()).getOngoingRestore();
      if (prevOngoingRestore != null && prevOngoingRestore) {
        return "RESTORING";
      }

      return "NONE";
    } catch (AwsAssetDatabaseException | IllegalArgumentException e) {
      throw new AssetStoreException(e);
    }
  }

  /*
   * Restore a frozen asset from deep archive
   * @param storagePath asset storage path
   * @param assetRestorePeriod number of days to restore assest for
   * @see https://aws.amazon.com/s3/storage-classes/
   */
  public void initiateRestoreAsset(StoragePath storagePath, Integer assetRestorePeriod) throws AssetStoreException {
    try {
      AwsAssetMapping map = database.findMapping(storagePath);
      restoreGlacierObject(map.getOrganizationId(), map.getObjectKey(), assetRestorePeriod, false);
    } catch (AwsAssetDatabaseException | IllegalArgumentException e) {
      throw new AssetStoreException(e);
    }
  }

  private boolean isRestoring(String orgId, String objectName) {
    String bucketName = getBucketName(orgId);
    Boolean prevOngoingRestore = s3.getObjectMetadata(bucketName, objectName).getOngoingRestore();
    //FIXME: prevOngoingRestore is null when the object isn't being restored for some reason
    // The javadocs for getOngoingRestore don't say anything about retuning null, and it doesn't make a ton of sense
    // so I'm guessing this is a bug in the library itself that's not present in the version Manchester is using
    if (prevOngoingRestore != null && prevOngoingRestore) {
      logger.info("Object {} is already being restored", objectName);
      return true;
    }
    logger.info("Object {} is not currently being restored", objectName);
    return false;
  }

  private void restoreGlacierObject(String orgId, String objectName, Integer objectRestorePeriod, Boolean wait) {
    String bucketName = getBucketName(orgId);
    boolean newRestore = false;
    if (isRestoring(orgId, objectName)) {
      if (!wait) {
        return;
      }
      logger.info("Waiting for object {}", objectName);
    } else {
      RestoreObjectRequest requestRestore = new RestoreObjectRequest(bucketName, objectName, objectRestorePeriod);
      s3.restoreObjectV2(requestRestore);
      newRestore = true;
    }

    // if the object had already been restored the restore request will just
    // increase the expiration time
    if (s3.getObjectMetadata(bucketName, objectName).getRestoreExpirationTime() == null) {
      logger.info("Restoring object {} from Glacier class storage", objectName);

      // Just initiate restore?
      if (!wait) {
        return;
      }

      // Check the restoration status of the object.
      // Wait min restore time and then poll ofter that
      try {
        if (newRestore) {
          Thread.sleep(RESTORE_MIN_WAIT);
        }

        while (s3.getObjectMetadata(bucketName, objectName).getOngoingRestore()) {
          Thread.sleep(RESTORE_POLL);
        }

        logger.info("Object {} has been restored from Glacier class storage, for {} days", objectName,
                                                                                           objectRestorePeriod);
      } catch (InterruptedException e) {
        logger.error("Object {} has not yet been restored from Glacier class storage", objectName);
      }
    } else {
      logger.info("Object {} has already been restored, further extended by {} days", objectName, objectRestorePeriod);
    }
  }

  private String getBucketName(String orgId) {
    String bucketName = orgBucketNameMap.get(orgId);
    if (bucketName == null) {
      // check if we have a default bucket name
      bucketName = orgBucketNameMap.get(DEFAULT_ORG_KEY);
      if (bucketName == null) {
        throw new ConfigurationException("No bucket configured for organization " + orgId);
      }
    }
    return bucketName;
  }

  /**
  *
  */
  @Override
  protected void deleteObject(AwsAssetMapping map) {
    String bucketName = getBucketName(map.getOrganizationId());
    s3.deleteObject(bucketName, map.getObjectKey());
  }

  public Integer getRestorePeriod() {
    return restorePeriod;
  }

  // For running tests
  void setS3(AmazonS3 s3) {
    this.s3 = s3;
  }

  void setS3TransferManager(TransferManager s3TransferManager) {
    this.s3TransferManager = s3TransferManager;
  }

  void setBucketName(String orgId, String bucketName) {
    orgBucketNameMap.put(orgId, bucketName);
  }
}
