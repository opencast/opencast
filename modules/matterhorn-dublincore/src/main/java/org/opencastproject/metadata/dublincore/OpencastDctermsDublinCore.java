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

import static com.entwinemedia.fn.Stream.$;
import static org.opencastproject.metadata.dublincore.DublinCore.LANGUAGE_ANY;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_AUDIENCE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CONTRIBUTOR;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATED;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATOR;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_DESCRIPTION;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_EXTENT;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IDENTIFIER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_ISSUED;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IS_PART_OF;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LANGUAGE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LICENSE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_PUBLISHER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_RIGHTS_HOLDER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_SOURCE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_SPATIAL;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TEMPORAL;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TITLE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TYPE;

import org.opencastproject.mediapackage.EName;
import org.opencastproject.metadata.dublincore.Temporal.Match;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.Unit;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Strings;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * {@link DublinCoreCatalog} wrapper to deal with DublinCore metadata according to the Opencast schema.
 * <p>
 * <h3>General behaviour</h3>
 * <ul>
 * <li>Set methods that take a string parameter only execute if the string is not blank.
 * <li>Set methods that take a list of strings only execute if the list contains at least one non-blank string.
 * <li>Set methods--if executed--replace the whole property with the given value/s.
 * <li>Update methods only execute if the parameter is some non-blank string. If executed they
 * behave like a set method and replace all exiting entries.
 * <li>Add methods only execute if the parameter is some non-blank string.
 * </ul>
 */
@ParametersAreNonnullByDefault
public abstract class OpencastDctermsDublinCore {
  protected final DublinCoreCatalog dc;

  private OpencastDctermsDublinCore(DublinCoreCatalog dc) {
    this.dc = dc;
  }

  /** Return the wrapped catalog. */
  public DublinCoreCatalog getCatalog() {
    return dc;
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  @Nonnull public List<String> getPublishers() {
    return get(PROPERTY_PUBLISHER);
  }

  public void setPublishers(List<String> publishers) {
    set(PROPERTY_PUBLISHER, publishers);
  }

  public void addPublisher(String publisher) {
    add(PROPERTY_PUBLISHER, publisher);
  }

  public void removePublishers() {
    dc.remove(PROPERTY_PUBLISHER);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  @Nonnull public List<String> getRightsHolders() {
    return get(PROPERTY_RIGHTS_HOLDER);
  }

  public void setRightsHolders(List<String> rightsHolders) {
    set(PROPERTY_RIGHTS_HOLDER, rightsHolders);
  }

  public void addRightsHolder(String rightsHolder) {
    add(PROPERTY_RIGHTS_HOLDER, rightsHolder);
  }

  public void removeRightsHolders() {
    dc.remove(PROPERTY_RIGHTS_HOLDER);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  @Nonnull public Opt<String> getLicense() {
    return getFirst(PROPERTY_LICENSE);
  }

  public void setLicense(String license) {
    set(PROPERTY_LICENSE, license);
  }

  public void removeLicense() {
    dc.remove(PROPERTY_LICENSE);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Get the {@link DublinCore#PROPERTY_IDENTIFIER} property. */
  @Nonnull public Opt<String> getDcIdentifier() {
    return getFirst(PROPERTY_IDENTIFIER);
  }

  /** Set the {@link DublinCore#PROPERTY_IDENTIFIER} property. */
  public void setDcIdentifier(String id) {
    set(PROPERTY_IDENTIFIER, id);
  }

  /** Update the {@link DublinCore#PROPERTY_IDENTIFIER} property. */
  public void updateDcIdentifier(Opt<String> id) {
    update(PROPERTY_IDENTIFIER, id);
  }

  /** Remove the {@link DublinCore#PROPERTY_IDENTIFIER} property. */
  public void removeDcIdentifier() {
    dc.remove(PROPERTY_IDENTIFIER);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Get the {@link DublinCore#PROPERTY_TITLE} property. */
  @Nonnull public Opt<String> getTitle() {
    return getFirst(PROPERTY_TITLE);
  }

  /** Set the {@link DublinCore#PROPERTY_TITLE} property. */
  public void setTitle(String title) {
    set(PROPERTY_TITLE, title);
  }

  /** Update the {@link DublinCore#PROPERTY_TITLE} property. */
  public void updateTitle(Opt<String> title) {
    update(PROPERTY_TITLE, title);
  }

  /** Remove the {@link DublinCore#PROPERTY_TITLE} property. */
  public void removeTitle() {
    dc.remove(PROPERTY_TITLE);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Get the {@link DublinCore#PROPERTY_DESCRIPTION} property. */
  @Nonnull public Opt<String> getDescription() {
    return getFirst(PROPERTY_DESCRIPTION);
  }

  /** Set the {@link DublinCore#PROPERTY_DESCRIPTION} property. */
  public void setDescription(String description) {
    set(PROPERTY_DESCRIPTION, description);
  }

  /** Update the {@link DublinCore#PROPERTY_DESCRIPTION} property. */
  public void updateDescription(Opt<String> description) {
    update(PROPERTY_DESCRIPTION, description);
  }

  /** Remove the {@link DublinCore#PROPERTY_DESCRIPTION} property. */
  public void removeDescription() {
    dc.remove(PROPERTY_DESCRIPTION);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Get all {@link DublinCore#PROPERTY_AUDIENCE} properties. */
  @Nonnull public List<String> getAudiences() {
    return get(PROPERTY_AUDIENCE);
  }

  /** Set multiple {@link DublinCore#PROPERTY_AUDIENCE} properties. */
  public void setAudiences(List<String> audiences) {
    set(PROPERTY_AUDIENCE, audiences);
  }

  /** Set the {@link DublinCore#PROPERTY_AUDIENCE} property. */
  public void setAudience(String audience) {
    set(PROPERTY_AUDIENCE, audience);
  }

  /** Add an {@link DublinCore#PROPERTY_AUDIENCE} property. */
  public void addAudience(String audience) {
    add(PROPERTY_AUDIENCE, audience);
  }

  /** Update the {@link DublinCore#PROPERTY_AUDIENCE} property. */
  public void updateAudience(Opt<String> audience) {
    update(PROPERTY_AUDIENCE, audience);
  }

  /** Remove all {@link DublinCore#PROPERTY_AUDIENCE} properties. */
  public void removeAudiences() {
    dc.remove(PROPERTY_AUDIENCE);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Get the {@link DublinCore#PROPERTY_CREATED} property. */
  @Nonnull public Opt<Temporal> getCreated() {
    return getFirstVal(PROPERTY_CREATED).map(OpencastMetadataCodec.decodeTemporal);
  }

  /** Set the {@link DublinCore#PROPERTY_CREATED} property. The date is encoded with a precision of {@link Precision#Day}. */
  public void setCreated(Date date) {
    // only allow to set a created date, if no start date is set. Otherwise DC created will be changed by changing the
    // start date with setTemporal. Synchronization is not vice versa, as setting DC created to arbitraty dates might
    // have unwanted side effects, like setting the wrong recording time, on imported data, or third-party REST calls.
    if (getTemporal().isNone()) {
      setDate(PROPERTY_CREATED, date, Precision.Day);
    }
  }

  /** Set the {@link DublinCore#PROPERTY_CREATED} property. The date is encoded with a precision of {@link Precision#Day}. */
  public void setCreated(Temporal t) {
    // only allow to set a created date, if no start date is set. Otherwise DC created will be changed by changing the
    // start date with setTemporal. Synchronization is not vice versa, as setting DC created to arbitraty dates might
    // have unwanted side effects, like setting the wrong recording time, on imported data, or third-party REST calls.
    if (getTemporal().isNone()) {
      t.fold(new Match<Unit>() {
        @Override public Unit period(DCMIPeriod period) {
          setCreated(period.getStart());
          return Unit.unit;
        }

        @Override public Unit instant(Date instant) {
          setCreated(instant);
          return Unit.unit;
        }

        @Override public Unit duration(long duration) {
          return Unit.unit;
        }
      });
    }
  }

  /** Remove the {@link DublinCore#PROPERTY_CREATED} property. */
  public void removeCreated() {
    dc.remove(PROPERTY_CREATED);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Get all {@link DublinCore#PROPERTY_CREATOR} properties. */
  @Nonnull public List<String> getCreators() {
    return get(PROPERTY_CREATOR);
  }

  /** Set multiple {@link DublinCore#PROPERTY_CREATOR} properties. */
  public void setCreators(List<String> creators) {
    set(PROPERTY_CREATOR, creators);
  }

  /** Set the {@link DublinCore#PROPERTY_CREATOR} property. */
  public void setCreator(String creator) {
    set(PROPERTY_CREATOR, creator);
  }

  /** Add a {@link DublinCore#PROPERTY_CREATOR} property. */
  public void addCreator(String name) {
    add(PROPERTY_CREATOR, name);
  }

  /** Update the {@link DublinCore#PROPERTY_CREATOR} property. */
  public void updateCreator(Opt<String> name) {
    update(PROPERTY_CREATOR, name);
  }

  /** Remove all {@link DublinCore#PROPERTY_CREATOR} properties. */
  public void removeCreators() {
    dc.remove(PROPERTY_CREATOR);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Get the {@link DublinCore#PROPERTY_EXTENT} property. */
  @Nonnull public Opt<Long> getExtent() {
    return getFirst(PROPERTY_EXTENT).map(OpencastMetadataCodec.decodeDuration);
  }

  /** Set the {@link DublinCore#PROPERTY_EXTENT} property. */
  public void setExtent(Long extent) {
    dc.set(PROPERTY_EXTENT, OpencastMetadataCodec.encodeDuration(extent));
  }

  /** Remove the {@link DublinCore#PROPERTY_EXTENT} property. */
  public void removeExtent() {
    dc.remove(PROPERTY_EXTENT);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Get the {@link DublinCore#PROPERTY_ISSUED} property. */
  @Nonnull public Opt<Date> getIssued() {
    return getFirst(PROPERTY_ISSUED).map(OpencastMetadataCodec.decodeDate);
  }

  /** Set the {@link DublinCore#PROPERTY_ISSUED} property. */
  public void setIssued(Date date) {
    setDate(PROPERTY_ISSUED, date, Precision.Day);
  }

  /** Update the {@link DublinCore#PROPERTY_ISSUED} property. */
  public void updateIssued(Opt<Date> date) {
    updateDate(PROPERTY_ISSUED, date, Precision.Day);
  }

  /** Remove the {@link DublinCore#PROPERTY_ISSUED} property. */
  public void removeIssued() {
    dc.remove(PROPERTY_ISSUED);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Get the {@link DublinCore#PROPERTY_LANGUAGE} property. */
  @Nonnull public Opt<String> getLanguage() {
    return getFirst(PROPERTY_LANGUAGE);
  }

  /**
   * Set the {@link DublinCore#PROPERTY_LANGUAGE} property.
   * A 2- or 3-letter ISO code. 2-letter ISO codes are tried to convert into a 3-letter code.
   * If this is not possible the provided string is used as is.
   */
  public void setLanguage(String lang) {
    if (StringUtils.isNotBlank(lang)) {
      String doLang = lang;
      if (lang.length() == 2) {
        try {
          doLang = new Locale(lang).getISO3Language();
        } catch (MissingResourceException ignore) {
        }
      }
      set(PROPERTY_LANGUAGE, doLang);
    }
  }

  /** Remove the {@link DublinCore#PROPERTY_LANGUAGE} property. */
  public void removeLanguage() {
    dc.remove(PROPERTY_LANGUAGE);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Get the {@link DublinCore#PROPERTY_SPATIAL} property. */
  @Nonnull public Opt<String> getSpatial() {
    return getFirst(PROPERTY_SPATIAL);
  }

  /** Set the {@link DublinCore#PROPERTY_SPATIAL} property. */
  public void setSpatial(String spatial) {
    set(PROPERTY_SPATIAL, spatial);
  }

  /** Update the {@link DublinCore#PROPERTY_SPATIAL} property. */
  public void updateSpatial(Opt<String> spatial) {
    update(PROPERTY_SPATIAL, spatial);
  }

  /** Remove the {@link DublinCore#PROPERTY_SPATIAL} property. */
  public void removeSpatial() {
    dc.remove(PROPERTY_SPATIAL);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Get the {@link DublinCore#PROPERTY_SOURCE} property. */
  @Nonnull public Opt<String> getSource() {
    return getFirst(PROPERTY_SOURCE);
  }

  /** Set the {@link DublinCore#PROPERTY_SOURCE} property. */
  public void setSource(String source) {
    set(PROPERTY_SOURCE, source);
  }

  /** Remove the {@link DublinCore#PROPERTY_SOURCE} property. */
  public void removeSource() {
    dc.remove(PROPERTY_SOURCE);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Get all {@link DublinCore#PROPERTY_CONTRIBUTOR} properties. */
  @Nonnull public List<String> getContributors() {
    return get(PROPERTY_CONTRIBUTOR);
  }

  /** Set multiple {@link DublinCore#PROPERTY_CONTRIBUTOR} properties. */
  public void setContributors(List<String> contributors) {
    set(PROPERTY_CONTRIBUTOR, contributors);
  }

  /** Set the {@link DublinCore#PROPERTY_CONTRIBUTOR} property. */
  public void setContributor(String contributor) {
    set(PROPERTY_CONTRIBUTOR, contributor);
  }

  /** Add a {@link DublinCore#PROPERTY_CONTRIBUTOR} property. */
  public void addContributor(String contributor) {
    add(PROPERTY_CONTRIBUTOR, contributor);
  }

  /** Update the {@link DublinCore#PROPERTY_CONTRIBUTOR} property. */
  public void updateContributor(Opt<String> contributor) {
    update(PROPERTY_CONTRIBUTOR, contributor);
  }

  /** Remove all {@link DublinCore#PROPERTY_CONTRIBUTOR} properties. */
  public void removeContributors() {
    dc.remove(PROPERTY_CONTRIBUTOR);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Get the {@link DublinCore#PROPERTY_TEMPORAL} property. */
  @Nonnull public Opt<Temporal> getTemporal() {
    return getFirstVal(PROPERTY_TEMPORAL).map(OpencastMetadataCodec.decodeTemporal);
  }

  /**
   * Set the {@link DublinCore#PROPERTY_TEMPORAL} property.
   * The dates are encoded with a precision of {@link Precision#Second}.
   */
  public void setTemporal(Date from, Date to) {
    setPeriod(PROPERTY_TEMPORAL, from, to, Precision.Second);

    // make sure that DC created is synchronized with start date, as discussed in MH-12250
    setDate(PROPERTY_CREATED, from, Precision.Day);
  }

  /** Remove the {@link DublinCore#PROPERTY_TEMPORAL} property. */
  public void removeTemporal() {
    dc.remove(PROPERTY_TEMPORAL);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Get the {@link DublinCore#PROPERTY_TYPE} property split into its components. Components are separated by "/". */
  @Nonnull public Opt<Stream<String>> getType() {
    return getFirst(PROPERTY_TYPE).map(Strings.split("/"));
  }

  /** Get the {@link DublinCore#PROPERTY_TYPE} property as a single string. */
  @Nonnull public Opt<String> getTypeCombined() {
    return getFirst(PROPERTY_TYPE);
  }

  /**
   * Set the {@link DublinCore#PROPERTY_TYPE} property from a type and a subtype.
   * Type and subtype are separated by "/".
   */
  public void setType(String type, String subtype) {
    set(PROPERTY_TYPE, type + "/" + subtype);
  }

  /** Set the {@link DublinCore#PROPERTY_TYPE} property from a single string. */
  public void setType(String type) {
    set(PROPERTY_TYPE, type);
  }

  /** Remove the {@link DublinCore#PROPERTY_TYPE} property. */
  public void removeType() {
    dc.remove(PROPERTY_TYPE);
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  public static final class Episode extends OpencastDctermsDublinCore {

    public Episode(DublinCoreCatalog dc) {
      super(dc);
    }

    /** Get the {@link DublinCore#PROPERTY_IS_PART_OF} property. */
    @Nonnull public Opt<String> getIsPartOf() {
      return getFirst(PROPERTY_IS_PART_OF);
    }

    /** Set the {@link DublinCore#PROPERTY_IS_PART_OF} property. */
    public void setIsPartOf(String seriesID) {
      set(PROPERTY_IS_PART_OF, seriesID);
    }

    /** Update the {@link DublinCore#PROPERTY_IS_PART_OF} property. */
    public void updateIsPartOf(Opt<String> seriesID) {
      update(PROPERTY_IS_PART_OF, seriesID);
    }

    /** Remove the {@link DublinCore#PROPERTY_IS_PART_OF} property. */
    public void removeIsPartOf() {
      dc.remove(PROPERTY_IS_PART_OF);
    }

  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  public static final class Series extends OpencastDctermsDublinCore {
    public Series(DublinCoreCatalog dc) {
      super(dc);
    }
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  protected void setDate(EName property, Date date, Precision p) {
    dc.set(property, OpencastMetadataCodec.encodeDate(date, p));
  }

  protected void updateDate(EName property, Opt<Date> date, Precision p) {
    for (Date d : date) {
      setDate(property, d, p);
    }
  }

  /** Encode with {@link Precision#Second}. */
  protected void setPeriod(EName property, Date from, Date to, Precision p) {
    dc.set(property, OpencastMetadataCodec.encodePeriod(from, to, p));
  }

  protected List<String> get(EName property) {
    return dc.get(property, LANGUAGE_ANY);
  }

  /** Like {@link DublinCore#getFirst(EName)} but with the result wrapped in an Opt. */
  protected Opt<String> getFirst(EName property) {
    return Opt.nul(dc.getFirst(property));
  }

  /** Like {@link DublinCore#getFirstVal(EName)} but with the result wrapped in an Opt. */
  protected Opt<DublinCoreValue> getFirstVal(EName property) {
    return Opt.nul(dc.getFirstVal(property));
  }

  protected void set(EName property, String value) {
    if (StringUtils.isNotBlank(value)) {
      dc.set(property, value);
    }
  }

  protected void set(EName property, List<String> values) {
    final List<DublinCoreValue> valuesFiltered = $(values).filter(Strings.isNotBlank).map(mkValue).toList();
    if (!valuesFiltered.isEmpty()) {
      dc.remove(property);
      dc.set(property, valuesFiltered);
    }
  }

  protected void add(EName property, String value) {
    if (StringUtils.isNotBlank(value)) {
      dc.add(property, value);
    }
  }

  protected void update(EName property, Opt<String> value) {
    for (String v : value) {
      set(property, v);
    }
  }

  private final Fn<String, DublinCoreValue> mkValue = new Fn<String, DublinCoreValue>() {
    @Override public DublinCoreValue apply(String v) {
      return DublinCoreValue.mk(v);
    }
  };
}
