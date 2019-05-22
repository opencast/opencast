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

import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.statistics.api.DataResolution;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.api.StatisticsService;
import org.opencastproject.statistics.api.TimeSeries;
import org.opencastproject.statistics.api.TimeSeriesProvider;
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
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class StatisticsExportServiceImpl implements StatisticsExportService, ManagedService {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(StatisticsExportServiceImpl.class);
  private static final String[] header = {"ID", "Name", "Date", "Value"};
  private static final String CFG_KEY_SERIES_TO_EVENT_PROVIDER_MAPPINGS = "series.to.event.provider.mappings";


  private Map<String, String> seriesToEventProviderMapping = new HashMap<>();

  private IndexService indexService;
  private SecurityService securityService;
  private StatisticsService statisticsService;


  @Override
  public void updated(Dictionary<String, ?> dictionary) {
    final String providerMappings = (String) dictionary.get(CFG_KEY_SERIES_TO_EVENT_PROVIDER_MAPPINGS);
    if (providerMappings == null) {
      return;
    }
    seriesToEventProviderMapping = Arrays.stream(providerMappings.split(","))
        .peek(s -> {
          if (!s.contains(":")) {
            throw new ConfigurationException("Missing ':' in mapping from series provider to episode provider: " + s);
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

  private static String formatDate(final String dateStr, DataResolution dataResolution, ZoneId zoneId) {
    final LocalDateTime ldt = LocalDateTime.ofInstant(Instant.parse(dateStr), zoneId);
    DateTimeFormatter formatter = null;
    switch (dataResolution) {
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
    try (CSVPrinter printer = CSVFormat.RFC4180.withHeader(header).print(stringWriter)) {
      switch (provider.getResourceType()) {
        case EPISODE:
          printEvent(provider, resourceId, from, to, dataResolution, index, zoneId, printer);
          break;
        case SERIES:
          if (seriesToEventProviderMapping.containsKey(provider.getId())) {
            // Advanced: instead of exporting the series data we export the data of all series events
            printSeriesEvents(provider, resourceId, from, to, dataResolution, index, zoneId, printer);
          } else {
            // Default: just export series data
            printSeries(provider, resourceId, from, to, dataResolution, index, zoneId, printer);
          }
          break;
        case ORGANIZATION:
          printOrganization(provider, resourceId, from, to, dataResolution, zoneId, printer);
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
                          DataResolution dataResolution, AbstractSearchIndex index, ZoneId zoneId, CSVPrinter printer)
      throws IOException, SearchIndexException, NotFoundException {
    final Opt<Event> event = indexService.getEvent(resourceId, index);
    if (!event.isSome()) {
      throw new NotFoundException("Event not found in index: " + resourceId);
    }
    final TimeSeries dataEvent = statisticsService.getTimeSeriesData(provider, resourceId, from, to, dataResolution, zoneId);
    printData(printer, dataEvent, dataResolution, resourceId, event.get().getTitle(), zoneId);
  }

  private void printSeries(StatisticsProvider provider, String resourceId, Instant from, Instant to,
                           DataResolution dataResolution, AbstractSearchIndex index, ZoneId zoneId, CSVPrinter printer)
      throws SearchIndexException, NotFoundException, IOException {
    final Opt<Series> series = indexService.getSeries(resourceId, index);
    if (!series.isSome()) {
      throw new NotFoundException("Series not found in index: " + resourceId);
    }
    final TimeSeries dataSeries = statisticsService.getTimeSeriesData(provider, resourceId, from, to, dataResolution, zoneId);
    printData(printer, dataSeries, dataResolution, resourceId, series.get().getTitle(), zoneId);
  }

  private void printSeriesEvents(StatisticsProvider provider, String resourceId, Instant from, Instant to,
                                 DataResolution dataResolution, AbstractSearchIndex index, ZoneId zoneId, CSVPrinter printer)
      throws SearchIndexException, IOException {
    final String eventProviderId = seriesToEventProviderMapping.get(provider.getId());
    final StatisticsProvider eventProvider = statisticsService.getProvider(eventProviderId)
        .orElseThrow(() -> new IllegalStateException("The configured provider " + eventProviderId + "is not available."));
    final EventSearchQuery query =
        new EventSearchQuery(securityService.getOrganization().getId(), securityService.getUser()).withSeriesId(resourceId);
    final SearchResult<Event> result = index.getByQuery(query);
    for (SearchResultItem<Event> currentEvent : result.getItems()) {
      final TimeSeries dataEvent = statisticsService.getTimeSeriesData(eventProvider,
          currentEvent.getSource().getIdentifier(), from, to, dataResolution, zoneId);
      printData(printer, dataEvent, dataResolution, currentEvent.getSource().getIdentifier(),
          currentEvent.getSource().getTitle(), zoneId);
    }
  }

  private void printOrganization(StatisticsProvider provider, String resourceId, Instant from, Instant to,
                                 DataResolution dataResolution, ZoneId zoneId, CSVPrinter printer)
      throws UnauthorizedException, IOException {
    final Organization organization = securityService.getOrganization();
    if (!resourceId.equals(organization.getId())) {
      throw new UnauthorizedException("Can only export CSV statistics for own organization.");
    }
    final TimeSeries dataOrg = statisticsService.getTimeSeriesData(provider, resourceId, from, to, dataResolution, zoneId);
    printData(printer, dataOrg, dataResolution, resourceId, organization.getName(), zoneId);
  }


  private static void printData(
      CSVPrinter printer,
      TimeSeries data,
      DataResolution dataResolution,
      String resourceId,
      String title,
      ZoneId zoneId) throws IOException {
    for (int i = 0; i < data.getLabels().size(); i++) {
      printer.printRecord(
          resourceId,
          title,
          formatDate(data.getLabels().get(i), dataResolution, zoneId),
          data.getValues().get(i)
      );
    }
  }
}
