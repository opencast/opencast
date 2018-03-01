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
package org.opencastproject.assetmanager.impl;

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.AssetId;
import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.util.MimeType;

import com.entwinemedia.fn.data.Opt;

import java.io.InputStream;

public class AssetImpl implements Asset {
  private final AssetId id;
  private final InputStream in;
  private final Opt<MimeType> mimeType;
  private final long size;
  private final Availability availability;
  private final String storageId;

  public AssetImpl(
          AssetId id,
          InputStream in,
          Opt<MimeType> mimeType,
          long size,
          String storeId,
          Availability availability) {
    this.id = id;
    this.in = in;
    this.mimeType = mimeType;
    this.size = size;
    this.availability = availability;
    this.storageId = storeId;
  }

  @Override public AssetId getId() {
    return id;
  }

  @Override public InputStream getInputStream() {
    return in;
  }

  @Override public Opt<MimeType> getMimeType() {
    return mimeType;
  }

  @Override public long getSize() {
    return size;
  }

  @Override public Availability getAvailability() {
    return availability;
  }

  @Override public String getStorageId() { return storageId; }
}
