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
package org.opencastproject.assetmanager.impl.persistence;

import org.opencastproject.util.MimeType;

import com.entwinemedia.fn.ProductBuilder;
import com.entwinemedia.fn.Products;
import com.entwinemedia.fn.data.Opt;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/** JPA DTO modeling the asset database table. */
@Entity(name = "Asset")
@Table(name = "oc_assets_asset")
// Maintain own generator to support database migrations from Archive to AssetManager
// The generator's initial value has to be set after the data migration.
// Otherwise duplicate key errors will most likely happen.
@TableGenerator(name = "seq_oc_assets_asset", initialValue = 0, allocationSize = 50)
public class AssetDto {
  private static final ProductBuilder p = Products.E;

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "seq_oc_assets_asset")
  @Column(name = "id")
  private Long id;

  // foreign key referencing SnapshotDto.id
  @Column(name = "snapshot_id", nullable = false)
  private Long snapshotId;

  @Column(name = "mediapackage_element_id", nullable = false, length = 128)
  private String mediaPackageElementId;

  @Column(name = "checksum", nullable = false, length = 64)
  private String checksum;

  @Column(name = "mime_type", nullable = true, length = 64)
  private String mimeType;

  @Column(name = "size", nullable = false)
  private Long size;

  @Column(name = "storage_id", nullable = false)
  private String storageId;

  /**
   * Create a new DTO.
   */
  public static AssetDto mk(String mediaPackageElementId, long snapshotId, String checksum, Opt<MimeType> mimeType, String storeageId, long size) {
    final AssetDto dto = new AssetDto();
    dto.snapshotId = snapshotId;
    dto.mediaPackageElementId = mediaPackageElementId;
    dto.checksum = checksum;
    dto.mimeType = mimeType.isSome() ? mimeType.get().toString() : null;
    dto.storageId = storeageId;
    dto.size = size;
    return dto;
  }

  public Long getId() {
    return id;
  }

  public String getMediaPackageElementId() {
    return mediaPackageElementId;
  }

  public Opt<MimeType> getMimeType() {
    return Conversions.toMimeType(mimeType);
  }

  public Long getSize() {
    return size;
  }

  public String getStorageId() {
    return storageId;
  }

  void setStorageId(String storage) {
    this.storageId = storage;
  }
}

