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

import static org.opencastproject.kernel.bundleinfo.BundleInfoImpl.bundleInfo;
import static org.opencastproject.kernel.bundleinfo.BundleInfos.getBuildNumber;
import static org.opencastproject.util.OsgiUtil.getContextProperty;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.systems.MatterhornConstants;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Strings;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log information about bundle build versions. The bundle needs to have the manifest header "Build-Number" set.
 */
public class BundleInfoLogger implements BundleListener {

  private static final Logger logger = LoggerFactory.getLogger(BundleInfoLogger.class);

  // Wrap db into an option.
  // This strategy prevents potential exceptions caused by an already closed connection pool or
  // entity manager in deactivate(). This happens when the logger is deactivated because of a db shutdown.
  //
  // However in a concurrent situation there may still occur exceptions when db methods are called _after_
  // the pool has been closed but _before_ BundleInfoLogger's deactivate method has been called.
  private Option<BundleInfoDb> db;
  private String host;

  /** OSGi DI */
  public void setDb(BundleInfoDb db) {
    this.db = some(db);
  }

  /** OSGi DI */
  public void unsetDb(BundleInfoDb db) {
    this.db = none();
  }

  /** OSGi callback */
  public void activate(ComponentContext cc) {
    host = option(getContextProperty(cc, MatterhornConstants.SERVER_URL_PROPERTY)).bind(Strings.trimToNone).getOrElse(
            UrlSupport.DEFAULT_BASE_URL);
    for (BundleInfoDb a : db)
      a.clear(host);
    cc.getBundleContext().addBundleListener(this);
    for (Bundle b : cc.getBundleContext().getBundles()) {
      logBundle(b);
    }
  }

  /** OSGi callback */
  public void deactivate() {
    for (BundleInfoDb a : db) {
      logger.info("Clearing versions");
      a.clear(host);
    }
  }

  @Override
  public void bundleChanged(BundleEvent event) {
    switch (event.getType()) {
      case BundleEvent.INSTALLED:
        logBundle(event.getBundle());
        break;
      case BundleEvent.STOPPED:
      case BundleEvent.UNINSTALLED:
        for (BundleInfoDb a : db)
          a.delete(host, event.getBundle().getBundleId());
        break;
      default:
        // do nothing
    }
  }

  private void logBundle(final Bundle bundle) {
    final BundleInfo info = bundleInfo(host, bundle.getSymbolicName(), bundle.getBundleId(), bundle.getVersion()
            .toString(), getBuildNumber(bundle));
    final String log = String.format("Bundle %s, id %d, version %s, build number %s", info.getBundleSymbolicName(),
            info.getBundleId(), info.getBundleVersion(), info.getBuildNumber().getOrElse("n/a"));
    logger.info(log);
    for (BundleInfoDb a : db)
      a.store(info);
  }
}
