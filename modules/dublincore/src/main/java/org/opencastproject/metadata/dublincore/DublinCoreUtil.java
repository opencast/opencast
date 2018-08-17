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

import static com.entwinemedia.fn.Equality.eq;
import static com.entwinemedia.fn.Prelude.chuck;
import static com.entwinemedia.fn.Stream.$;

import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.mediapackage.XMLCatalogImpl.CatalogEntry;
import org.opencastproject.util.Checksum;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.ImmutableListWrapper;
import com.entwinemedia.fn.data.Opt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
  public static Opt<DublinCoreCatalog> loadEpisodeDublinCore(final Workspace ws, MediaPackage mp) {
    return loadDublinCore(ws, mp, MediaPackageSupport.Filters.isEpisodeDublinCore.toFn());
  }

  /**
   * Load the series DublinCore catalog contained in a media package.
   *
   * @return the catalog or none if the media package does not contain a series DublinCore
   */
  public static Opt<DublinCoreCatalog> loadSeriesDublinCore(final Workspace ws, MediaPackage mp) {
    return loadDublinCore(ws, mp, MediaPackageSupport.Filters.isSeriesDublinCore.toFn());
  }

  /**
   * Load a DublinCore catalog of a media package identified by predicate <code>p</code>.
   *
   * @return the catalog or none if no media package element matches predicate <code>p</code>.
   */
  public static Opt<DublinCoreCatalog> loadDublinCore(final Workspace ws, MediaPackage mp,
                                                      Fn<MediaPackageElement, Boolean> p) {
    return $(mp.getElements()).filter(p).head().map(new Fn<MediaPackageElement, DublinCoreCatalog>() {
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
    URI uri = mpe.getURI();
    logger.debug("Loading DC catalog from {}", uri);
    try (InputStream in = workspace.read(uri)) {
      return DublinCores.read(in);
    } catch (Exception e) {
      logger.error("Unable to load metadata from catalog '{}'", mpe, e);
      return chuck(e);
    }
  }

  /**
   * Define equality on DublinCoreCatalogs. Two DublinCores are considered equal if they have the same properties and if
   * each property has the same values in the same order.
   * <p>
   * Note: As long as http://opencast.jira.com/browse/MH-8759 is not fixed, the encoding scheme of values is not
   * considered.
   * <p>
   * Implementation Note: DublinCores should not be compared by their string serialization since the ordering of
   * properties is not defined and cannot be guaranteed between serializations.
   */
  public static boolean equals(DublinCoreCatalog a, DublinCoreCatalog b) {
    final Map<EName, List<DublinCoreValue>> av = a.getValues();
    final Map<EName, List<DublinCoreValue>> bv = b.getValues();
    if (av.size() == bv.size()) {
      for (Map.Entry<EName, List<DublinCoreValue>> ave : av.entrySet()) {
        if (!eq(ave.getValue(), bv.get(ave.getKey())))
          return false;
      }
      return true;
    } else {
      return false;
    }
  }

  /** Return a sorted list of all catalog entries. */
  public static List<CatalogEntry> getPropertiesSorted(DublinCoreCatalog dc) {
    final List<EName> properties = new ArrayList<>(dc.getProperties());
    Collections.sort(properties);
    final List<CatalogEntry> entries = new ArrayList<>();
    for (final EName property : properties) {
      Collections.addAll(entries, dc.getValues(property));
    }
    return new ImmutableListWrapper<>(entries);
  }

  /** Calculate an MD5 checksum for a DublinCore catalog. */
  public static Checksum calculateChecksum(DublinCoreCatalog dc) {
    // Use 0 as a word separator. This is safe since none of the UTF-8 code points
    // except \u0000 contains a null byte when converting to a byte array.
    final byte[] sep = new byte[]{0};
    final MessageDigest md =
        // consider all DublinCore properties
        $(getPropertiesSorted(dc))
            .bind(new Fn<CatalogEntry, Stream<String>>() {
              @Override public Stream<String> apply(CatalogEntry entry) {
                // get attributes, sorted and serialized as [name, value, name, value, ...]
                final Stream<String> attributesSorted = $(entry.getAttributes().entrySet())
                    .sort(new Comparator<Entry<EName, String>>() {
                      @Override public int compare(Entry<EName, String> o1, Entry<EName, String> o2) {
                        return o1.getKey().compareTo(o2.getKey());
                      }
                    })
                    .bind(new Fn<Entry<EName, String>, Stream<String>>() {
                      @Override public Stream<String> apply(Entry<EName, String> attribute) {
                        return $(attribute.getKey().toString(), attribute.getValue());
                      }
                    });
                return $(entry.getEName().toString(), entry.getValue()).append(attributesSorted);
              }
            })
            // consider the root tag
            .append(Opt.nul(dc.getRootTag()).map(toString))
            // digest them
            .foldl(mkMd5MessageDigest(), new Fn2<MessageDigest, String, MessageDigest>() {
              @Override public MessageDigest apply(MessageDigest digest, String s) {
                digest.update(s.getBytes(StandardCharsets.UTF_8));
                // add separator byte (see definition above)
                digest.update(sep);
                return digest;
              }
            });
    try {
      return Checksum.create("md5", Checksum.convertToHex(md.digest()));
    } catch (NoSuchAlgorithmException e) {
      return chuck(e);
    }
  }

  private static final Fn<Object, String> toString = new Fn<Object, String>() {
    @Override public String apply(Object o) {
      return o.toString();
    }
  };

  private static MessageDigest mkMd5MessageDigest() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      logger.error("Unable to create md5 message digest");
      return chuck(e);
    }
  }
}
