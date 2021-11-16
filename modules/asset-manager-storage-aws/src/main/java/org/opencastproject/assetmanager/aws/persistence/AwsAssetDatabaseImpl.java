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

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

  /** OSGi callback. */
  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating AWS S3 archive");
  }

  /** OSGi callback. Closes entity manager factory. */
  @Deactivate
  public void deactivate(ComponentContext cc) {
    emf.close();
  }

  /** OSGi DI */
  @Reference(
      name = "entityManagerFactory",
      target = "(osgi.unit.name=org.opencastproject.assetmanager.aws.persistence)"
  )
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Override
  public AwsAssetMapping storeMapping(StoragePath path, String objectKey, String objectVersion)
          throws AwsAssetDatabaseException {
    AwsAssetMappingDto dto = AwsAssetMappingDto.storeMapping(emf.createEntityManager(), path, objectKey,
            objectVersion);
    if (dto != null) {
      return dto.toAWSArchiveMapping();
    }
    return null;
  }

  @Override
  public void deleteMapping(StoragePath path) throws AwsAssetDatabaseException {
    AwsAssetMappingDto.deleteMappping(emf.createEntityManager(), path);
  }

  @Override
  public AwsAssetMapping findMapping(StoragePath path) throws AwsAssetDatabaseException {
    AwsAssetMappingDto dto = AwsAssetMappingDto.findMapping(emf.createEntityManager(), path);
    if (dto != null) {
      return dto.toAWSArchiveMapping();
    }
    return null;
  }

  @Override
  public List<AwsAssetMapping> findMappingsByKey(String objectKey) throws AwsAssetDatabaseException {
    List<AwsAssetMappingDto> list = AwsAssetMappingDto.findMappingsByKey(emf.createEntityManager(), objectKey);
    List<AwsAssetMapping> resultList = new ArrayList<AwsAssetMapping>();
    for (AwsAssetMappingDto dto : list) {
      resultList.add(dto.toAWSArchiveMapping());
    }
    return resultList;
  }

  @Override
  public List<AwsAssetMapping> findMappingsByMediaPackageAndVersion(StoragePath path)
          throws AwsAssetDatabaseException {
    List<AwsAssetMappingDto> list = AwsAssetMappingDto.findMappingsByMediaPackageAndVersion(
            emf.createEntityManager(), path);
    List<AwsAssetMapping> resultList = new ArrayList<AwsAssetMapping>();
    for (AwsAssetMappingDto dto : list) {
      resultList.add(dto.toAWSArchiveMapping());
    }
    return resultList;
  }

  @Override
  public List<AwsAssetMapping> findAllByMediaPackage(String mpId) throws AwsAssetDatabaseException {
    List<AwsAssetMappingDto> list = AwsAssetMappingDto.findMappingsByMediaPackage(emf.createEntityManager(),
            mpId);
    List<AwsAssetMapping> resultList = new ArrayList<AwsAssetMapping>();
    for (AwsAssetMappingDto dto : list) {
      resultList.add(dto.toAWSArchiveMapping());
    }
    return resultList;
  }

}
