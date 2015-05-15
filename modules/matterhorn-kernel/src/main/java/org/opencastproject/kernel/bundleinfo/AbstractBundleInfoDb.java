/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.kernel.bundleinfo;

import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.persistence.Queries.persist;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.persistence.PersistenceEnv;

import java.util.List;

import javax.persistence.EntityManager;

public abstract class AbstractBundleInfoDb implements BundleInfoDb {
  protected abstract PersistenceEnv getPersistenceEnv();

  @Override
  public void store(final BundleInfo info) {
    tx(persist(BundleInfoJpa.create(info)));
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
    return mlist(tx(BundleInfoJpa.findAll)).map(BundleInfoJpa.toBundleInfo).value();
  }

  @Override
  public List<BundleInfo> getBundles(String... prefixes) throws BundleInfoDbException {
    return mlist(tx(BundleInfoJpa.findAll(prefixes))).map(BundleInfoJpa.toBundleInfo).value();
  }

  private <A> A tx(Function<EntityManager, A> f) {
    return getPersistenceEnv().<A> tx().rethrow(exTransformer).apply(f);
  }

  private static final Function<Exception, BundleInfoDbException> exTransformer = new Function<Exception, BundleInfoDbException>() {
    @Override
    public BundleInfoDbException apply(Exception e) {
      return new BundleInfoDbException(e);
    }
  };
}
