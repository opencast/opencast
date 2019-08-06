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
package org.opencastproject.lti.service.impl;

import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventHttpServletRequest;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.lti.service.api.Job;
import org.opencastproject.lti.service.api.LtiService;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowDatabaseException;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.cm.ManagedService;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LtiServiceImpl implements LtiService, ManagedService {
  private IndexService indexService;
  private IngestService ingestService;
  private SecurityService securityService;
  private SeriesService seriesService;
  private AbstractSearchIndex searchIndex;
  private String workflow;
  private String workflowConfiguration;
  private String retractWorkflowId;
  private final List<EventCatalogUIAdapter> catalogUIAdapters = new ArrayList<>();

  /** OSGi DI */
  public void setSearchIndex(AbstractSearchIndex searchIndex) {
    this.searchIndex = searchIndex;
  }

  /** OSGi DI */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /** OSGi DI */
  public void setIndexService(IndexService indexService) {
    this.indexService = indexService;
  }

  /** OSGi DI */
  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  /** OSGi DI */
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi DI. */
  public void addCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    catalogUIAdapters.add(catalogUIAdapter);
  }

  /** OSGi DI. */
  public void removeCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    catalogUIAdapters.remove(catalogUIAdapter);
  }

  private List<EventCatalogUIAdapter> getEventCatalogUIAdapters() {
    return new ArrayList<>(getEventCatalogUIAdapters(securityService.getOrganization().getId()));
  }

  public List<EventCatalogUIAdapter> getEventCatalogUIAdapters(String organization) {
    List<EventCatalogUIAdapter> adapters = new ArrayList<>();
    for (EventCatalogUIAdapter adapter : catalogUIAdapters) {
      if (organization.equals(adapter.getOrganization())) {
        adapters.add(adapter);
      }
    }
    return adapters;
  }

  @Override
  public List<Job> listJobs(String seriesName, String seriesId) {
    final User user = securityService.getUser();
    final EventSearchQuery query = new EventSearchQuery(securityService.getOrganization().getId(), user)
            .withCreator(user.getName()).withSeriesId(StringUtils.trimToNull(seriesId))
            .withSeriesName(StringUtils.trimToNull(seriesName));
    try {
      SearchResult<Event> results = this.searchIndex.getByQuery(query);
      ZonedDateTime startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);
      return Arrays.stream(results.getItems())
              .map(SearchResultItem::getSource)
              .filter(e -> ZonedDateTime.parse(e.getCreated()).compareTo(startOfDay) > 0)
              .map(e -> new Job(e.getTitle(), e.getEventStatus()))
              .collect(Collectors.toList());
    } catch (SearchIndexException e) {
      throw new RuntimeException("search index exception", e);
    }
  }

  @Override
  public String upload(final InputStream file, final String sourceName, String seriesId, final String seriesName,
          final Map<String, String> metadata) {
    if (workflow == null || workflowConfiguration == null) {
      throw new RuntimeException("no workflow configured, cannot upload");
    }
    try {
      final EventHttpServletRequest r = new EventHttpServletRequest();
      final MediaPackage mp = ingestService.createMediaPackage();
      if (mp == null) {
        throw new RuntimeException("Unable to create media package for event");
      }
      r.setMediaPackage(mp);
      final MetadataList metadataList = new MetadataList();
      final MediaPackageElementFlavor flavor = new MediaPackageElementFlavor("dublincore", "episode");
      final EventCatalogUIAdapter adapter = catalogUIAdapters.stream().filter(e -> e.getFlavor().equals(flavor)).findAny()
              .orElse(null);
      if (adapter == null) {
        throw new RuntimeException("no adapter found");
      }
      final MetadataCollection collection = adapter.getRawFields();
      if (StringUtils.trimToNull(seriesId) == null) {
        seriesId = resolveSeriesName(seriesName);
      }
      r.setMediaPackage(ingestService.addTrack(file, sourceName, MediaPackageElements.PRESENTER_SOURCE, mp));
      metadata.put("isPartOf", seriesId);
      final SimpleDateFormat sdf = MetadataField
              .getSimpleDateFormatter(collection.getOutputFields().get("startDate").getPattern().get());
      metadata.put("startDate", sdf.format(new Date()));
      metadata.put("duration", "6000");
      metadata.forEach((k, v) -> replaceField(collection, k, v));

      r.setAcl(new AccessControlList(new AccessControlEntry("ROLE_ADMIN", "write", true),
              new AccessControlEntry("ROLE_ADMIN", "read", true),
              new AccessControlEntry("ROLE_OAUTH_USER", "write", true),
              new AccessControlEntry("ROLE_OAUTH_USER", "read", true)));
      r.setProcessing(
              (JSONObject) new JSONParser().parse(
                      String.format("{\"workflow\":\"%s\",\"configuration\":%s}", workflow, workflowConfiguration)));
      r.setMetadataList(metadataList);
      metadataList.add(adapter, collection);

      JSONObject source = new JSONObject();
      source.put("type", "UPLOAD");
      r.setSource(source);
      indexService.createEvent(r);
      return seriesId;
    } catch (SchedulerException e) {
      if (e.getCause() != null && e.getCause() instanceof NotFoundException
              || e.getCause() instanceof IllegalArgumentException) {
        throw new RuntimeException("unable to create event", e.getCause());
      } else {
        throw new RuntimeException("unable to create event", e);
      }
    } catch (Exception e) {
      throw new RuntimeException("unable to create event", e);
    }
  }

  private void replaceField(MetadataCollection collection, String fieldName, String fieldValue) {
    final MetadataField<?> field = collection.getOutputFields().get(fieldName);
    collection.removeField(field);
    collection.addField(MetadataField.copyMetadataFieldWithValue(field, fieldValue));
  }

  private String resolveSeriesName(String seriesName) throws SeriesException, UnauthorizedException {
    DublinCoreCatalogList result;
    result = seriesService.getSeries(new SeriesQuery().setSeriesTitle(seriesName));
    if (result.getTotalCount() == 0) {
      throw new IllegalArgumentException("series with given name doesn't exist");
    }
    if (result.getTotalCount() > 1) {
      throw new IllegalArgumentException("more than one series matches given series name");
    }
    DublinCoreCatalog seriesResult = result.getCatalogList().get(0);
    final List<DublinCoreValue> identifiers = seriesResult.get(DublinCore.PROPERTY_IDENTIFIER);
    if (identifiers.size() != 1) {
      throw new IllegalArgumentException("more than one identifier in dublin core catalog for series");
    }
    return identifiers.get(0).getValue();
  }

  @Override
  public void delete(String id) {
    try {
      final Opt<Event> event = indexService.getEvent(id, searchIndex);
      if (event.isNone()) {
        throw new RuntimeException("Event '" + id + "' not found");
      }
      final IndexService.EventRemovalResult eventRemovalResult = indexService.removeEvent(event.get(), () -> {
      }, retractWorkflowId);
      if (eventRemovalResult == IndexService.EventRemovalResult.GENERAL_FAILURE) {
        throw new RuntimeException("error deleting event: " + eventRemovalResult);
      }
    } catch (WorkflowDatabaseException | SearchIndexException | UnauthorizedException | NotFoundException e) {
      throw new RuntimeException("error deleting event", e);
    }
  }

  /** OSGi callback if properties file is present */
  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    // Ensure properties is not null
    if (properties == null) {
      throw new IllegalArgumentException("No configuration specified for events endpoint");
    }
    String workflowStr = (String) properties.get("workflow");
    if (workflowStr == null) {
      throw new IllegalArgumentException("Configuration is missing 'workflow' parameter");
    }
    String workflowConfigurationStr = (String) properties.get("workflow-configuration");
    if (workflowConfigurationStr == null) {
      throw new IllegalArgumentException("Configuration is missing 'workflow-configuration' parameter");
    }
    try {
      new JSONParser().parse(workflowConfigurationStr);
      workflowConfiguration = workflowConfigurationStr;
      workflow = workflowStr;
      final String retractWorkflowId = (String) properties.get("retract-workflow-id");
      if (retractWorkflowId == null) {
        this.retractWorkflowId = "retract";
      } else {
        this.retractWorkflowId = retractWorkflowId;
      }
    } catch (ParseException e) {
      throw new IllegalArgumentException("Invalid JSON specified for workflow configuration");
    }
  }
}
