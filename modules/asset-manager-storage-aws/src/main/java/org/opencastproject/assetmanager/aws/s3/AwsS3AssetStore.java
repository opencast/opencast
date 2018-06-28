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

package org.opencastproject.assetmanager.aws.s3;

import org.opencastproject.assetmanager.aws.AwsAbstractArchive;
import org.opencastproject.assetmanager.aws.AwsUploadOperationResult;
import org.opencastproject.assetmanager.aws.persistence.AwsAssetMapping;
import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.assetmanager.impl.storage.AssetStoreException;
import org.opencastproject.assetmanager.impl.storage.RemoteAssetStore;
import org.opencastproject.util.ConfigurationException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;

public class AwsS3AssetStore extends AwsAbstractArchive implements RemoteAssetStore {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(AwsS3AssetStore.class);

  // Service configuration
  public static final String AWS_S3_ACCESS_KEY_ID_CONFIG = "org.opencastproject.assetmanager.aws.s3.access.id";
  public static final String AWS_S3_SECRET_ACCESS_KEY_CONFIG = "org.opencastproject.assetmanager.aws.s3.secret.key";
  public static final String AWS_S3_REGION_CONFIG = "org.opencastproject.assetmanager.aws.s3.region";
  public static final String AWS_S3_BUCKET_CONFIG = "org.opencastproject.assetmanager.aws.s3.bucket";

  /** The AWS client and transfer manager */
  private AmazonS3 s3 = null;
  private TransferManager s3TransferManager = null;

  /** The AWS S3 bucket name */
  private String bucketName = null;

  private boolean bucketCreated = false;

  /**
   * Service activator, called via declarative services configuration.
   *
   * @param cc
   *          the component context
   */
  public void activate(final ComponentContext cc) throws IllegalStateException, IOException, ConfigurationException {
    // Get the configuration
    if (cc != null) {
      @SuppressWarnings("rawtypes")
      Dictionary properties = cc.getProperties();

      // Store type: "aws-s3"
      storeType = StringUtils.trimToEmpty((String) properties.get(AssetStore.STORE_TYPE_PROPERTY));
      if (StringUtils.isEmpty(storeType)) {
        throw new ConfigurationException("Invalid store type value");
      }
      logger.info("{} is: {}", AssetStore.STORE_TYPE_PROPERTY, storeType);

      // AWS S3 bucket name
      bucketName = getAWSConfigKey(cc, AWS_S3_BUCKET_CONFIG);
      logger.info("AWS S3 bucket name is {}", bucketName);

      // AWS region
      regionName = getAWSConfigKey(cc, AWS_S3_REGION_CONFIG);
      logger.info("AWS region is {}", regionName);

      String accessKeyId = getAWSConfigKey(cc, AWS_S3_ACCESS_KEY_ID_CONFIG);
      String accessKeySecret = getAWSConfigKey(cc, AWS_S3_SECRET_ACCESS_KEY_CONFIG);

      // Create AWS client.
      BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, accessKeySecret);
      s3 = AmazonS3ClientBuilder.standard()
              .withRegion(regionName)
              .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
              .build();

      s3TransferManager = new TransferManager(s3);

      logger.info("AwsS3ArchiveAssetStore activated!");
    }

  }

  /**
   * Creates the AWS S3 bucket if it doesn't exist yet.
   */
  private void createAWSBucket() {
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
          throw new IllegalStateException("ARCHIVE bucket " + bucketName + " cannot be created: " + e2.getMessage(), e2);
        }
      } else {
        throw new IllegalStateException("ARCHIVE bucket " + bucketName + " exists, but we can't access it: "
                + e.getMessage(), e);
      }
    }
    // Bucket already existed or was just created
    bucketCreated = true;
  }

  /**
   * Returns the aws s3 object id created by aws
   */
  protected AwsUploadOperationResult uploadObject(File origin, String objectName) throws AssetStoreException {
    // Check first if bucket is there.
    if (!bucketCreated)
      createAWSBucket();

    // Upload file to AWS S3
    // Use TransferManager to take advantage of multipart upload.
    // TransferManager processes all transfers asynchronously, so this call will return immediately.
    logger.info("Uploading {} to archive bucket {}...", objectName, bucketName);
    Upload upload = s3TransferManager.upload(bucketName, objectName, origin);
    long start = System.currentTimeMillis();

    S3Object obj = null;
    try {
      // Block and wait for the upload to finish
      upload.waitForCompletion();
      logger.info("Upload of {} to archive bucket {} completed in {} seconds",
              new Object[] { objectName, bucketName, (System.currentTimeMillis() - start) / 1000 });
      obj = s3.getObject(bucketName, objectName);
      //If bucket versioning is disabled the versionId is null, so return a -1 to indicate no version
      String versionId = obj.getObjectMetadata().getVersionId();
      //FIXME: We need to do better checking this, what if versioning is just suspended?
      if (null == versionId) {
        return new AwsUploadOperationResult(objectName, "-1");
      }
      return new AwsUploadOperationResult(objectName, versionId);
    } catch (InterruptedException e) {
      throw new AssetStoreException("Operation interrupted", e);
    } finally {
      try {
        obj.close();
      } catch (IOException e) {
        //Swallow and ignore
      }
    }
  }

  /**
   *
   */
  protected InputStream getObject(AwsAssetMapping map) {
    S3Object object = s3.getObject(bucketName, map.getObjectKey());
    return object.getObjectContent();
  }

  /**
  *
  */
  protected void deleteObject(AwsAssetMapping map) {
    s3.deleteObject(bucketName, map.getObjectKey());
  }

  // Used by restore service
  public String getBucketName() {
    return this.bucketName;
  }

  // For running tests
  void setS3(AmazonS3 s3) {
    this.s3 = s3;
  }

  void setS3TransferManager(TransferManager s3TransferManager) {
    this.s3TransferManager = s3TransferManager;
  }

  void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

}
