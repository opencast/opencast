/*
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

package org.opencastproject.mediapackage;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.opencastproject.util.IoSupport.withResource;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Utility class used for media package handling. */
public final class MediaPackageSupport {
  /** Disable construction of this utility class */
  private MediaPackageSupport() {
  }

  private static final List NIL = java.util.Collections.EMPTY_LIST;

  /**
   * Mode used when merging media packages.
   * <p>
   * <ul>
   * <li><code>Merge</code> assigns a new identifier in case of conflicts</li>
   * <li><code>Replace</code> replaces elements in the target media package with matching identifier</li>
   * <li><code>Skip</code> skips elements from the source media package with matching identifer</li>
   * <li><code>Fail</code> fail in case of conflicting identifier</li>
   * </ul>
   */
  public enum MergeMode {
    Merge, Replace, Skip, Fail
  }

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(MediaPackageSupport.class.getName());

  /**
   * Merges the contents of media package located at <code>sourceDir</code> into the media package located at
   * <code>targetDir</code>.
   * <p>
   * When choosing to move the media package element into the new place instead of copying them, the source media
   * package folder will be removed afterwards.
   * </p>
   *
   * @param dest
   *          the target media package directory
   * @param src
   *          the source media package directory
   * @param mode
   *          conflict resolution strategy in case of identical element identifier
   * @throws MediaPackageException
   *           if an error occurs either accessing one of the two media packages or merging them
   */
  public static MediaPackage merge(MediaPackage dest, MediaPackage src, MergeMode mode) throws MediaPackageException {
    try {
      for (MediaPackageElement e : src.elements()) {
        if (dest.getElementById(e.getIdentifier()) == null)
          dest.add(e);
        else {
          if (MergeMode.Replace == mode) {
            logger.debug("Replacing element " + e.getIdentifier() + " while merging " + dest + " with " + src);
            dest.remove(dest.getElementById(e.getIdentifier()));
            dest.add(e);
          } else if (MergeMode.Skip == mode) {
            logger.debug("Skipping element " + e.getIdentifier() + " while merging " + dest + " with " + src);
            continue;
          } else if (MergeMode.Merge == mode) {
            logger.debug("Renaming element " + e.getIdentifier() + " while merging " + dest + " with " + src);
            e.setIdentifier(null);
            dest.add(e);
          } else if (MergeMode.Fail == mode) {
            throw new MediaPackageException("Target media package " + dest + " already contains element with id "
                    + e.getIdentifier());
          }
        }
      }
    } catch (UnsupportedElementException e) {
      throw new MediaPackageException(e);
    }
    return dest;
  }

  /**
   * Returns <code>true</code> if the media package contains an element with the specified identifier.
   *
   * @param identifier
   *          the identifier
   * @return <code>true</code> if the media package contains an element with this identifier
   */
  public static boolean contains(String identifier, MediaPackage mp) {
    for (MediaPackageElement element : mp.getElements()) {
      if (element.getIdentifier().equals(identifier))
        return true;
    }
    return false;
  }

  /**
   * Extract the file name from a media package elements URI.
   *
   * @return the file name or none if it could not be determined
   */
  public static Optional<String> getFileName(MediaPackageElement mpe) {
    URI uri = mpe.getURI();
    if (uri == null) {
      return Optional.empty();
    }
    String name = FilenameUtils.getName(uri.toString());
    if (name == null || name.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(name);
  }

  /**
   * Create a copy of the given media package.
   * <p>
   * ATTENTION: Copying changes the type of the media package elements, e.g. an element of
   * type <code>DublinCoreCatalog</code> will become a <code>CatalogImpl</code>.
   */
  public static MediaPackage copy(MediaPackage mp) {
    return (MediaPackage) mp.clone();
  }

  /** Update a mediapackage element of a mediapackage. Mutates <code>mp</code>. */
  public static void updateElement(MediaPackage mp, MediaPackageElement e) {
    mp.removeElementById(e.getIdentifier());
    mp.add(e);
  }

  /** {@link #updateElement(MediaPackage, MediaPackageElement)} as en effect. */
  public static Consumer<MediaPackageElement> updateElement(final MediaPackage mp) {
    return e -> updateElement(mp, e);
  }

  /** Filters and predicates to work with media package element collections. */
  public static final class Filters {
    private Filters() {
    }

    // functions implemented for monadic bind in order to cast types
    public static final Predicate<MediaPackageElement> isPublication = Publication.class::isInstance;
    public static final Predicate<MediaPackageElement> isNotPublication = isPublication.negate();

    public static final Predicate<MediaPackageElement> hasChecksum = e -> e.getChecksum() != null;

    public static final Predicate<MediaPackageElement> hasNoChecksum = hasChecksum.negate();

    /** Filters publications to channel <code>channelId</code>. */
    public static boolean ofChannel(Publication p, String channelId) {
      return p.getChannel().equals(channelId);
    }

    /** Check if mediapackage element has any of the given tags. */
    public static boolean hasTagAny(MediaPackageElement mpe, List<String> tags) {
      return mpe.containsTag(tags);
    }

    /**
     * Return true if the element has a flavor that matches <code>flavor</code>.
     *
     * @see MediaPackageElementFlavor#matches(MediaPackageElementFlavor)
     */
    public static Function<MediaPackageElement, Boolean> matchesFlavor(final MediaPackageElementFlavor flavor) {
      return mpe -> flavor.matches(mpe.getFlavor());
    }

    public static boolean isEpisodeDublinCore(MediaPackageElement mpe) {
      // match is commutative
      return MediaPackageElements.EPISODE.matches(mpe.getFlavor());
    }

    public static boolean isSmilCatalog(MediaPackageElement mpe) {
      // match is commutative
      return MediaPackageElements.SMIL.matches(mpe.getFlavor());
    }
  }

  /**
   * Basic sanity checking for media packages.
   *
   * <pre>
   * // media package is ok
   * sanityCheck(mp).isNone()
   * </pre>
   *
   * @return none if the media package is a healthy condition, some([error_msgs]) otherwise
   */
  public static Optional<List<String>> sanityCheck(MediaPackage mp) {
    List<String> errors = java.util.stream.Stream.of(
            mp.getIdentifier() == null ? "no ID" : null,
            (mp.getIdentifier() != null && isNotBlank(mp.getIdentifier().toString())) ? null : "blank ID"
        )
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    return errors.isEmpty() ? Optional.empty() : Optional.of(errors);
  }

  /** To be used in unit tests. */
  public static MediaPackage loadFromClassPath(String path) {
    return withResource(
        MediaPackageSupport.class.getResourceAsStream(path),
        (InputStream is) -> {
          try {
            return MediaPackageBuilderFactory.newInstance()
                .newMediaPackageBuilder()
                .loadFromXml(is);
          } catch (Exception e) {
            return chuck(e);
          }
        }
    );
  }
}
