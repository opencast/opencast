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

package org.opencastproject.statistics.export.impl;

import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.impl.index.series.SeriesSearchQuery;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchQuery;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.statistics.api.DataResolution;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.api.StatisticsService;
import org.opencastproject.statistics.api.TimeSeries;
import org.opencastproject.statistics.api.TimeSeriesProvider;
import org.opencastproject.statistics.export.api.DetailLevel;
import org.opencastproject.statistics.export.api.StatisticsExportService;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class StatisticsExportServiceImpl implements StatisticsExportService, ManagedService {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(StatisticsExportServiceImpl.class);
  private static final String[] header = {"ID", "Name", "Date", "Value"};
  private static final String CFG_KEY_SERIES_TO_EVENT_PROVIDER_MAPPINGS = "series.to.event.provider.mappings";
  private static final String CFG_KEY_ORGANIZATION_TO_EVENT_PROVIDER_MAPPINGS = "organization.to.event.provider.mappings";
  private static final String CFG_KEY_ORGANIZATION_TO_SERIES_PROVIDER_MAPPINGS = "organization.to.series.provider.mappings";


  private Map<String, String> seriesToEventProviderMapping = new HashMap<>();
  private Map<String, String> organizationToEventProviderMapping = new HashMap<>();
  private Map<String, String> organizationToSeriesProviderMapping = new HashMap<>();

  private IndexService indexService;
  private SecurityService securityService;
  private StatisticsService statisticsService;
  private AssetManager assetManager;

  @Override
  public void updated(Dictionary<String, ?> dictionary) {
    final String seriesToEventProviderMappings = (String) dictionary.get(CFG_KEY_SERIES_TO_EVENT_PROVIDER_MAPPINGS);
    if (seriesToEventProviderMappings != null) {
      this.seriesToEventProviderMapping = getMapping(seriesToEventProviderMappings);
    }
    final String organizationToEventProviderMappings = (String) dictionary.get(CFG_KEY_ORGANIZATION_TO_EVENT_PROVIDER_MAPPINGS);
    if (organizationToEventProviderMappings != null) {
      this.organizationToEventProviderMapping = getMapping(organizationToEventProviderMappings);
    }
    final String organizationToSeriesProviderMappings = (String) dictionary.get(CFG_KEY_ORGANIZATION_TO_SERIES_PROVIDER_MAPPINGS);
    if (organizationToSeriesProviderMappings != null) {
      this.organizationToSeriesProviderMapping = getMapping(organizationToSeriesProviderMappings);
    }
  }

  private Map<String, String> getMapping(String seriesProviderMappings) {
    return Arrays.stream(seriesProviderMappings.split(","))
        .peek(s -> {
          if (!s.contains(":")) {
            throw new ConfigurationException("Missing ':' in mapping between providers: " + s);
          }
        })
        .collect(Collectors.toMap(
            s -> s.split(":")[0], s -> s.split(":")[1]
        ));
  }

  public void activate(ComponentContext cc) {
    logger.info("Activating Statistics Service");
  }

  public void deactivate(ComponentContext cc) {
    logger.info("Deactivating Statistics Service");
  }

  public void setIndexService(IndexService indexService) {
    this.indexService = indexService;
  }

  public void setStatisticsService(StatisticsService statisticsService) {
    this.statisticsService = statisticsService;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public void setAssetManager(final AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  private static String formatDate(final String dateStr, DataResolution dataResolution, ZoneId zoneId) {
    final LocalDateTime ldt = LocalDateTime.ofInstant(Instant.parse(dateStr), zoneId);
    DateTimeFormatter formatter = null;
    switch (dataResolution) {
      case HOURLY:
        formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:00");
        return formatter.format(ldt);
      case DAILY:
        formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd");
        return formatter.format(ldt);
      case WEEKLY:
        formatter = DateTimeFormatter.ofPattern("uuuu-ww");
        return formatter.format(ldt);
      case MONTHLY:
        formatter = DateTimeFormatter.ofPattern("uuuu-MM");
        return formatter.format(ldt);
      case YEARLY:
        formatter = DateTimeFormatter.ofPattern("uuuu");
        return formatter.format(ldt);
      default:
        throw new IllegalStateException("Unexpected value: " + dataResolution);
    }
  }


  @Override
  public String getCSV(StatisticsProvider provider, String resourceId, Instant from, Instant to, DataResolution
      dataResolution, AbstractSearchIndex index, ZoneId zoneId) throws SearchIndexException, UnauthorizedException,
      NotFoundException {
    if (!(provider instanceof TimeSeriesProvider)) {
      throw new IllegalStateException("CSV export not supported for provider of type " + provider.getClass().getName());
    }
    final StringWriter stringWriter = new StringWriter();
    try (CSVPrinter printer = CSVFormat.RFC4180.print(stringWriter)) {
      switch (provider.getResourceType()) {
        case EPISODE:
          printEvent(provider, resourceId, from, to, dataResolution, index, zoneId, printer, false, 0, 0);
          break;
        case SERIES:
          if (seriesToEventProviderMapping.containsKey(provider.getId())) {
            // Advanced: instead of exporting the series data we export the data of all series events
            printSeriesEvents(provider, resourceId, from, to, dataResolution, index, zoneId, printer, false,
                    0, 0, Collections.emptyMap());
          } else {
            // Default: just export series data
            printSeries(provider, resourceId, from, to, dataResolution, index, zoneId, printer, false, 0, 0);
          }
          break;
        case ORGANIZATION:
          if (organizationToEventProviderMapping.containsKey(provider.getId())) {
            // Advanced: instead of exporting the organization data we export the data of all organization events
            printOrganizationEvents(provider, resourceId, from, to, dataResolution, index, zoneId, printer, false,
                    0, 0, Collections.emptyMap());
          } else if (organizationToSeriesProviderMapping.containsKey(provider.getId())) {
            // Advanced: instead of exporting the organization data we export the data of all organization series
            printOrganizationSeries(provider, resourceId, from, to, dataResolution, index, zoneId, printer, false,
                    0, 0, Collections.emptyMap());
          } else {
            printOrganization(provider, resourceId, from, to, dataResolution, zoneId, printer, 0, 0);
          }
          break;
        default:
          throw new IllegalStateException("Unknown resource type: " + provider.getResourceType().name());
      }
    } catch (IOException e) {
      return chuck(e);
    }
    return stringWriter.toString();
  }

  @Override
  public String getCSV(StatisticsProvider provider, String resourceId, Instant from, Instant to, DataResolution
          dataResolution, AbstractSearchIndex index, ZoneId zoneId, boolean fullMetadata, DetailLevel detailLevel,
          int limit, int offset, Map<String, String> filters)
          throws SearchIndexException, UnauthorizedException, NotFoundException {
    if (!(provider instanceof TimeSeriesProvider)) {
      throw new IllegalStateException("CSV export not supported for provider of type " + provider.getClass().getName());
    }
    final StringWriter stringWriter = new StringWriter();
    try (CSVPrinter printer = CSVFormat.RFC4180.print(stringWriter)) {
      switch (provider.getResourceType()) {
        case EPISODE:
          printEvent(provider, resourceId, from, to, dataResolution, index, zoneId, printer, fullMetadata, limit, offset);
          break;
        case SERIES:
          if (detailLevel == DetailLevel.EPISODE) {
            // Advanced: instead of exporting the series data we export the data of all series events
            printSeriesEvents(provider, resourceId, from, to, dataResolution, index, zoneId, printer, fullMetadata,
                    limit, offset, filters);
          } else {
            // Default: just export series data
            printSeries(provider, resourceId, from, to, dataResolution, index, zoneId, printer, fullMetadata, limit, offset);
          }
          break;
        case ORGANIZATION:
          if (detailLevel == DetailLevel.EPISODE) {
            // Advanced: instead of exporting the organization data we export the data of all organization events
            printOrganizationEvents(provider, resourceId, from, to, dataResolution, index, zoneId, printer, fullMetadata,
                    limit, offset, filters);
          } else if (detailLevel == DetailLevel.SERIES) {
            // Advanced: instead of exporting the organization data we export the data of all organization series
            printOrganizationSeries(provider, resourceId, from, to, dataResolution, index, zoneId, printer, fullMetadata,
                    limit, offset, filters);
          } else {
            printOrganization(provider, resourceId, from, to, dataResolution, zoneId, printer, limit, offset);
          }
          break;
        default:
          throw new IllegalStateException("Unknown resource type: " + provider.getResourceType().name());
      }
    } catch (IOException e) {
      return chuck(e);
    }
    return stringWriter.toString();
  }


  private void printEvent(StatisticsProvider provider, String resourceId, Instant from, Instant to,
                          DataResolution dataResolution, AbstractSearchIndex index, ZoneId zoneId, CSVPrinter printer,
                          boolean fullMetaData, int limit, int offset)
      throws IOException, SearchIndexException, NotFoundException {
    if (offset != 0) {
      return;
    }
    final Opt<Event> event = indexService.getEvent(resourceId, index);
    if (!event.isSome()) {
      throw new NotFoundException("Event not found in index: " + resourceId);
    }
    final TimeSeries dataEvent = statisticsService.getTimeSeriesData(provider, resourceId, from, to, dataResolution, zoneId);
    if (fullMetaData) {
      this.printFullEventData(printer, dataEvent, dataResolution, resourceId, zoneId, true);
    } else {
      printData(printer, dataEvent, dataResolution, resourceId, event.get().getTitle(), zoneId, true);
    }
  }

  private void printSeries(StatisticsProvider provider, String resourceId, Instant from, Instant to,
                           DataResolution dataResolution, AbstractSearchIndex index, ZoneId zoneId, CSVPrinter printer,
                           boolean fullMetadata, int limit, int offset)
          throws SearchIndexException, NotFoundException, IOException {
    if (offset != 0) {
      return;
    }
    final Opt<Series> series = indexService.getSeries(resourceId, index);
    if (!series.isSome()) {
      throw new NotFoundException("Series not found in index: " + resourceId);
    }
    final TimeSeries dataSeries = statisticsService.getTimeSeriesData(provider, resourceId, from, to, dataResolution, zoneId);
    if (fullMetadata) {
      this.printFullSeriesData(printer, dataSeries, dataResolution, resourceId, zoneId, true);
    } else {
      printData(printer, dataSeries, dataResolution, resourceId, series.get().getTitle(), zoneId, true);
    }
  }

  private void printSeriesEvents(StatisticsProvider provider, String resourceId, Instant from, Instant to,
                                 DataResolution dataResolution, AbstractSearchIndex index, ZoneId zoneId, CSVPrinter printer,
                                 boolean fullMetadata, int limit, int offset, Map<String, String> filters)
      throws SearchIndexException, IOException {
    final String eventProviderId = seriesToEventProviderMapping.get(provider.getId());
    final StatisticsProvider eventProvider = statisticsService.getProvider(eventProviderId)
        .orElseThrow(() -> new IllegalStateException("The configured provider " + eventProviderId + " is not available."));
    EventSearchQuery query = (EventSearchQuery) new EventSearchQuery(securityService.getOrganization().getId(),
            securityService.getUser()).withSeriesId(resourceId).withLimit(limit).withOffset(offset);
    for (Map.Entry<String, String> filter: filters.entrySet()) {
      query = (EventSearchQuery) applyFilter(filter.getKey(), filter.getValue(), query);
    }

    final SearchResult<Event> result = index.getByQuery(query);
    boolean first = offset == 0;
    for (SearchResultItem<Event> currentEvent : result.getItems()) {
      final TimeSeries dataEvent = statisticsService.getTimeSeriesData(eventProvider,
          currentEvent.getSource().getIdentifier(), from, to, dataResolution, zoneId);
      if (fullMetadata) {
        this.printFullEventData(printer, dataEvent, dataResolution, currentEvent.getSource().getIdentifier(), zoneId, first);
      } else {
        printData(printer, dataEvent, dataResolution, currentEvent.getSource().getIdentifier(),
                currentEvent.getSource().getTitle(), zoneId, first);
      }
      first = false;
    }
  }

  private void printOrganization(StatisticsProvider provider, String resourceId, Instant from, Instant to,
                                 DataResolution dataResolution, ZoneId zoneId, CSVPrinter printer, int limit, int offset)
      throws UnauthorizedException, IOException {
    if (offset != 0) {
      return;
    }
    final Organization organization = securityService.getOrganization();
    if (!resourceId.equals(organization.getId())) {
      throw new UnauthorizedException("Can only export CSV statistics for own organization.");
    }
    final TimeSeries dataOrg = statisticsService.getTimeSeriesData(provider, resourceId, from, to, dataResolution, zoneId);
    printData(printer, dataOrg, dataResolution, resourceId, organization.getName(), zoneId, true);
  }

  private void printOrganizationEvents(StatisticsProvider provider, String resourceId, Instant from, Instant to,
                                 DataResolution dataResolution, AbstractSearchIndex index, ZoneId zoneId,
                                 CSVPrinter printer, boolean fullMetadata, int limit, int offset, Map<String, String> filters)
          throws UnauthorizedException, SearchIndexException, IOException {

    final Organization organization = securityService.getOrganization();
    if (!resourceId.equals(organization.getId())) {
      throw new UnauthorizedException("Can only export CSV statistics for own organization.");
    }

    final String eventProviderId = organizationToEventProviderMapping.get(provider.getId());
    final StatisticsProvider eventProvider = statisticsService.getProvider(eventProviderId)
            .orElseThrow(() -> new IllegalStateException("The configured provider " + eventProviderId + " is not available."));
    EventSearchQuery query = (EventSearchQuery) new EventSearchQuery(securityService.getOrganization().getId(),
            securityService.getUser()).withLimit(limit).withOffset(offset);
    for (Map.Entry<String, String> filter: filters.entrySet()) {
      query = (EventSearchQuery) applyFilter(filter.getKey(), filter.getValue(), query);
    }
    final SearchResult<Event> result = index.getByQuery(query);
    boolean first = offset == 0;
    for (SearchResultItem<Event> currentEvent : result.getItems()) {
      final TimeSeries dataEvent = statisticsService.getTimeSeriesData(eventProvider,
              currentEvent.getSource().getIdentifier(), from, to, dataResolution, zoneId);
      if (fullMetadata) {
        this.printFullEventData(printer, dataEvent, dataResolution, currentEvent.getSource().getIdentifier(), zoneId, first);
      } else {
        printData(printer, dataEvent, dataResolution, currentEvent.getSource().getIdentifier(),
                currentEvent.getSource().getTitle(), zoneId, first);
      }
      first = false;
    }
  }


  private void printOrganizationSeries(StatisticsProvider provider, String resourceId, Instant from, Instant to,
                                       DataResolution dataResolution, AbstractSearchIndex index, ZoneId zoneId,
                                       CSVPrinter printer, boolean fullMetadata, int limit, int offset, Map<String, String> filters)
          throws UnauthorizedException, SearchIndexException, IOException {

    final Organization organization = securityService.getOrganization();
    if (!resourceId.equals(organization.getId())) {
      throw new UnauthorizedException("Can only export CSV statistics for own organization.");
    }

    final String seriesProviderId = organizationToSeriesProviderMapping.get(provider.getId());
    final StatisticsProvider seriesProvider = statisticsService.getProvider(seriesProviderId)
            .orElseThrow(() -> new IllegalStateException("The configured provider " + seriesProviderId + " is not available."));

    SeriesSearchQuery query = (SeriesSearchQuery) new SeriesSearchQuery(securityService.getOrganization().getId(),
            securityService.getUser()).withLimit(limit).withOffset(offset);
    for (Map.Entry<String, String> filter: filters.entrySet()) {
      query = (SeriesSearchQuery) applyFilter(filter.getKey(), filter.getValue(), query);
    }
    final SearchResult<Series> result = index.getByQuery(query);
    boolean first = offset == 0;
    for (SearchResultItem<Series> currentSeries : result.getItems()) {
      final TimeSeries dataEvent = statisticsService.getTimeSeriesData(seriesProvider,
              currentSeries.getSource().getIdentifier(), from, to, dataResolution, zoneId);
      if (fullMetadata) {
        this.printFullSeriesData(printer, dataEvent, dataResolution, currentSeries.getSource().getIdentifier(), zoneId, first);
      } else {
        printData(printer, dataEvent, dataResolution, currentSeries.getSource().getIdentifier(),
                currentSeries.getSource().getTitle(), zoneId, first);
      }
      first = false;
    }
  }


  private static void printData(
      CSVPrinter printer,
      TimeSeries data,
      DataResolution dataResolution,
      String resourceId,
      String title,
      ZoneId zoneId,
      boolean printHeader) throws IOException {
    if (printHeader) {
      printer.printRecord(header);
    }
    for (int i = 0; i < data.getLabels().size(); i++) {
      printer.printRecord(
          resourceId,
          title,
          formatDate(data.getLabels().get(i), dataResolution, zoneId),
          data.getValues().get(i)
      );
    }
  }

  private static void printFullData(
          CSVPrinter printer,
          TimeSeries data,
          DataResolution dataResolution,
          String resourceId,
          ZoneId zoneId,
          List<MetadataField> mdfs) throws IOException {
    for (int i = 0; i < data.getLabels().size(); i++) {
      List<Object> values = new ArrayList<>();
      values.add(resourceId);
      values.addAll(mdfs.stream().map(f -> f.getValue().getOr("")).collect(Collectors.toList()));
      values.add(formatDate(data.getLabels().get(i), dataResolution, zoneId));
      values.add(data.getValues().get(i));
      printer.printRecord(values.toArray());
    }
  }

  private void printFullEventData(
          CSVPrinter printer,
          TimeSeries data,
          DataResolution dataResolution,
          String resourceId,
          ZoneId zoneId,
          boolean printHeader) throws IOException {
    final List<MetadataField> mdfs = getEventMetadata(resourceId);
    if (printHeader) {
      printer.printRecord(getFullHeader(mdfs));
    }
    printFullData(printer, data, dataResolution, resourceId, zoneId, mdfs);
  }

  private void printFullSeriesData(
          CSVPrinter printer,
          TimeSeries data,
          DataResolution dataResolution,
          String resourceId,
          ZoneId zoneId,
          boolean printHeader) throws IOException {
    final List<MetadataField> mdfs = getSeriesMetadata(resourceId);
    if (printHeader) {
      printer.printRecord(getFullHeader(mdfs));
    }
    printFullData(printer, data, dataResolution, resourceId, zoneId, mdfs);
  }

  private static List<String> getFullHeader(List<MetadataField> mdfs) {
    final List<String> header = new ArrayList<>();
    header.add("ID");
    header.addAll(mdfs.stream().map(MetadataField::getInputID).collect(Collectors.toList()));
    header.add("Date");
    header.add("Value");
    return header;
  }

  private List<MetadataField> getSeriesMetadata(String resourceId) {
    final List<MetadataCollection> mdcs = this.indexService.getSeriesCatalogUIAdapters()
            .stream()
            .filter(a -> !a.equals(this.indexService.getCommonSeriesCatalogUIAdapter()))
            .filter(a -> !a.getFlavor().equals(this.indexService.getCommonSeriesCatalogUIAdapter().getFlavor()))
            .map(adapter -> adapter.getFields(resourceId))
            .filter(Opt::isSome)
            .map(Opt::get)
            .collect(Collectors.toList());
    if (this.indexService.getCommonSeriesCatalogUIAdapter().getFields(resourceId).isSome()) {
      mdcs.add(0, this.indexService.getCommonSeriesCatalogUIAdapter().getFields(resourceId).get());
    }
    return mdcs.stream()
            .map(MetadataCollection::getFields)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
  }

  private List<MetadataField> getEventMetadata(String resourceId) {
    final Opt<MediaPackage> optMp = this.assetManager.getMediaPackage(resourceId);
    if (optMp.isEmpty()) {
      return Collections.emptyList();
    }
    final List<MetadataCollection> mdcs = this.indexService.getEventCatalogUIAdapters()
            .stream()
            .filter(a -> !a.equals(this.indexService.getCommonEventCatalogUIAdapter()))
            .filter(a -> !a.getFlavor().equals(this.indexService.getCommonEventCatalogUIAdapter().getFlavor()))
            .map(adapter -> adapter.getFields(optMp.get()))
            .collect(Collectors.toList());
    mdcs.add(0, this.indexService.getCommonEventCatalogUIAdapter().getFields(optMp.get()));
    return mdcs.stream()
            .map(MetadataCollection::getFields)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
  }

  private static SearchQuery applyFilter(final String name, final String value, final EventSearchQuery query) {
    if ("presenters".equals(name)) {
      return query.withPresenter(value);
    } else if ("creator".equals(name)) {
      return query.withCreator(value);
    } else if ("contributors".equals(name)) {
      return query.withContributor(value);
    } else if ("location".equals(name)) {
      return query.withLocation(value);
    } else if ("textFilter".equals(name)) {
      return query.withText("*" + value + "*");
    } else if ("series".equals(name)) {
      return query.withSeriesId(value);
    } else if ("subject".equals(name)) {
      return query.withSubject(value);
    } else if ("title".equals(name)) {
      return query.withTitle(value);
    } else if ("description".equals(name)) {
      return query.withDescription(value);
    } else if ("series_name".equals(name)) {
      return query.withSeriesName(value);
    } else if ("language".equals(name)) {
      return query.withLanguage(value);
    } else if ("created".equals(name)) {
      return query.withCreated(value);
    } else if ("license".equals(name)) {
      return query.withLicense(value);
    } else if ("rightsholder".equals(name)) {
      return query.withRights(value);
    } else if ("is_part_of".equals(name)) {
      return query.withSeriesId(value);
    } else if ("source".equals(name)) {
      return query.withSource(value);
    } else if ("status".equals(name)) {
      return query.withEventStatus(value);
    } else if ("agent_id".equals(name)) {
      return query.withAgentId(value);
    } else if ("publisher".equals(name)) {
      return query.withPublisher(value);
    } else {
      throw new IllegalArgumentException("Unknown filter :" + name);
    }
  }

  private static SearchQuery applyFilter(final String name, final String value, final SeriesSearchQuery query) {
    if ("contributors".equals(name)) {
      return query.withContributor(value);
    } else if ("creator".equals(name)) {
      return query.withCreator(value);
    } else if ("textFilter".equals(name)) {
      return query.withText("*" + value + "*");
    } else if ("subject".equals(name)) {
      return query.withSubject(value);
    } else if ("title".equals(name)) {
      return query.withTitle(value);
    } else if ("description".equals(name)) {
      return query.withDescription(value);
    } else if ("language".equals(name)) {
      return query.withLanguage(value);
    } else if ("license".equals(name)) {
      return query.withLicense(value);
    } else if ("publisher".equals(name)) {
      return query.withPublisher(value);
    } else if ("organizer".equals(name)) {
      return query.withOrganizer(value);
    } else if ("rightsholder".equals(name)) {
      return query.withRightsHolder(value);
    } else {
      throw new IllegalArgumentException("Unknown filter :" + name);
    }
  }


}
