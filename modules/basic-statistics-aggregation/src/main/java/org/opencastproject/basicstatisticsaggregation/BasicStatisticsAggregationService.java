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

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service for the rotating daily secret used in sessions hashes for Opencast basic statistics
 *
 * Must only run on a single node in the cluster.
 */
@Component(
    property = {
        "service.description=Basic Statistics Aggregation Service",
    },
    immediate = true,
    service = BasicStatisticsAggregationService.class
)
public class BasicStatisticsAggregationService {

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(BasicStatisticsAggregationService.class);

  private Scheduler quartz = null;

  @Activate
  public void activate(ComponentContext cc) {
    try {
      quartz = new StdSchedulerFactory().getScheduler();
      quartz.start();


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


}
