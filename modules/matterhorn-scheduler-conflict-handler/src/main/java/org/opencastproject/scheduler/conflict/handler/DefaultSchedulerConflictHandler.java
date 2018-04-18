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
package org.opencastproject.scheduler.conflict.handler;

import static org.opencastproject.scheduler.api.ConflictResolution.Strategy.OLD;
import static org.opencastproject.util.OsgiUtil.getOptCfg;

import org.opencastproject.scheduler.api.ConflictHandler;
import org.opencastproject.scheduler.api.ConflictResolution;
import org.opencastproject.scheduler.api.ConflictResolution.Strategy;
import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.impl.ConflictResolutionImpl;

import com.entwinemedia.fn.Fn;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

/**
 * Default implementation of a scheduler conflict handler
 */
public class DefaultSchedulerConflictHandler implements ConflictHandler, ManagedService {

  private static final Logger logger = LoggerFactory.getLogger(DefaultSchedulerConflictHandler.class);

  private static final String CFG_KEY_HANDLER = "handler";

  private Strategy strategy = Strategy.OLD;

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null) {
      logger.info("No configuration available, using defaults");
      return;
    }

    strategy = getOptCfg(properties, CFG_KEY_HANDLER).toOpt().map(toStrategy).getOr(OLD);
    logger.info("Updated scheduler conflict handler configuration to {}", strategy);
  }

  @Override
  public ConflictResolution handleConflict(SchedulerEvent newEvent, SchedulerEvent oldEvent) {
    switch (strategy) {
      case OLD:
        return new ConflictResolutionImpl(Strategy.OLD, oldEvent);
      case NEW:
        return new ConflictResolutionImpl(Strategy.NEW, newEvent);
      default:
        throw new IllegalStateException("No strategy found for " + strategy);
    }
  }

  private static final Fn<String, Strategy> toStrategy = new Fn<String, Strategy>() {
    @Override
    public Strategy apply(String strategy) {
      if (Strategy.OLD.toString().equalsIgnoreCase(strategy)) {
        return Strategy.OLD;
      } else if (Strategy.NEW.toString().equalsIgnoreCase(strategy)) {
        return Strategy.NEW;
      } else {
        logger.warn("No configuration option for {} exists. Use default old", strategy);
        return Strategy.OLD;
      }
    }

  };

}
