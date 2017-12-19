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

import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Order;
import org.opencastproject.assetmanager.api.query.Predicate;

public class ASelectQueryDecorator implements ASelectQuery {
  protected final ASelectQuery delegate;

  public ASelectQueryDecorator(ASelectQuery delegate) {
    this.delegate = delegate;
  }

  @Override public ASelectQuery where(Predicate predicate) {
    return mkDecorator(delegate.where(predicate));
  }

  @Override public ASelectQuery page(int offset, int size) {
    return mkDecorator(delegate.page(offset, size));
  }

  @Override public ASelectQuery orderBy(Order order) {
    return mkDecorator(delegate.orderBy(order));
  }

  @Override public AResult run() {
    return delegate.run();
  }

  protected ASelectQueryDecorator mkDecorator(ASelectQuery delegate) {
    return new ASelectQueryDecorator(delegate);
  }
}
