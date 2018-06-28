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

package org.opencastproject.assetmanager.aws;

import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;

import org.opencastproject.assetmanager.aws.persistence.AwsAssetDatabase;
import org.opencastproject.assetmanager.aws.persistence.AwsAssetDatabaseException;
import org.opencastproject.assetmanager.aws.persistence.AwsAssetMapping;
import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.assetmanager.impl.storage.AssetStoreException;
import org.opencastproject.assetmanager.impl.storage.DeletionSelector;
import org.opencastproject.assetmanager.impl.storage.Source;
import org.opencastproject.assetmanager.impl.storage.StoragePath;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public abstract class AwsAbstractArchive implements AssetStore {
  /** The default AWS region name */
  public static final String DEFAULT_AWS_REGION = "us-east-1";

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(AwsAbstractArchive.class);

  protected Workspace workspace;
  protected AwsAssetDatabase database;

  /** The store type e.g. aws (long-term), or other implementations */
  protected String storeType = null;
  /** The AWS region */
  protected String regionName = null;

  protected String getAWSConfigKey(ComponentContext cc, String key) {
    try {
      String value = StringUtils.trimToEmpty(OsgiUtil.getComponentContextProperty(cc, key));
      if (StringUtils.isNotBlank(value)) {
        return value;
      }
      throw new ConfigurationException(key + " is invalid");
    } catch (RuntimeException e) {
      throw new ConfigurationException(key + " is missing or invalid", e);
    }
  }

  public Option<Long> getUsedSpace() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public Option<Long> getUsableSpace() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public Option<Long> getTotalSpace() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public String getStoreType() {
    return this.storeType;
  }

  public String getRegion() {
    return this.regionName;
  }

  /** OSGi Di */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /** OSGi Di */
  public void setDatabase(AwsAssetDatabase db) {
    this.database = db;
  }

  /** @see org.opencastproject.assetmanager.impl.storage.AssetStore#copy(StoragePath, StoragePath) */
  public boolean copy(final StoragePath from, final StoragePath to) throws AssetStoreException
  {
    try {
      AwsAssetMapping map = database.findMapping(from);
      if (map == null) {
        logger.warn("Origin file mapping not found in database: {}", from);
        return false;
      }
      // New mapping will point to the SAME AWS object, nothing will be uploaded
      logger.debug(String.format("Adding AWS %s link mapping to database: %s points to %s, version %s", getStoreType(),
              to, map.getObjectKey(), map.getObjectVersion()));
      database.storeMapping(to, map.getObjectKey(), map.getObjectVersion());
      return true;
    } catch (AwsAssetDatabaseException e) {
      throw new AssetStoreException(e);
    }
  }

  public boolean contains(StoragePath path) throws AssetStoreException {
    try {
      AwsAssetMapping map = database.findMapping(path);
      return (map != null);
    } catch (AwsAssetDatabaseException e) {
      throw new AssetStoreException(e);
    }
  }

  protected File getFileFromWorkspace(Source source) {
    try {
      return workspace.get(source.getUri());
    } catch (NotFoundException e) {
      logger.error("Source file '{}' does not exist", source.getUri());
      throw new AssetStoreException(e);
    } catch (IOException e) {
      logger.error("Error while getting file '{}' from workspace: {}", source.getUri(), getMessage(e));
      throw new AssetStoreException(e);
    }
  }

  public String buildObjectName(File origin, StoragePath storagePath) {
    // origin file name from workspace will be called
    // WORKSPACE/http_ADMIN_HOST/episode/archive/mediapackage/MP_ID/EL_ID/VERSION/EL_TYPE.EXTENSION
    // while the actual file is in ARCHIVE/ORG/MP_ID/VERSION/EL_ID.EXTENSION

    // Create object key - S3 style but works for Glacier as well
    String fileExt = FilenameUtils.getExtension(origin.getName());
    return buildFilename(storagePath, fileExt.isEmpty() ? "" : "." + fileExt);
  }

  /**
   * Builds the aws object name.
   */
  protected String buildFilename(StoragePath path, String ext) {
    // Something like ORG_ID/MP_ID/VERSION/ELEMENT_ID.EXTENSION
    return StringUtils.join(new String[] { path.getOrganizationId(), path.getMediaPackageId(),
            path.getVersion().toString(), path.getMediaPackageElementId() + ext }, "/");
  }

  /**
   * @see org.opencastproject.assetmanager.impl.storage.AssetStore#put(StoragePath, Source)
   */
  public void put(StoragePath storagePath, Source source) throws AssetStoreException {
    // If the workspace x archive hack is in place (hard-linking between the two), then this is just a
    // hard-link. If not, this will be a download + hard-link
    final File origin = getFileFromWorkspace(source);

    String objectName = buildObjectName(origin, storagePath);
    String objectVersion = null;
    try {
      // Upload file to AWS
      AwsUploadOperationResult result = uploadObject(origin, objectName);
      objectName = result.getObjectName();
      objectVersion = result.getObjectVersion();
    } catch (Exception e) {
      throw new AssetStoreException(e);
    }

    try {
      // Upload was successful. Store mapping in the database
      logger.debug(String.format("Adding AWS %s mapping to database: %s points to %s, object version %s", getStoreType(),
              storagePath, objectName, objectVersion));
      database.storeMapping(storagePath, objectName, objectVersion);
    } catch (AwsAssetDatabaseException e) {
      throw new AssetStoreException(e);
    }
  }

  protected abstract AwsUploadOperationResult uploadObject(File origin, String objectName) throws AssetStoreException;

  /** @see org.opencastproject.assetmanager.impl.storage.AssetStore#get(StoragePath) */
  public Opt<InputStream> get(final StoragePath path) throws AssetStoreException {
    try {
      AwsAssetMapping map = database.findMapping(path);
      if (map == null) {
        logger.warn("File mapping not found in database: {}", path);
        return Opt.none();
      }

      logger.debug("Getting archive object from AWS {}: {}", getStoreType(), map.getObjectKey());
      return Opt.some(getObject(map));

    } catch (AssetStoreException e) {
      throw e;
    } catch (AwsAssetDatabaseException e) {
      throw new AssetStoreException(e);
    }
  }

  protected abstract InputStream getObject(AwsAssetMapping map) throws AssetStoreException;

  /** @see org.opencastproject.assetmanager.impl.storage.AssetStore#delete(DeletionSelector) */
  public boolean delete(DeletionSelector sel) throws AssetStoreException {
    // Build path, version may be null if all versions are desired
    StoragePath path = new StoragePath(sel.getOrganizationId(), sel.getMediaPackageId(), sel.getVersion().orNull(), null);
    try {
      List<AwsAssetMapping> list = database.findMappingsByMediaPackageAndVersion(path);
      // Traverse all file mappings for that media package / version(s)
      for (AwsAssetMapping map : list) {
        // Find all mappings that point to the same object (like hard-links)
        List<AwsAssetMapping> links = database.findMappingsByKey(map.getObjectKey());
        if (links.size() == 1) {
          // This is the only active mapping thats point to the object; thus, the object can be deleted.
          logger.debug("Deleting archive object from AWS {}: {}, version {}", getStoreType(), map.getObjectKey(), map.getObjectVersion());
          deleteObject(map);
          logger.info("Archive object deleted from AWS {}: {}, version {}", getStoreType(), map.getObjectKey(), map.getObjectVersion());
        }
        // Add a deletion date to the mapping in the table. This doesn't delete the row.
        database.deleteMapping(new StoragePath(map.getOrganizationId(), map.getMediaPackageId(), new VersionImpl(map
                .getVersion()), map.getMediaPackageElementId()));
      }
      return true;
    } catch (AwsAssetDatabaseException e) {
      throw new AssetStoreException(e);
    }
  }

  protected abstract void deleteObject(AwsAssetMapping map) throws AssetStoreException;
}
