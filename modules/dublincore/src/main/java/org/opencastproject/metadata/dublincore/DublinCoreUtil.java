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

import static com.entwinemedia.fn.Prelude.chuck;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.XMLCatalogImpl.CatalogEntry;
import org.opencastproject.util.Checksum;
import org.opencastproject.workspace.api.Workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
  public static Optional<DublinCoreCatalog> loadEpisodeDublinCore(final Workspace workspace, MediaPackage mediaPackage) {
    return Arrays.stream(mediaPackage.getCatalogs(MediaPackageElements.EPISODE))
        .findFirst()
        .map(dc -> loadDublinCore(workspace, dc));
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
    return a.getValues().equals(b.getValues());
  }

  /** Return a sorted list of all catalog entries. */
  public static List<CatalogEntry> getPropertiesSorted(DublinCoreCatalog dc) {
    return dc.getProperties().stream()
        .sorted()
        .flatMap(e -> Arrays.stream(dc.getValues(e)))
        .collect(Collectors.toList());
  }

  /** Calculate an MD5 checksum for a DublinCore catalog. */
  public static Checksum calculateChecksum(DublinCoreCatalog dc) {
    // Use 0 as a word separator. This is safe since none of the UTF-8 code points
    // except \u0000 contains a null byte when converting to a byte array.
    final byte[] sep = new byte[]{0};
    var strings = new ArrayList<String>();
    for (var property: getPropertiesSorted(dc)) {
      strings.add(property.getEName().toString());
      strings.add(property.getValue());
      strings.addAll(property.getAttributes().entrySet().stream()
          .sorted(Entry.comparingByKey())
          .flatMap(e -> java.util.stream.Stream.of(e.getKey().toString(), e.getValue()))
          .collect(Collectors.toList()));
    }
    strings.add(Objects.toString(dc.getRootTag(), ""));
    try {
      final MessageDigest digest = MessageDigest.getInstance("MD5");
      for (var s: strings) {
        digest.update(s.getBytes(StandardCharsets.UTF_8));
        // add separator byte (see definition above)
        digest.update(sep);
      }
      return Checksum.create("md5", Checksum.convertToHex(digest.digest()));
    } catch (NoSuchAlgorithmException e) {
      return chuck(e);
    }
  }

}
