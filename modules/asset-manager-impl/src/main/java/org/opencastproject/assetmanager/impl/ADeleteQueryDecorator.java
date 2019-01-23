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

import org.opencastproject.assetmanager.api.DeleteSnapshotHandler;
import org.opencastproject.assetmanager.api.query.ADeleteQuery;
import org.opencastproject.assetmanager.api.query.Predicate;

public class ADeleteQueryDecorator implements ADeleteQuery {
  protected final ADeleteQuery delegate;

  public ADeleteQueryDecorator(ADeleteQuery delegate) {
    this.delegate = delegate;
  }

  @Override public ADeleteQuery where(Predicate predicate) {
    return mkDecorator(delegate.where(predicate));
  }

  @Override public ADeleteQuery name(String queryName) {
    return mkDecorator(delegate.name(queryName));
  }

  @Override
  public ADeleteQuery willRemoveWholeMediaPackage(boolean willRemoveWholeMediaPackage) {
    return mkDecorator(delegate.willRemoveWholeMediaPackage(willRemoveWholeMediaPackage));
  }

  @Override public long run(DeleteSnapshotHandler deleteSnapshotHandler) {
    return delegate.run(deleteSnapshotHandler);
  }

  protected ADeleteQueryDecorator mkDecorator(ADeleteQuery delegate) {
    return new ADeleteQueryDecorator(delegate);
  }
}
