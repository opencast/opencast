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

package org.opencastproject.metadata.dublincore;

import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;

/** Utility functions for DublinCores. */
public final class DublinCoreUtil {
  private static final Logger logger = LoggerFactory.getLogger(DublinCoreUtil.class);

  private DublinCoreUtil() {
  }

  /**
   * Load the episode DublinCore catalog contained in a media package.
   *
   * @return the catalog or none if the media package does not contain an episode DublinCore
   */
  public static Option<DublinCoreCatalog> loadEpisodeDublinCore(final Workspace ws, MediaPackage mp) {
    return loadDublinCore(ws, mp, MediaPackageSupport.Filters.isEpisodeDublinCore);
  }

  /**
   * Load the series DublinCore catalog contained in a media package.
   *
   * @return the catalog or none if the media package does not contain a series DublinCore
   */
  public static Option<DublinCoreCatalog> loadSeriesDublinCore(final Workspace ws, MediaPackage mp) {
    return loadDublinCore(ws, mp, MediaPackageSupport.Filters.isSeriesDublinCore);
  }

  /**
   * Load a DublinCore catalog of a media package identified by predicate <code>p</code>.
   *
   * @return the catalog or none if no media package element matches predicate <code>p</code>.
   */
  public static Option<DublinCoreCatalog> loadDublinCore(final Workspace ws, MediaPackage mp,
          Function<MediaPackageElement, Boolean> p) {
    return mlist(mp.getElements()).filter(p).headOpt().map(new Function<MediaPackageElement, DublinCoreCatalog>() {
      @Override
      public DublinCoreCatalog apply(MediaPackageElement mpe) {
        return loadDublinCore(ws, mpe);
      }
    });
  }

  /**
   * Load the DublinCore catalog identified by <code>mpe</code>. Throws an exception if it does not exist or cannot be
   * loaded by any reason.
   *
   * @return the catalog
   */
  public static DublinCoreCatalog loadDublinCore(Workspace workspace, MediaPackageElement mpe) {
    InputStream in = null;
    try {
      in = new FileInputStream(workspace.get(mpe.getURI()));
      return DublinCores.read(in);
    } catch (Exception e) {
      logger.error("Unable to load metadata from catalog '{}': {}", mpe, e);
      return chuck(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Parse an XML string into a DublinCore catalog. Returns none if the xml cannot be parsed into a catalog.
   */
  public static Option<DublinCoreCatalog> fromXml(String xml) {
    InputStream in = null;
    try {
      in = IOUtils.toInputStream(xml, "UTF-8");
      return Option.<DublinCoreCatalog>some(DublinCores.read(in));
    } catch (Exception e) {
      return none();
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /** {@link #fromXml(String)} as a function. */
  public static final Function<String, Option<DublinCoreCatalog>> fromXml = new Function<String, Option<DublinCoreCatalog>>() {
    @Override
    public Option<DublinCoreCatalog> apply(String s) {
      return fromXml(s);
    }
  };
}
