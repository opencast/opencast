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
package org.opencastproject.mediapackage;

import org.opencastproject.fun.juc.Immutables;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.opencastproject.util.IoSupport.withResource;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.functions.Booleans.not;
import static org.opencastproject.util.data.functions.Options.sequenceOpt;
import static org.opencastproject.util.data.functions.Options.toOption;

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
  enum MergeMode {
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
   *         the target media package directory
   * @param src
   *         the source media package directory
   * @param mode
   *         conflict resolution strategy in case of identical element identifier
   * @throws MediaPackageException
   *         if an error occurs either accessing one of the two media packages or merging them
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
   * Creates a unique filename inside the root folder, based on the parameter <code>filename</code>.
   *
   * @param root
   *         the root folder
   * @param filename
   *         the original filename
   * @return the new and unique filename
   */
  public static File createElementFilename(File root, String filename) {
    String baseName = PathSupport.removeFileExtension(filename);
    String extension = PathSupport.getFileExtension(filename);
    int count = 1;
    StringBuffer name = null;
    File f = new File(root, filename);
    while (f.exists()) {
      name = new StringBuffer(baseName).append("-").append(count).append(".").append(extension);
      f = new File(root, name.toString());
      count++;
    }
    return f;
  }

  /** Immutable modification of a media package. */
  public static MediaPackage modify(MediaPackage mp, Effect<MediaPackage> e) {
    final MediaPackage clone = (MediaPackage) mp.clone();
    e.apply(clone);
    return clone;
  }

  /**
   * Immutable modification of a media package element. Attention: The returned element loses its media package
   * membership (see {@link org.opencastproject.mediapackage.AbstractMediaPackageElement#clone()})
   */
  public static <A extends MediaPackageElement> A modify(A mpe, Effect<A> e) {
    final A clone = (A) mpe.clone();
    e.apply(clone);
    return clone;
  }

  /** Create a copy of the given media package. */
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
      @Override protected void run(MediaPackageElement e) {
        updateElement(mp, e);
      }
    };
  }

  public static void removeElements(List<MediaPackageElement> es, MediaPackage mp) {
    for (MediaPackageElement e : es) {
      mp.remove(e);
    }
  }

  public static Effect<MediaPackage> removeElements(final List<MediaPackageElement> es) {
    return new Effect<MediaPackage>() {
      @Override protected void run(MediaPackage mp) {
        removeElements(es, mp);
      }
    };
  }

  /** Replaces all elements of <code>mp</code> with <code>es</code>. Mutates <code>mp</code>. */
  public static void replaceElements(MediaPackage mp, List<MediaPackageElement> es) {
    for (MediaPackageElement e : mp.getElements())
      mp.remove(e);
    for (MediaPackageElement e : es)
      mp.add(e);
  }

  public static final Function<MediaPackageElement, String> getMediaPackageElementId = new Function<MediaPackageElement, String>() {
    @Override public String apply(MediaPackageElement mediaPackageElement) {
      return mediaPackageElement.getIdentifier();
    }
  };

  public static final Function<MediaPackageElement, Option<String>> getMediaPackageElementReferenceId = new Function<MediaPackageElement, Option<String>>() {
    @Override public Option<String> apply(MediaPackageElement mediaPackageElement) {
      return option(mediaPackageElement.getReference()).map(getReferenceId);
    }
  };

  public static final Function<MediaPackageReference, String> getReferenceId = new Function<MediaPackageReference, String>() {
    @Override public String apply(MediaPackageReference mediaPackageReference) {
      return mediaPackageReference.getIdentifier();
    }
  };

  /** Get the checksum from a media package element. */
  public static final Function<MediaPackageElement, Option<String>> getChecksum = new Function<MediaPackageElement, Option<String>>() {
    @Override public Option<String> apply(MediaPackageElement mpe) {
      return option(mpe.getChecksum().getValue());
    }
  };

  /** Filters and predicates to work with media package element collections. */
  public static final class Filters {
    private Filters() {
    }

    // functions implemented for monadic bind in order to cast types

    public static <A extends MediaPackageElement> Function<MediaPackageElement, List<A>> byType(final Class<A> type) {
      return new Function<MediaPackageElement, List<A>>() {
        @SuppressWarnings("unchecked")
        @Override
        public List<A> apply(MediaPackageElement mpe) {
          return type.isAssignableFrom(mpe.getClass()) ? list((A) mpe) : (List<A>) NIL;
        }
      };
    }

    public static Function<MediaPackageElement, List<MediaPackageElement>> byFlavor(
            final MediaPackageElementFlavor flavor) {
      return new Function<MediaPackageElement, List<MediaPackageElement>>() {
        @SuppressWarnings("unchecked")
        @Override
        public List<MediaPackageElement> apply(MediaPackageElement mpe) {
          // match is commutative
          return flavor.matches(mpe.getFlavor()) ? list(mpe) : Immutables.<MediaPackageElement>nil();
        }
      };
    }

    public static Function<MediaPackageElement, List<MediaPackageElement>> byTags(final List<String> tags) {
      return new Function<MediaPackageElement, List<MediaPackageElement>>() {
        @SuppressWarnings("unchecked")
        @Override
        public List<MediaPackageElement> apply(MediaPackageElement mpe) {
          return mpe.containsTag(tags) ? list(mpe) : Immutables.<MediaPackageElement>nil();
        }
      };
    }

    /** {@link MediaPackageElement#containsTag(java.util.Collection)} as a function. */
    public static Function<MediaPackageElement, Boolean> ofTags(final List<String> tags) {
      return new Function<MediaPackageElement, Boolean>() {
        @Override public Boolean apply(MediaPackageElement mpe) {
          return mpe.containsTag(tags);
        }
      };
    }

    public static <A extends MediaPackageElement> Function<MediaPackageElement, Boolean> ofType(final Class<A> type) {
      return new Function<MediaPackageElement, Boolean>() {
        @Override public Boolean apply(MediaPackageElement mpe) {
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
      @Override public Boolean apply(MediaPackageElement e) {
        return e.getChecksum() != null;
      }
    };

    public static final Function<MediaPackageElement, Boolean> hasNoChecksum = not(hasChecksum);

    /** Filters publications to channel <code>channelId</code>. */
    public static Function<Publication, Boolean> ofChannel(final String channelId) {
      return new Function<Publication, Boolean>() {
        @Override public Boolean apply(Publication p) {
          return p.getChannel().equals(channelId);
        }
      };
    }

    /** Check if mediapackage element has any of the given tags. */
    public static Function<MediaPackageElement, Boolean> hasTagAny(final List<String> tags) {
      return new Function<MediaPackageElement, Boolean>() {
        @Override public Boolean apply(MediaPackageElement mpe) {
          return mpe.containsTag(tags);
        }
      };
    }

    public static Function<MediaPackageElement, Boolean> hasTag(final String tag) {
      return new Function<MediaPackageElement, Boolean>() {
        @Override public Boolean apply(MediaPackageElement mpe) {
          return mpe.containsTag(tag);
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
        @Override public Boolean apply(MediaPackageElement mpe) {
          // match is commutative
          return flavor.matches(mpe.getFlavor());
        }
      };
    }

    /**
     * Return true if the element has a flavor that matches any of the <code>flavors</code>.
     *
     * @see MediaPackageElementFlavor#matches(MediaPackageElementFlavor)
     */
    public static Function<MediaPackageElement, Boolean> matchesFlavorAny(
            final List<MediaPackageElementFlavor> flavors) {
      return new Function<MediaPackageElement, Boolean>() {
        @Override public Boolean apply(MediaPackageElement mpe) {
          for (MediaPackageElementFlavor f : flavors) {
            if (f.matches(mpe.getFlavor())) {
              return true;
            }
          }
          return false;
        }
      };
    }

    public static final Function<MediaPackageElementFlavor, Function<MediaPackageElement, Boolean>> matchesFlavor =
            new Function<MediaPackageElementFlavor, Function<MediaPackageElement, Boolean>>() {
              @Override public Function<MediaPackageElement, Boolean> apply(final MediaPackageElementFlavor flavor) {
                return matchesFlavor(flavor);
              }
            };

    /** {@link MediaPackageElementFlavor#matches(MediaPackageElementFlavor)} as a function. */
    public static Function<MediaPackageElementFlavor, Boolean> matches(final MediaPackageElementFlavor flavor) {
      return new Function<MediaPackageElementFlavor, Boolean>() {
        @Override public Boolean apply(MediaPackageElementFlavor f) {
          return f.matches(flavor);
        }
      };
    }

    public static final Function<MediaPackageElement, Boolean> isEpisodeDublinCore = new Function<MediaPackageElement, Boolean>() {
      @Override public Boolean apply(MediaPackageElement mpe) {
        // match is commutative
        return MediaPackageElements.EPISODE.matches(mpe.getFlavor());
      }
    };

    public static final Function<MediaPackageElement, Boolean> isSeriesDublinCore = new Function<MediaPackageElement, Boolean>() {
      @Override public Boolean apply(MediaPackageElement mpe) {
        // match is commutative
        return MediaPackageElements.SERIES.matches(mpe.getFlavor());
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
    return errors.getOrElse(NIL).size() == 0 ? Option.<List<String>>none() : errors;
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
   * Function to extract the ID of a media package.
   *
   * @deprecated use {@link Fn#getId}
   */
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

    /** Function to extract the ID of a media package. */
    public static final Function<MediaPackage, String> getId = new Function<MediaPackage, String>() {
      @Override public String apply(MediaPackage mp) {
        return mp.getIdentifier().toString();
      }
    };

    public static final Function<MediaPackage, List<MediaPackageElement>> getElements = new Function<MediaPackage, List<MediaPackageElement>>() {
      @Override public List<MediaPackageElement> apply(MediaPackage a) {
        return Immutables.list(a.getElements());
      }
    };

    public static final Function<MediaPackage, List<Track>> getTracks = new Function<MediaPackage, List<Track>>() {
      @Override public List<Track> apply(MediaPackage a) {
        return Immutables.list(a.getTracks());
      }
    };

    public static final Function<MediaPackage, List<Publication>> getPublications = new Function<MediaPackage, List<Publication>>() {
      @Override public List<Publication> apply(MediaPackage a) {
        return Immutables.list(a.getPublications());
      }
    };
  }
}
