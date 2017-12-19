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
package org.opencastproject.scheduler.impl;

import static com.entwinemedia.fn.Prelude.chuck;
import static com.entwinemedia.fn.Stream.$;

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;
import org.opencastproject.scheduler.api.TechnicalMetadata;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Functions to support scheduler service operations.
 */
public final class SchedulerUtil {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SchedulerUtil.class);

  private SchedulerUtil() {
  }

  public static final Comparator<Catalog> sortCatalogById = new Comparator<Catalog>() {
    @Override
    public int compare(Catalog c1, Catalog c2) {
      return c1.getIdentifier().compareTo(c2.getIdentifier());
    }
  };

  public static String calculateChecksum(
      Workspace workspace,
      List<MediaPackageElementFlavor> eventCatalogUIAdapterFlavors,
      Date startDateTime,
      Date endDateTime,
      String captureAgentId,
      Set<String> userIds,
      MediaPackage mediaPackage,
      Opt<DublinCoreCatalog> episodeDublincore,
      Map<String, String> wfProperties,
      Map<String, String> finalCaProperties,
      boolean optOut,
      AccessControlList acl) {
    List<String> userIdsList = new ArrayList<>(userIds);
    Collections.sort(userIdsList);
    final MessageDigest messageDigest = mkMd5MessageDigest();
    messageDigest.update(mkChecksumInput(startDateTime));
    messageDigest.update(mkChecksumInput(endDateTime));
    messageDigest.update(mkChecksumInput(captureAgentId));
    for (String user : userIdsList) {
      messageDigest.update(mkChecksumInput(user));
    }
    if (episodeDublincore.isSome()) {
      Catalog episodeCatalog = $(mediaPackage.getCatalogs())
          .filter(MediaPackageSupport.Filters.isEpisodeDublinCore.toFn()).head2();
      Checksum checksum = episodeCatalog.getChecksum();
      if (checksum == null) {
        checksum = DublinCoreUtil.calculateChecksum(episodeDublincore.get());
        episodeCatalog.setChecksum(checksum);
      }
      messageDigest.update(mkChecksumInput(checksum.toString()));
    }
    // Add extended metadata to calculation
    for (Catalog c : $(mediaPackage.getCatalogs()).sort(sortCatalogById)) {
      if (eventCatalogUIAdapterFlavors.contains(c.getFlavor())) {
        Checksum checksum = c.getChecksum();
        if (checksum == null) {
          DublinCoreCatalog dublinCore = DublinCoreUtil.loadDublinCore(workspace, c);
          checksum = DublinCoreUtil.calculateChecksum(dublinCore);
          c.setChecksum(checksum);
        }
        messageDigest.update(mkChecksumInput(checksum.toString()));
      }
    }
    messageDigest.update(mkChecksumInput(AccessControlUtil.calculateChecksum(acl).toString()));
    for (Entry<String, String> entry : new TreeMap<>(wfProperties).entrySet()) {
      messageDigest.update(mkChecksumInput(entry.getKey()));
      messageDigest.update(mkChecksumInput(entry.getValue()));
    }
    for (Entry<String, String> entry : new TreeMap<>(finalCaProperties).entrySet()) {
      messageDigest.update(mkChecksumInput(entry.getKey()));
      messageDigest.update(mkChecksumInput(entry.getValue()));
    }
    messageDigest.update(mkChecksumInput(optOut));
    return Checksum.convertToHex(messageDigest.digest());
  }

  private static MessageDigest mkMd5MessageDigest() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      logger.error("Unable to create md5 message digest");
      return chuck(e);
    }
  }

  private static byte[] mkChecksumInput(String input) {
    return input.getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] mkChecksumInput(Date input) {
    return mkChecksumInput(Long.toString(input.getTime()));
  }

  private static byte mkChecksumInput(boolean input) {
    return (byte) (input ? 1 : 0);
  }

  /**
   * Converts a scheduler event to a human readable string
   *
   * @param workspace
   *          the workspace
   * @param catalogFlavors
   *          the event catalog flavors
   * @param event
   *          the scheduler event
   * @return a human readable string
   */
  public static String toHumanReadableString(Workspace workspace, List<MediaPackageElementFlavor> catalogFlavors,
          SchedulerEvent event) {
    TechnicalMetadata technicalMetadata = event.getTechnicalMetadata();
    StringBuilder sb = new StringBuilder("Event: ").append(CharUtils.LF);
    sb.append("- ").append(event.getEventId()).append(CharUtils.LF);
    sb.append(CharUtils.LF);

    sb.append("Version").append(CharUtils.LF);
    sb.append("- ").append(event.getVersion()).append(CharUtils.LF);
    sb.append(CharUtils.LF);

    sb.append("Start").append(CharUtils.LF);
    sb.append("- ").append(DateTimeSupport.toUTC(technicalMetadata.getStartDate().getTime())).append(CharUtils.LF);
    sb.append(CharUtils.LF);

    sb.append("End").append(CharUtils.LF);
    sb.append("- ").append(DateTimeSupport.toUTC(technicalMetadata.getEndDate().getTime())).append(CharUtils.LF);
    sb.append(CharUtils.LF);

    sb.append("Room").append(CharUtils.LF);
    sb.append("- ").append(technicalMetadata.getAgentId()).append(CharUtils.LF);
    sb.append(CharUtils.LF);

    sb.append("Scheduling configuration").append(CharUtils.LF);
    sb.append("- optout: ").append(technicalMetadata.isOptOut()).append(CharUtils.LF);
    for (Entry<String, String> entry : technicalMetadata.getCaptureAgentConfiguration().entrySet()) {
      sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append(CharUtils.LF);
    }
    sb.append(CharUtils.LF);

    sb.append("Presenters").append(CharUtils.LF);
    for (String presenter : technicalMetadata.getPresenters()) {
      sb.append("- ").append(presenter).append(CharUtils.LF);
    }
    sb.append(CharUtils.LF);

    sb.append("Workflow configuration").append(CharUtils.LF);
    for (Entry<String, String> entry : technicalMetadata.getWorkflowProperties().entrySet()) {
      sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append(CharUtils.LF);
    }
    sb.append(CharUtils.LF);

    for (Catalog c : $(event.getMediaPackage().getCatalogs())) {
      if (!catalogFlavors.contains(c.getFlavor()))
        continue;

      DublinCoreCatalog dublinCore;
      try {
        dublinCore = DublinCoreUtil.loadDublinCore(workspace, c);
      } catch (Exception e) {
        logger.error("Unable to read event dublincore: {}", ExceptionUtils.getStackTrace(e));
        continue;
      }

      sb.append("Event metadata ").append("(").append(c.getFlavor().toString()).append(")").append(CharUtils.LF);
      for (Entry<EName, List<DublinCoreValue>> entry : dublinCore.getValues().entrySet()) {
        EName eName = entry.getKey();
        for (DublinCoreValue value : entry.getValue()) {
          sb.append("- ").append(eName.getNamespaceURI()).append(":").append(eName.getLocalName()).append(": ")
                  .append(value.getValue());

          boolean hasLanguageDefined = !DublinCore.LANGUAGE_UNDEFINED.equals(value.getLanguage());

          if (hasLanguageDefined || value.getEncodingScheme().isSome()) {
            sb.append(" (");
            if (hasLanguageDefined) {
              sb.append("lang:").append(value.getLanguage());
              if (value.getEncodingScheme().isSome())
                sb.append("/");
            }

            for (EName schema : value.getEncodingScheme()) {
              sb.append(schema.getLocalName());
            }
            sb.append(")");
          }
          sb.append(CharUtils.LF);
        }
      }
      sb.append(CharUtils.LF);
    }
    return sb.toString();
  }

  public static final Fn<MediaPackageElementFlavor, Boolean> isNotEpisodeDublinCore = new Fn<MediaPackageElementFlavor, Boolean>() {
    @Override
    public Boolean apply(MediaPackageElementFlavor mpe) {
      // match is commutative
      return !MediaPackageElements.EPISODE.matches(mpe);
    }
  };

  public static final Fn<EventCatalogUIAdapter, MediaPackageElementFlavor> uiAdapterToFlavor = new Fn<EventCatalogUIAdapter, MediaPackageElementFlavor>() {
    @Override
    public MediaPackageElementFlavor apply(EventCatalogUIAdapter adapter) {
      return adapter.getFlavor();
    }
  };

  public static final Fn2<EventCatalogUIAdapter, String, Boolean> eventOrganizationFilter = new Fn2<EventCatalogUIAdapter, String, Boolean>() {
    @Override
    public Boolean apply(EventCatalogUIAdapter catalogUIAdapter, String organization) {
      return catalogUIAdapter.getOrganization().equals(organization);
    }
  };

  public static final Fn2<Property, String, Boolean> filterByNamespace = new Fn2<Property, String, Boolean>() {
    @Override
    public Boolean apply(Property a, String b) {
      return b.equals(a.getId().getNamespace());
    }
  };

  public static final Fn<Snapshot, MediaPackage> episodeToMp = new Fn<Snapshot, MediaPackage>() {
    @Override
    public MediaPackage apply(Snapshot snapshot) {
      return snapshot.getMediaPackage();
    }
  };

  public static final Fn<ARecord, Opt<MediaPackage>> recordToMp = new Fn<ARecord, Opt<MediaPackage>>() {
    @Override
    public Opt<MediaPackage> apply(ARecord record) {
      return record.getSnapshot().map(episodeToMp);
    }
  };

  public static final Fn<Boolean, String> decomposeBooleanValue = new Fn<Boolean, String>() {
    @Override
    public String apply(Boolean b) {
      return b.toString();
    }
  };

  public static final Fn<Long, String> decomposeLongValue = new Fn<Long, String>() {
    @Override
    public String apply(Long l) {
      return l.toString();
    }
  };

  public static final Fn<Date, String> decomposeDateValue = new Fn<Date, String>() {
    @Override
    public String apply(Date d) {
      return DateTimeSupport.toUTC(d.getTime());
    }
  };

  public static final Fn<String, String> decomposeStringValue = new Fn<String, String>() {
    @Override
    public String apply(String s) {
      return s;
    }
  };

  public static final Fn<Version, String> decomposeVersionValue = new Fn<Version, String>() {
    @Override
    public String apply(Version v) {
      return v.toString();
    }
  };

  public static final Fn<Property, String> toKey = new Fn<Property, String>() {
    @Override
    public String apply(Property property) {
      return property.getId().getName();
    }
  };

  public static final Fn<Property, String> toValue = new Fn<Property, String>() {
    @Override
    public String apply(Property property) {
      return property.getValue().decompose(decomposeStringValue, decomposeDateValue, decomposeLongValue,
              decomposeBooleanValue, decomposeVersionValue);
    }
  };

  public static final Fn<String, ReviewStatus> toReviewStatus = new Fn<String, ReviewStatus>() {
    @Override
    public ReviewStatus apply(String reviewStatus) {
      return ReviewStatus.valueOf(reviewStatus);
    }
  };

}
