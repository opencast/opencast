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
package org.opencastproject.assetmanager.impl.query;

import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.VersionField;
import org.opencastproject.assetmanager.impl.RuntimeTypes;
import org.opencastproject.assetmanager.impl.persistence.QSnapshotDto;

import com.mysema.query.jpa.JPASubQuery;

public class VersionFieldImpl extends AbstractSnapshotField<Version, Long> implements VersionField {
  private static final QSnapshotDto Q_SNAPSHOT_ALIAS = new QSnapshotDto("s");

  public VersionFieldImpl() {
    super(Q_SNAPSHOT.version);
  }

  @Override protected Long extract(Version version) {
    return RuntimeTypes.convert(version).value();
  }

  @Override public Predicate isLatest() {
    return mkFirstLatestPredicate(true);
  }

  @Override public Predicate isFirst() {
    return mkFirstLatestPredicate(false);
  }

  /**
   * @param isLatest
   *         true - latest version; false - first version
   */
  private Predicate mkFirstLatestPredicate(boolean isLatest) {
    return mkPredicate(Q_SNAPSHOT.version.eq(
            new JPASubQuery().from(Q_SNAPSHOT_ALIAS)
                    .where(Q_SNAPSHOT_ALIAS.mediaPackageId.eq(Q_SNAPSHOT.mediaPackageId))
                    .unique(isLatest ? Q_SNAPSHOT_ALIAS.version.max() : Q_SNAPSHOT_ALIAS.version.min())));
  }
}
