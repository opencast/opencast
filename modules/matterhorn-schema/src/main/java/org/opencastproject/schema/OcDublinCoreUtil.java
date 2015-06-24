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

package org.opencastproject.schema;

import static org.opencastproject.util.data.Option.option;

import org.opencastproject.mediapackage.EName;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Functions;
import org.opencastproject.util.data.functions.Strings;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Constructor, converter and encoder functions for {@link org.opencastproject.schema.OcDublinCore}. */
public final class OcDublinCoreUtil {
  private OcDublinCoreUtil() {
  }

  private static final Function<DublinCoreValue, String> value = new Function<DublinCoreValue, String>() {
    @Override public String apply(DublinCoreValue dublinCoreValue) {
      return dublinCoreValue.getValue();
    }
  };

  private static Function<String, Date> decodeDate = new Function<String, Date>() {
    @Override public Date apply(String s) {
      final Date d = EncodingSchemeUtils.decodeDate(s);
      if (d != null) return d;
      else throw new Error(s + " is not a W3C-DTF encoded date");
    }
  };

  public static DublinCoreValue encodeCreated(Date a) {
    return EncodingSchemeUtils.encodeDate(a, Precision.Second);
  }

  public static final Function<Date, DublinCoreValue> encodeCreated = new Function<Date, DublinCoreValue>() {
    @Override public DublinCoreValue apply(Date a) {
      return encodeCreated(a);
    }
  };

  public static DublinCoreValue encodeDate(Date a) {
    return EncodingSchemeUtils.encodeDate(a, Precision.Second);
  }

  public static final Function<Date, DublinCoreValue> encodeDate = new Function<Date, DublinCoreValue>() {
    @Override public DublinCoreValue apply(Date a) {
      return encodeDate(a);
    }
  };

  public static DublinCoreValue encodeDateAccepted(Date a) {
    return EncodingSchemeUtils.encodeDate(a, Precision.Second);
  }

  public static final Function<Date, DublinCoreValue> encodeDateAccepted = new Function<Date, DublinCoreValue>() {
    @Override public DublinCoreValue apply(Date a) {
      return encodeDateAccepted(a);
    }
  };

  public static DublinCoreValue encodeDateCopyrighted(Date a) {
    return EncodingSchemeUtils.encodeDate(a, Precision.Second);
  }

  public static final Function<Date, DublinCoreValue> encodeDateCopyrighted = new Function<Date, DublinCoreValue>() {
    @Override public DublinCoreValue apply(Date a) {
      return encodeDateCopyrighted(a);
    }
  };

  public static DublinCoreValue encodeDateSubmitted(Date a) {
    return EncodingSchemeUtils.encodeDate(a, Precision.Second);
  }

  public static final Function<Date, DublinCoreValue> encodeDateSubmitted = new Function<Date, DublinCoreValue>() {
    @Override public DublinCoreValue apply(Date a) {
      return encodeDateSubmitted(a);
    }
  };

  public static DublinCoreValue encodeExtent(Long a) {
    return DublinCoreValue.mk(a.toString());
  }

  public static final Function<Long, DublinCoreValue> encodeExtent = new Function<Long, DublinCoreValue>() {
    @Override public DublinCoreValue apply(Long a) {
      return encodeExtent(a);
    }
  };

  /**
   * Create a generic DublinCoreCatalog from an OcDublinCore.
   * Fields are encoded according to the Opencast rules. This class provides functions for each DublinCore
   * property that needs special encoding.
   */
  public static DublinCoreCatalog toCatalog(final OcDublinCore source) {
    // completeness assured by unit test
    final DublinCoreCatalog target = DublinCores.mkOpencast();
    for (String a : source.getAbstract()) target.set(DublinCore.PROPERTY_ABSTRACT, a);
    for (String a : source.getAccessRights()) target.set(DublinCore.PROPERTY_ACCESS_RIGHTS, a);
    for (String a : source.getAccrualMethod()) target.set(DublinCore.PROPERTY_ACCRUAL_METHOD, a);
    for (String a : source.getAccrualPeriodicity()) target.set(DublinCore.PROPERTY_ACCRUAL_PERIODICITY, a);
    for (String a : source.getAccrualPolicy()) target.set(DublinCore.PROPERTY_ACCRUAL_POLICY, a);
    for (String a : source.getAlternative()) target.set(DublinCore.PROPERTY_ALTERNATIVE, a);
    for (String a : source.getAudience()) target.set(DublinCore.PROPERTY_AUDIENCE, a);
    for (String a : source.getAvailable()) target.set(DublinCore.PROPERTY_AVAILABLE, a);
    for (String a : source.getBibliographicCitation()) target.set(DublinCore.PROPERTY_BIBLIOGRAPHIC_CITATION, a);
    for (String a : source.getConformsTo()) target.set(DublinCore.PROPERTY_CONFORMS_TO, a);
    for (String a : source.getContributor()) target.set(DublinCore.PROPERTY_CONTRIBUTOR, a);
    for (String a : source.getCoverage()) target.set(DublinCore.PROPERTY_COVERAGE, a);
    target.set(DublinCore.PROPERTY_CREATED, encodeCreated(source.getCreated()));
    for (String a : source.getCreator()) target.set(DublinCore.PROPERTY_CREATOR, a);
    for (Date a : source.getDate()) target.set(DublinCore.PROPERTY_DATE, encodeDate(a));
    for (Date a : source.getDateAccepted()) target.set(DublinCore.PROPERTY_DATE_ACCEPTED, encodeDateAccepted(a));
    for (Date a : source.getDateCopyrighted())
      target.set(DublinCore.PROPERTY_DATE_COPYRIGHTED, encodeDateCopyrighted(a));
    for (Date a : source.getDateSubmitted()) target.set(DublinCore.PROPERTY_DATE_SUBMITTED, encodeDateSubmitted(a));
    for (String a : source.getDescription()) target.set(DublinCore.PROPERTY_DESCRIPTION, a);
    for (String a : source.getEducationLevel()) target.set(DublinCore.PROPERTY_EDUCATION_LEVEL, a);
    for (Long a : source.getExtent()) target.set(DublinCore.PROPERTY_EXTENT, encodeExtent(a));
    for (String a : source.getFormat()) target.set(DublinCore.PROPERTY_FORMAT, a);
    for (String a : source.getHasFormat()) target.set(DublinCore.PROPERTY_HAS_FORMAT, a);
    for (String a : source.getHasPart()) target.set(DublinCore.PROPERTY_HAS_PART, a);
    for (String a : source.getHasVersion()) target.set(DublinCore.PROPERTY_HAS_VERSION, a);
    for (String a : source.getIdentifier()) target.set(DublinCore.PROPERTY_IDENTIFIER, a);
    for (String a : source.getInstructionalMethod()) target.set(DublinCore.PROPERTY_INSTRUCTIONAL_METHOD, a);
    for (String a : source.getIsFormatOf()) target.set(DublinCore.PROPERTY_IS_FORMAT_OF, a);
    for (String a : source.getIsPartOf()) target.set(DublinCore.PROPERTY_IS_PART_OF, a);
    for (String a : source.getIsReferencedBy()) target.set(DublinCore.PROPERTY_IS_REFERENCED_BY, a);
    for (String a : source.getIsReplacedBy()) target.set(DublinCore.PROPERTY_IS_REPLACED_BY, a);
    for (String a : source.getIsRequiredBy()) target.set(DublinCore.PROPERTY_IS_REQUIRED_BY, a);
    for (String a : source.getIssued()) target.set(DublinCore.PROPERTY_ISSUED, a);
    for (String a : source.getIsVersionOf()) target.set(DublinCore.PROPERTY_IS_VERSION_OF, a);
    for (String a : source.getLanguage()) target.set(DublinCore.PROPERTY_LANGUAGE, a);
    for (String a : source.getLicense()) target.set(DublinCore.PROPERTY_LICENSE, a);
    for (String a : source.getMediator()) target.set(DublinCore.PROPERTY_MEDIATOR, a);
    for (String a : source.getMedium()) target.set(DublinCore.PROPERTY_MEDIUM, a);
    for (String a : source.getModified()) target.set(DublinCore.PROPERTY_MODIFIED, a);
    for (String a : source.getProvenance()) target.set(DublinCore.PROPERTY_PROVENANCE, a);
    for (String a : source.getPublisher()) target.set(DublinCore.PROPERTY_PUBLISHER, a);
    for (String a : source.getReferences()) target.set(DublinCore.PROPERTY_REFERENCES, a);
    for (String a : source.getRelation()) target.set(DublinCore.PROPERTY_RELATION, a);
    for (String a : source.getReplaces()) target.set(DublinCore.PROPERTY_REPLACES, a);
    for (String a : source.getRequires()) target.set(DublinCore.PROPERTY_REQUIRES, a);
    for (String a : source.getRights()) target.set(DublinCore.PROPERTY_RIGHTS, a);
    for (String a : source.getRightsHolder()) target.set(DublinCore.PROPERTY_RIGHTS_HOLDER, a);
    for (String a : source.getSource()) target.set(DublinCore.PROPERTY_SOURCE, a);
    for (String a : source.getSpatial()) target.set(DublinCore.PROPERTY_SPATIAL, a);
    for (String a : source.getSubject()) target.set(DublinCore.PROPERTY_SUBJECT, a);
    for (String a : source.getTableOfContents()) target.set(DublinCore.PROPERTY_TABLE_OF_CONTENTS, a);
    for (String a : source.getTemporal()) target.set(DublinCore.PROPERTY_TEMPORAL, a);
    target.set(DublinCore.PROPERTY_TITLE, source.getTitle());
    for (String a : source.getType()) target.set(DublinCore.PROPERTY_TYPE, a);
    for (String a : source.getValid()) target.set(DublinCore.PROPERTY_VALID, a);
    return target;
  }

  public static class OcDublinCoreConversion {
    private final OcDublinCore dublinCore;
    private final List<String> conversionErrors;

    public OcDublinCoreConversion(OcDublinCore dublinCore, List<String> conversionErrors) {
      this.dublinCore = dublinCore;
      this.conversionErrors = conversionErrors;
    }

    public OcDublinCore getDublinCore() {
      return dublinCore;
    }

    public List<String> getConversionErrors() {
      return conversionErrors;
    }
  }

  /**
   * Create an OcDublinCore from a generic DublinCoreCatalog enforcing schema rules.
   * If mandatory properties are missing default values will be used and the error is recorded.
   */
  public static OcDublinCoreConversion create(DublinCoreCatalog src) {
    final List<String> errors = new ArrayList<String>();
    final Option<String> abstrakt = option(src.getFirst(DublinCore.PROPERTY_ABSTRACT));
    final Option<String> accessRights = option(src.getFirst(DublinCore.PROPERTY_ACCESS_RIGHTS));
    final Option<String> accrualMethod = option(src.getFirst(DublinCore.PROPERTY_ACCRUAL_METHOD));
    final Option<String> accrualPeriodicity = option(src.getFirst(DublinCore.PROPERTY_ACCRUAL_PERIODICITY));
    final Option<String> accrualPolicy = option(src.getFirst(DublinCore.PROPERTY_ACCRUAL_POLICY));
    final Option<String> alternative = option(src.getFirst(DublinCore.PROPERTY_ALTERNATIVE));
    final Option<String> audience = option(src.getFirst(DublinCore.PROPERTY_AUDIENCE));
    final Option<String> available = option(src.getFirst(DublinCore.PROPERTY_AVAILABLE));
    final Option<String> bibliographicCitation = option(src.getFirst(DublinCore.PROPERTY_BIBLIOGRAPHIC_CITATION));
    final Option<String> conformsTo = option(src.getFirst(DublinCore.PROPERTY_CONFORMS_TO));
    final Option<String> contributor = option(src.getFirst(DublinCore.PROPERTY_CONTRIBUTOR));
    final Option<String> coverage = option(src.getFirst(DublinCore.PROPERTY_COVERAGE));
    final Date created = getMandatoryProperty(src, DublinCore.PROPERTY_CREATED, decodeDate, new Date(0), errors);
    final Option<String> creator = option(src.getFirst(DublinCore.PROPERTY_CREATOR));
    final Option<Date> date = option(src.getFirst(DublinCore.PROPERTY_DATE)).map(decodeDate);
    final Option<Date> dateAccepted = option(src.getFirst(DublinCore.PROPERTY_DATE_ACCEPTED)).map(decodeDate);
    final Option<Date> dateCopyrighted = option(src.getFirst(DublinCore.PROPERTY_DATE_COPYRIGHTED)).map(decodeDate);
    final Option<Date> dateSubmitted = option(src.getFirst(DublinCore.PROPERTY_DATE_SUBMITTED)).map(decodeDate);
    final Option<String> description = option(src.getFirst(DublinCore.PROPERTY_DESCRIPTION));
    final Option<String> educationLevel = option(src.getFirst(DublinCore.PROPERTY_EDUCATION_LEVEL));
    final Option<Long> extent = option(src.getFirst(DublinCore.PROPERTY_EXTENT)).bind(Strings.toLong);
    final Option<String> format = option(src.getFirst(DublinCore.PROPERTY_FORMAT));
    final Option<String> hasFormat = option(src.getFirst(DublinCore.PROPERTY_HAS_FORMAT));
    final Option<String> hasPart = option(src.getFirst(DublinCore.PROPERTY_HAS_PART));
    final Option<String> hasVersion = option(src.getFirst(DublinCore.PROPERTY_HAS_VERSION));
    final Option<String> identifier = option(src.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    final Option<String> instructionalMethod = option(src.getFirst(DublinCore.PROPERTY_INSTRUCTIONAL_METHOD));
    final Option<String> isFormatOf = option(src.getFirst(DublinCore.PROPERTY_IS_FORMAT_OF));
    final Option<String> isPartOf = option(src.getFirst(DublinCore.PROPERTY_IS_PART_OF));
    final Option<String> isReferencedBy = option(src.getFirst(DublinCore.PROPERTY_IS_REFERENCED_BY));
    final Option<String> isReplacedBy = option(src.getFirst(DublinCore.PROPERTY_IS_REPLACED_BY));
    final Option<String> isRequiredBy = option(src.getFirst(DublinCore.PROPERTY_IS_REQUIRED_BY));
    final Option<String> issued = option(src.getFirst(DublinCore.PROPERTY_ISSUED));
    final Option<String> isVersionOf = option(src.getFirst(DublinCore.PROPERTY_IS_VERSION_OF));
    final Option<String> language = option(src.getFirst(DublinCore.PROPERTY_LANGUAGE));
    final Option<String> license = option(src.getFirst(DublinCore.PROPERTY_LICENSE));
    final Option<String> mediator = option(src.getFirst(DublinCore.PROPERTY_MEDIATOR));
    final Option<String> medium = option(src.getFirst(DublinCore.PROPERTY_MEDIUM));
    final Option<String> modified = option(src.getFirst(DublinCore.PROPERTY_MODIFIED));
    final Option<String> provenance = option(src.getFirst(DublinCore.PROPERTY_PROVENANCE));
    final Option<String> publisher = option(src.getFirst(DublinCore.PROPERTY_PUBLISHER));
    final Option<String> references = option(src.getFirst(DublinCore.PROPERTY_REFERENCES));
    final Option<String> relation = option(src.getFirst(DublinCore.PROPERTY_RELATION));
    final Option<String> replaces = option(src.getFirst(DublinCore.PROPERTY_REPLACES));
    final Option<String> requires = option(src.getFirst(DublinCore.PROPERTY_REQUIRES));
    final Option<String> rights = option(src.getFirst(DublinCore.PROPERTY_RIGHTS));
    final Option<String> rightsHolder = option(src.getFirst(DublinCore.PROPERTY_RIGHTS_HOLDER));
    final Option<String> source = option(src.getFirst(DublinCore.PROPERTY_SOURCE));
    final Option<String> spatial = option(src.getFirst(DublinCore.PROPERTY_SPATIAL));
    final Option<String> subject = option(src.getFirst(DublinCore.PROPERTY_SUBJECT));
    final Option<String> tableOfContents = option(src.getFirst(DublinCore.PROPERTY_TABLE_OF_CONTENTS));
    final Option<String> temporal = option(src.getFirst(DublinCore.PROPERTY_TEMPORAL));
    final String title = getMandatoryProperty(src, DublinCore.PROPERTY_TITLE, Functions.<String>identity(), "?", errors);
    final Option<String> type = option(src.getFirst(DublinCore.PROPERTY_TYPE));
    final Option<String> valid = option(src.getFirst(DublinCore.PROPERTY_VALID));

    final OcDublinCore dc = new OcDublinCore() {
      @Override public Option<String> getAbstract() {
        return abstrakt;
      }

      @Override public Option<String> getAccessRights() {
        return accessRights;
      }

      @Override public Option<String> getAccrualMethod() {
        return accrualMethod;
      }

      @Override public Option<String> getAccrualPeriodicity() {
        return accrualPeriodicity;
      }

      @Override public Option<String> getAccrualPolicy() {
        return accrualPolicy;
      }

      @Override public Option<String> getAlternative() {
        return alternative;
      }

      @Override public Option<String> getAudience() {
        return audience;
      }

      @Override public Option<String> getAvailable() {
        return available;
      }

      @Override public Option<String> getBibliographicCitation() {
        return bibliographicCitation;
      }

      @Override public Option<String> getConformsTo() {
        return conformsTo;
      }

      @Override public Option<String> getContributor() {
        return contributor;
      }

      @Override public Option<String> getCoverage() {
        return coverage;
      }

      @Override public Date getCreated() {
        return created;
      }

      @Override public Option<String> getCreator() {
        return creator;
      }

      @Override public Option<Date> getDate() {
        return date;
      }

      @Override public Option<Date> getDateAccepted() {
        return dateAccepted;
      }

      @Override public Option<Date> getDateCopyrighted() {
        return dateCopyrighted;
      }

      @Override public Option<Date> getDateSubmitted() {
        return dateSubmitted;
      }

      @Override public Option<String> getDescription() {
        return description;
      }

      @Override public Option<String> getEducationLevel() {
        return educationLevel;
      }

      @Override public Option<Long> getExtent() {
        return extent;
      }

      @Override public Option<String> getFormat() {
        return format;
      }

      @Override public Option<String> getHasFormat() {
        return hasFormat;
      }

      @Override public Option<String> getHasPart() {
        return hasPart;
      }

      @Override public Option<String> getHasVersion() {
        return hasVersion;
      }

      @Override public Option<String> getIdentifier() {
        return identifier;
      }

      @Override public Option<String> getInstructionalMethod() {
        return instructionalMethod;
      }

      @Override public Option<String> getIsFormatOf() {
        return isFormatOf;
      }

      @Override public Option<String> getIsPartOf() {
        return isPartOf;
      }

      @Override public Option<String> getIsReferencedBy() {
        return isReferencedBy;
      }

      @Override public Option<String> getIsReplacedBy() {
        return isReplacedBy;
      }

      @Override public Option<String> getIsRequiredBy() {
        return isRequiredBy;
      }

      @Override public Option<String> getIssued() {
        return issued;
      }

      @Override public Option<String> getIsVersionOf() {
        return isVersionOf;
      }

      @Override public Option<String> getLanguage() {
        return language;
      }

      @Override public Option<String> getLicense() {
        return license;
      }

      @Override public Option<String> getMediator() {
        return mediator;
      }

      @Override public Option<String> getMedium() {
        return medium;
      }

      @Override public Option<String> getModified() {
        return modified;
      }

      @Override public Option<String> getProvenance() {
        return provenance;
      }

      @Override public Option<String> getPublisher() {
        return publisher;
      }

      @Override public Option<String> getReferences() {
        return references;
      }

      @Override public Option<String> getRelation() {
        return relation;
      }

      @Override public Option<String> getReplaces() {
        return replaces;
      }

      @Override public Option<String> getRequires() {
        return requires;
      }

      @Override public Option<String> getRights() {
        return rights;
      }

      @Override public Option<String> getRightsHolder() {
        return rightsHolder;
      }

      @Override public Option<String> getSource() {
        return source;
      }

      @Override public Option<String> getSpatial() {
        return spatial;
      }

      @Override public Option<String> getSubject() {
        return subject;
      }

      @Override public Option<String> getTableOfContents() {
        return tableOfContents;
      }

      @Override public Option<String> getTemporal() {
        return temporal;
      }

      @Override public String getTitle() {
        return title;
      }

      @Override public Option<String> getType() {
        return type;
      }

      @Override public Option<String> getValid() {
        return valid;
      }
    };
    return new OcDublinCoreConversion(dc, errors);
  }

  private static <A> A getMandatoryProperty(DublinCoreCatalog c, EName property, Function<String, A> convert, A dflt,
                                            List<String> errors) {
    for (A value : option(c.getFirst(property)).map(convert)) {
      return value;
    }
    errors.add(property.getLocalName() + " is missing");
    return dflt;
  }

  /** Create a JAXB transfer object from an OcDublinCore. */
  public static JaxbOcDublinCore toJaxb(final OcDublinCore source) {
    // completeness assured by unit test
    final JaxbOcDublinCore target = new JaxbOcDublinCore();
    target.abstrakt = source.getAbstract().getOrElseNull();
    target.accessRights = source.getAccessRights().getOrElseNull();
    target.accrualMethod = source.getAccrualMethod().getOrElseNull();
    target.accrualPeriodicity = source.getAccrualPeriodicity().getOrElseNull();
    target.accrualPolicy = source.getAccrualPolicy().getOrElseNull();
    target.alternative = source.getAlternative().getOrElseNull();
    target.audience = source.getAudience().getOrElseNull();
    target.available = source.getAvailable().getOrElseNull();
    target.bibliographicCitation = source.getBibliographicCitation().getOrElseNull();
    target.conformsTo = source.getConformsTo().getOrElseNull();
    target.contributor = source.getContributor().getOrElseNull();
    target.coverage = source.getCoverage().getOrElseNull();
    target.created = source.getCreated();
    target.creator = source.getCreator().getOrElseNull();
    target.date = source.getDate().getOrElseNull();
    target.dateAccepted = source.getDateAccepted().getOrElseNull();
    target.dateCopyrighted = source.getDateCopyrighted().getOrElseNull();
    target.dateSubmitted = source.getDateSubmitted().getOrElseNull();
    target.description = source.getDescription().getOrElseNull();
    target.educationLevel = source.getEducationLevel().getOrElseNull();
    target.extent = source.getExtent().getOrElseNull();
    target.format = source.getFormat().getOrElseNull();
    target.hasFormat = source.getHasFormat().getOrElseNull();
    target.hasPart = source.getHasPart().getOrElseNull();
    target.hasVersion = source.getHasVersion().getOrElseNull();
    target.identifier = source.getIdentifier().getOrElseNull();
    target.instructionalMethod = source.getInstructionalMethod().getOrElseNull();
    target.isFormatOf = source.getIsFormatOf().getOrElseNull();
    target.isPartOf = source.getIsPartOf().getOrElseNull();
    target.isReferencedBy = source.getIsReferencedBy().getOrElseNull();
    target.isReplacedBy = source.getIsReplacedBy().getOrElseNull();
    target.isRequiredBy = source.getIsRequiredBy().getOrElseNull();
    target.issued = source.getIssued().getOrElseNull();
    target.isVersionOf = source.getIsVersionOf().getOrElseNull();
    target.language = source.getLanguage().getOrElseNull();
    target.license = source.getLicense().getOrElseNull();
    target.mediator = source.getMediator().getOrElseNull();
    target.medium = source.getMedium().getOrElseNull();
    target.modified = source.getModified().getOrElseNull();
    target.provenance = source.getProvenance().getOrElseNull();
    target.publisher = source.getPublisher().getOrElseNull();
    target.references = source.getReferences().getOrElseNull();
    target.relation = source.getRelation().getOrElseNull();
    target.replaces = source.getReplaces().getOrElseNull();
    target.requires = source.getRequires().getOrElseNull();
    target.rights = source.getRights().getOrElseNull();
    target.rightsHolder = source.getRightsHolder().getOrElseNull();
    target.source = source.getSource().getOrElseNull();
    target.spatial = source.getSpatial().getOrElseNull();
    target.subject = source.getSubject().getOrElseNull();
    target.tableOfContents = source.getTableOfContents().getOrElseNull();
    target.temporal = source.getTemporal().getOrElseNull();
    target.title = source.getTitle();
    target.type = source.getType().getOrElseNull();
    target.valid = source.getValid().getOrElseNull();
    return target;
  }
}
