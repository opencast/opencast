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

import static com.entwinemedia.fn.Stream.$;
import static com.entwinemedia.fn.Stream.empty;

import com.entwinemedia.fn.Stream;
import com.mysema.query.types.EntityPath;

/**
 * Shortcuts to Querydsl entity paths.
 */
public interface EntityPaths {
  QPropertyDto Q_PROPERTY = QPropertyDto.propertyDto;
  QSnapshotDto Q_SNAPSHOT = QSnapshotDto.snapshotDto;
  QAssetDto Q_ASSET = QAssetDto.assetDto;
  Stream<QSnapshotDto> $Q_SNAPSHOT = $(Q_SNAPSHOT);
  Stream<QPropertyDto> $Q_PROPERTY = $(Q_PROPERTY);
  Stream<EntityPath<?>> $NO_ENTITY = empty();
}
