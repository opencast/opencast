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

package org.opencastproject.kernel.bundleinfo;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

public abstract class AbstractBundleInfoDb implements BundleInfoDb {
  protected abstract DBSession getDBSession();

  @Override
  public void store(final BundleInfo info) {
    tx(namedQuery.persist(BundleInfoJpa.create(info)));
  }

  @Override
  public void delete(String host, long bundleId) {
    tx(BundleInfoJpa.delete(host, bundleId));
  }

  @Override
  public void clear(String host) {
    tx(BundleInfoJpa.deleteByHost(host));
  }

  @Override
  public void clearAll() throws BundleInfoDbException {
    tx(BundleInfoJpa.deleteAll);
  }

  @Override
  public List<BundleInfo> getBundles() {
    return tx(BundleInfoJpa.findAll).stream()
        .map(BundleInfoJpa::toBundleInfo)
        .collect(Collectors.toList());
  }

  @Override
  public List<BundleInfo> getBundles(String... prefixes) throws BundleInfoDbException {
    return tx(BundleInfoJpa.findAll(prefixes)).stream()
        .map(BundleInfoJpa::toBundleInfo)
        .collect(Collectors.toList());
  }

  private <A> A tx(Function<EntityManager, A> fn) {
    try {
      return getDBSession().execTx(fn);
    } catch (Exception e) {
      throw new BundleInfoDbException(e);
    }
  }
}
