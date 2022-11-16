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

package org.opencastproject.assetmanager.aws.persistence;

import org.opencastproject.assetmanager.api.storage.StoragePath;
import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;

@Component(
    property = {
    "service.description=Aws S3 File Archive Persistence"
    },
    immediate = false,
    service = { AwsAssetDatabase.class }
)
public class AwsAssetDatabaseImpl implements AwsAssetDatabase {

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(AwsAssetDatabaseImpl.class);

  public static final String PERSISTENCE_UNIT = "org.opencastproject.assetmanager.aws.persistence";

  /** Factory used to create {@link javax.persistence.EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  /** OSGi callback. */
  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating AWS S3 archive");
    db = dbSessionFactory.createSession(emf);
  }

  /** OSGi callback. Closes entity manager factory. */
  @Deactivate
  public void deactivate(ComponentContext cc) {
    db.close();
  }

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.assetmanager.aws.persistence)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  @Override
  public AwsAssetMapping storeMapping(StoragePath path, String objectKey, String objectVersion)
          throws AwsAssetDatabaseException {
    try {
      AwsAssetMappingDto dto = db.execTx(AwsAssetMappingDto.storeMappingQuery(path, objectKey, objectVersion));
      if (dto != null) {
        return dto.toAWSArchiveMapping();
      }
      return null;
    } catch (Exception e) {
      throw new AwsAssetDatabaseException(String.format("Could not store mapping for path %s", path), e);
    }
  }

  @Override
  public void deleteMapping(StoragePath path) throws AwsAssetDatabaseException {
    try {
      db.execTx(AwsAssetMappingDto.deleteMapppingQuery(path));
    } catch (Exception e) {
      throw new AwsAssetDatabaseException(String.format("Could not delete mapping for path %s", path), e);
    }
  }

  @Override
  public AwsAssetMapping findMapping(StoragePath path) throws AwsAssetDatabaseException {
    try {
      return db.execTx(AwsAssetMappingDto.findMappingQuery(path))
          .map(AwsAssetMappingDto::toAWSArchiveMapping)
          .orElse(null);
    } catch (Exception e) {
      throw new AwsAssetDatabaseException(e);
    }
  }

  @Override
  public List<AwsAssetMapping> findMappingsByKey(String objectKey) throws AwsAssetDatabaseException {
    try {
      return db.execTx(AwsAssetMappingDto.findMappingsByKeyQuery(objectKey)).stream()
          .map(AwsAssetMappingDto::toAWSArchiveMapping)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new AwsAssetDatabaseException(e);
    }
  }

  @Override
  public List<AwsAssetMapping> findMappingsByMediaPackageAndVersion(StoragePath path)
          throws AwsAssetDatabaseException {
    try {
      return db.execTx(AwsAssetMappingDto.findMappingsByMediaPackageAndVersionQuery(path)).stream()
          .map(AwsAssetMappingDto::toAWSArchiveMapping)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new AwsAssetDatabaseException(e);
    }
  }

  @Override
  public List<AwsAssetMapping> findAllByMediaPackage(String mpId) throws AwsAssetDatabaseException {
    try {
      return db.execTx(AwsAssetMappingDto.findMappingsByMediaPackageQuery(mpId)).stream()
          .map(AwsAssetMappingDto::toAWSArchiveMapping)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new AwsAssetDatabaseException(e);
    }
  }
}
