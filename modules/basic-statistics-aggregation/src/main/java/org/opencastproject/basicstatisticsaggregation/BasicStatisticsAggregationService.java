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
package org.opencastproject.basicstatisticsaggregation;

import org.opencastproject.basicstatistics.ItemType;
import org.opencastproject.basicstatistics.RawEvent;
import org.opencastproject.basicstatistics.persistence.BasicStatisticsDatabaseException;
import org.opencastproject.basicstatistics.persistence.BasicStatisticsDatabaseService;
import org.opencastproject.basicstatisticsaggregation.persistence.BasicStatisticsAggregationDatabaseException;
import org.opencastproject.basicstatisticsaggregation.persistence.BasicStatisticsAggregationDatabaseService;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Continuously (re-)aggregates raw basic-statistics events into hourly view/watchtime buckets.
 *
 * Must only run on a single node in the cluster. Each tick finds which items have new raw activity since the
 * last tick, then fully re-scans and recomputes each affected item's day from the complete raw event log (raw
 * events are immutable, so this is always safe/idempotent) rather than trying to incrementally track partial
 * state across ticks.
 */
@Component(
    property = {
        "service.description=Basic Statistics Aggregation Service",
    },
    immediate = true,
    service = BasicStatisticsAggregationService.class
)
public class BasicStatisticsAggregationService {

  /** How often the aggregation tick runs. */
  private static final long TICK_INTERVAL_MILLIS = 2 * 60 * 1000;

  private static final Logger logger = LoggerFactory.getLogger(BasicStatisticsAggregationService.class);

  private Scheduler quartz = null;

  private BasicStatisticsDatabaseService rawEventDatabase;
  private BasicStatisticsAggregationDatabaseService aggregationDatabase;
  private ElasticsearchIndex elasticsearchIndex;
  private OrganizationDirectoryService organizationDirectoryService;
  private SecurityService securityService;

  private DurationLookup durationLookup;

  @Reference
  public void setBasicStatisticsDatabaseService(BasicStatisticsDatabaseService rawEventDatabase) {
    this.rawEventDatabase = rawEventDatabase;
  }

  @Reference
  public void setBasicStatisticsAggregationDatabaseService(
      BasicStatisticsAggregationDatabaseService aggregationDatabase) {
    this.aggregationDatabase = aggregationDatabase;
  }

  @Reference
  public void setElasticsearchIndex(ElasticsearchIndex elasticsearchIndex) {
    this.elasticsearchIndex = elasticsearchIndex;
  }

  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Activate
  public void activate(ComponentContext cc) {
    try {
      String systemUserName = SecurityUtil.getSystemUserName(cc);
      durationLookup = new DurationLookup(elasticsearchIndex, organizationDirectoryService, systemUserName);

      quartz = new StdSchedulerFactory().getScheduler();
      quartz.start();

      JobDetail job = new JobDetail();
      job.setName("basicstatistics-aggregation-tick");
      job.setJobClass(AggregationTickJob.class);
      job.getJobDataMap().put("service", this);

      SimpleTrigger trigger = new SimpleTrigger();
      trigger.setName("basicstatistics-aggregation-tick-trigger");
      trigger.setStartTime(new Date());
      trigger.setRepeatInterval(TICK_INTERVAL_MILLIS);
      trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);

      quartz.scheduleJob(job, trigger);
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @Deactivate
  public void deactivate() {
    if (quartz != null) {
      try {
        quartz.shutdown(true);
      } catch (SchedulerException e) {
        logger.warn("Unable to shut down Quartz scheduler", e);
      }
    }
  }

  /**
   * Get the all-time view/watchtime total for an item, scoped to the current request's organization.
   */
  public AggregatedTotal getTotal(ItemType itemType, String itemId)
          throws BasicStatisticsAggregationDatabaseException {
    return aggregationDatabase.getTotal(securityService.getOrganization().getId(), itemType, itemId);
  }

  /**
   * Get the hourly view/watchtime buckets for an item over the given (inclusive) day range, scoped to the current
   * request's organization. Only days with at least one recorded view are included.
   */
  public List<AggregatedEvent> getAggregatedEvents(ItemType itemType, String itemId, LocalDate from, LocalDate to)
          throws BasicStatisticsAggregationDatabaseException {
    return aggregationDatabase.getAggregatedEvents(securityService.getOrganization().getId(), itemType, itemId,
        from, to);
  }

  /**
   * Re-aggregate one item over the given day range (or, if {@code from}/{@code to} are null, all recorded
   * history), scoped to the current request's organization.
   *
   * @return the number of days recomputed
   */
  public int reaggregate(String itemId, LocalDate from, LocalDate to)
          throws BasicStatisticsDatabaseException, BasicStatisticsAggregationDatabaseException {
    String organization = securityService.getOrganization().getId();
    Instant rangeStart = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.EPOCH;
    Instant rangeEnd = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.now();

    List<RawEvent> events = rawEventDatabase.getRawEvents(organization, ItemType.VIDEO, itemId, rangeStart,
        rangeEnd);

    Set<ItemDay> touched = new HashSet<>();
    for (RawEvent event : events) {
      touched.add(new ItemDay(organization, ItemType.VIDEO, itemId,
          event.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate()));
    }

    for (ItemDay day : touched) {
      reaggregateDay(day);
    }

    return touched.size();
  }

  /**
   * Run one aggregation tick: find items with new raw video activity since the last tick, and fully recompute the
   * affected days.
   */
  void runTick() {
    try {
      Instant cursor = aggregationDatabase.getCursor();
      List<RawEvent> newEvents = rawEventDatabase.getRawEventsSince(ItemType.VIDEO, cursor);
      if (newEvents.isEmpty()) {
        return;
      }

      Set<ItemDay> touched = new HashSet<>();
      Instant maxTimestamp = cursor;
      for (RawEvent event : newEvents) {
        touched.add(new ItemDay(event.getOrganization(), event.getItemType(), event.getItemId(),
            event.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate()));
        if (event.getTimestamp().isAfter(maxTimestamp)) {
          maxTimestamp = event.getTimestamp();
        }
      }

      for (ItemDay itemDay : touched) {
        reaggregateDay(itemDay);
      }

      aggregationDatabase.setCursor(maxTimestamp);
    } catch (BasicStatisticsDatabaseException | BasicStatisticsAggregationDatabaseException e) {
      logger.warn("Failed to run basic statistics aggregation tick", e);
    }
  }

  private void reaggregateDay(ItemDay itemDay)
          throws BasicStatisticsDatabaseException, BasicStatisticsAggregationDatabaseException {
    Instant dayStart = itemDay.day().atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant dayEnd = dayStart.plusSeconds(24 * 60 * 60);

    List<RawEvent> dayEvents = rawEventDatabase.getRawEvents(itemDay.organization(), itemDay.itemType(),
        itemDay.itemId(), dayStart, dayEnd);
    HourlyBuckets recomputed = ViewCorrelator.correlate(dayEvents, itemDay.organization(), itemDay.itemId(),
        durationLookup);

    AggregatedEvent existing = aggregationDatabase.getAggregatedEvent(itemDay.organization(), itemDay.itemType(),
        itemDay.itemId(), itemDay.day());
    long previousViews = existing != null ? Arrays.stream(existing.getViews()).sum() : 0;
    long previousWatchtime = existing != null ? Arrays.stream(existing.getWatchtime()).sum() : 0;

    AggregatedEvent updated = existing != null
        ? existing
        : new AggregatedEvent(itemDay.organization(), itemDay.itemType(), itemDay.itemId(), itemDay.day());
    updated.setViews(recomputed.getViews());
    updated.setWatchtime(recomputed.getWatchtime());
    aggregationDatabase.updateAggregatedEvent(updated);

    long deltaViews = Arrays.stream(recomputed.getViews()).sum() - previousViews;
    long deltaWatchtime = Arrays.stream(recomputed.getWatchtime()).sum() - previousWatchtime;
    if (deltaViews != 0 || deltaWatchtime != 0) {
      aggregationDatabase.addToTotal(itemDay.organization(), itemDay.itemType(), itemDay.itemId(), deltaViews,
          deltaWatchtime);
    }
  }

  /**
   * Used only within a single {@link #runTick()} call to collect the distinct set of item-days that need
   * re-aggregating.
   */
  private record ItemDay(String organization, ItemType itemType, String itemId, LocalDate day) {
  }

  public static class AggregationTickJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
      BasicStatisticsAggregationService service =
          (BasicStatisticsAggregationService) context.getMergedJobDataMap().get("service");
      service.runTick();
    }
  }
}
