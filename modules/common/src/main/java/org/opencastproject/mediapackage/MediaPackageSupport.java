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

package org.opencastproject.mediapackage;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.opencastproject.util.IoSupport.withResource;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.functions.Booleans.not;
import static org.opencastproject.util.data.functions.Options.sequenceOpt;
import static org.opencastproject.util.data.functions.Options.toOption;

import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Strings;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;

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
  public static Opt<String> getFileName(MediaPackageElement mpe) {
    final URI uri = mpe.getURI();
    if (uri != null) {
      return Opt.nul(FilenameUtils.getName(uri.toString())).bind(Strings.blankToNone);
    } else {
      return Opt.none();
    }
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
  public static Effect<MediaPackageElement> updateElement(final MediaPackage mp) {
    return new Effect<MediaPackageElement>() {
      @Override
      protected void run(MediaPackageElement e) {
        updateElement(mp, e);
      }
    };
  }

  public static final Function<MediaPackageElement, String> getMediaPackageElementId = new Function<MediaPackageElement, String>() {
    @Override
    public String apply(MediaPackageElement mediaPackageElement) {
      return mediaPackageElement.getIdentifier();
    }
  };

  /** Filters and predicates to work with media package element collections. */
  public static final class Filters {
    private Filters() {
    }

    // functions implemented for monadic bind in order to cast types

    public static <A extends MediaPackageElement> Function<MediaPackageElement, List<A>> byType(final Class<A> type) {
      return new Function<MediaPackageElement, List<A>>() {
        @Override
        public List<A> apply(MediaPackageElement mpe) {
          return type.isAssignableFrom(mpe.getClass()) ? list((A) mpe) : (List<A>) NIL;
        }
      };
    }

    public static Function<MediaPackageElement, List<MediaPackageElement>> byFlavor(
            final MediaPackageElementFlavor flavor) {
      return new Function<MediaPackageElement, List<MediaPackageElement>>() {
        @Override
        public List<MediaPackageElement> apply(MediaPackageElement mpe) {
          // match is commutative
          return flavor.matches(mpe.getFlavor()) ? Collections.singletonList(mpe) : Collections.emptyList();
        }
      };
    }

    public static <A extends MediaPackageElement> Function<MediaPackageElement, Boolean> ofType(final Class<A> type) {
      return new Function<MediaPackageElement, Boolean>() {
        @Override
        public Boolean apply(MediaPackageElement mpe) {
          return type.isAssignableFrom(mpe.getClass());
        }
      };
    }

    public static final Function<MediaPackageElement, List<Publication>> presentations = byType(Publication.class);

    public static final Function<MediaPackageElement, List<Attachment>> attachments = byType(Attachment.class);

    public static final Function<MediaPackageElement, List<Track>> tracks = byType(Track.class);

    public static final Function<MediaPackageElement, List<Catalog>> catalogs = byType(Catalog.class);

    public static final Function<MediaPackageElement, Boolean> isPublication = ofType(Publication.class);

    public static final Function<MediaPackageElement, Boolean> isNotPublication = not(isPublication);

    public static final Function<MediaPackageElement, Boolean> hasChecksum = new Function<MediaPackageElement, Boolean>() {
      @Override
      public Boolean apply(MediaPackageElement e) {
        return e.getChecksum() != null;
      }
    };

    public static final Function<MediaPackageElement, Boolean> hasNoChecksum = not(hasChecksum);

    public static final Function<Track, Boolean> hasVideo = new Function<Track, Boolean>() {
      @Override
      public Boolean apply(Track track) {
        return track.hasVideo();
      }
    };

    public static final Function<Track, Boolean> hasAudio = new Function<Track, Boolean>() {
      @Override
      public Boolean apply(Track track) {
        return track.hasAudio();
      }
    };

    public static final Function<Track, Boolean> hasNoVideo = not(hasVideo);

    public static final Function<Track, Boolean> hasNoAudio = not(hasAudio);

    /** Filters publications to channel <code>channelId</code>. */
    public static Function<Publication, Boolean> ofChannel(final String channelId) {
      return new Function<Publication, Boolean>() {
        @Override
        public Boolean apply(Publication p) {
          return p.getChannel().equals(channelId);
        }
      };
    }

    /** Check if mediapackage element has any of the given tags. */
    public static Function<MediaPackageElement, Boolean> hasTagAny(final List<String> tags) {
      return new Function<MediaPackageElement, Boolean>() {
        @Override
        public Boolean apply(MediaPackageElement mpe) {
          return mpe.containsTag(tags);
        }
      };
    }

    /**
     * Return true if the element has a flavor that matches <code>flavor</code>.
     *
     * @see MediaPackageElementFlavor#matches(MediaPackageElementFlavor)
     */
    public static Function<MediaPackageElement, Boolean> matchesFlavor(final MediaPackageElementFlavor flavor) {
      return new Function<MediaPackageElement, Boolean>() {
        @Override
        public Boolean apply(MediaPackageElement mpe) {
          // match is commutative
          return flavor.matches(mpe.getFlavor());
        }
      };
    }

    public static final Function<MediaPackageElementFlavor, Function<MediaPackageElement, Boolean>> matchesFlavor = new Function<MediaPackageElementFlavor, Function<MediaPackageElement, Boolean>>() {
      @Override
      public Function<MediaPackageElement, Boolean> apply(final MediaPackageElementFlavor flavor) {
        return matchesFlavor(flavor);
      }
    };

    public static final Function<MediaPackageElement, Boolean> isEpisodeAcl = new Function<MediaPackageElement, Boolean>() {
      @Override
      public Boolean apply(MediaPackageElement mpe) {
        // match is commutative
        return MediaPackageElements.XACML_POLICY_EPISODE.matches(mpe.getFlavor());
      }
    };

    public static final Function<MediaPackageElement, Boolean> isEpisodeDublinCore = new Function<MediaPackageElement, Boolean>() {
      @Override
      public Boolean apply(MediaPackageElement mpe) {
        // match is commutative
        return MediaPackageElements.EPISODE.matches(mpe.getFlavor());
      }
    };

    public static final Function<MediaPackageElement, Boolean> isSmilCatalog = new Function<MediaPackageElement, Boolean>() {
      @Override
      public Boolean apply(MediaPackageElement mpe) {
        // match is commutative
        return MediaPackageElements.SMIL.matches(mpe.getFlavor());
      }
    };
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
  public static Option<List<String>> sanityCheck(MediaPackage mp) {
    final Option<List<String>> errors = sequenceOpt(list(toOption(mp.getIdentifier() != null, "no ID"),
            toOption(mp.getIdentifier() != null && isNotBlank(mp.getIdentifier().toString()), "blank ID")));
    return errors.getOrElse(NIL).size() == 0 ? Option.<List<String>> none() : errors;
  }

  /** To be used in unit tests. */
  public static MediaPackage loadFromClassPath(String path) {
    return withResource(MediaPackageSupport.class.getResourceAsStream(path),
            new Function.X<InputStream, MediaPackage>() {
              @Override
              public MediaPackage xapply(InputStream is) throws MediaPackageException {
                return MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(is);
              }
            });
  }

  /**
   * Media package must have a title and contain tracks in order to be published.
   *
   * @param mp
   *          the media package
   * @return <code>true</code> if the media package can be published
   */
  public static boolean isPublishable(MediaPackage mp) {
    return !isBlank(mp.getTitle()) && mp.hasTracks();
  }

  /**
   * Function to extract the ID of a media package.
   */
  @Deprecated
  public static final Function<MediaPackage, String> getId = new Function<MediaPackage, String>() {
    @Override
    public String apply(MediaPackage mp) {
      return mp.getIdentifier().toString();
    }
  };

  /** Functions on media packages. */
  public static final class Fn {
    private Fn() {
    }

  }
}
