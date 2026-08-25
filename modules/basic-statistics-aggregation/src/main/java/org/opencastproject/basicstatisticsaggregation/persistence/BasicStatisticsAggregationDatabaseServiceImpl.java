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
package org.opencastproject.basicstatisticsaggregation.persistence;

import org.opencastproject.basicstatistics.ItemType;
import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

@Component(
    immediate = true,
    service = { BasicStatisticsAggregationDatabaseService.class },
    property = {
        "service.description=Basic Statistics Aggregation Database Service"
    }
)
public class BasicStatisticsAggregationDatabaseServiceImpl implements BasicStatisticsAggregationDatabaseService {

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.basicstatisticsaggregation";

  private static final Logger logger = LoggerFactory.getLogger(BasicStatisticsAggregationDatabaseServiceImpl.class);

  private EntityManagerFactory emf;
  private DBSessionFactory dbSessionFactory;
  private DBSession db;

  @Reference(target = "(osgi.unit.name=org.opencastproject.basicstatisticsaggregation)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for basic statistics aggregation");
    db = dbSessionFactory.createSession(emf);
  }

  @Override
  public AggregatedEvent getAggregatedEvent(String organization, ItemType itemType, String itemId, LocalDate day)
          throws BasicStatisticsAggregationDatabaseException {
    try {
      return db.exec(em -> {
        return findByDay(em, organization, itemType, itemId, day);
      });
    } catch (Exception e) {
      throw new BasicStatisticsAggregationDatabaseException("Could not fetch aggregated event", e);
    }
  }

  @Override
  public List<AggregatedEvent> getAggregatedEvents(String organization, ItemType itemType, String itemId,
      LocalDate from, LocalDate to) throws BasicStatisticsAggregationDatabaseException {
    try {
      return db.exec(em -> {
        TypedQuery<AggregatedEvent> query = em.createNamedQuery("AggregatedEvent.findByRange",
            AggregatedEvent.class);
        query.setParameter("organization", organization);
        query.setParameter("itemType", itemType);
        query.setParameter("itemId", itemId);
        query.setParameter("from", from);
        query.setParameter("to", to);
        return query.getResultList();
      });
    } catch (Exception e) {
      throw new BasicStatisticsAggregationDatabaseException("Could not fetch aggregated events", e);
    }
  }

  @Override
  public void updateAggregatedEvent(AggregatedEvent event) throws BasicStatisticsAggregationDatabaseException {
    try {
      db.execTx(em -> {
        AggregatedEvent existing = findByDay(em, event.getOrganization(), event.getItemType(), event.getItemId(),
            event.getDay());
        if (existing == null) {
          em.persist(event);
        } else {
          existing.setViews(event.getViews());
          existing.setWatchtime(event.getWatchtime());
        }
      });
    } catch (Exception e) {
      throw new BasicStatisticsAggregationDatabaseException("Could not persist aggregated event", e);
    }
  }

  @Override
  public AggregatedTotal getTotal(String organization, ItemType itemType, String itemId)
          throws BasicStatisticsAggregationDatabaseException {
    try {
      return db.exec(em -> {
        AggregatedTotal total = findTotal(em, organization, itemType, itemId);
        return total != null ? total : new AggregatedTotal(organization, itemType, itemId);
      });
    } catch (Exception e) {
      throw new BasicStatisticsAggregationDatabaseException("Could not fetch aggregated total", e);
    }
  }

  @Override
  public void addToTotal(String organization, ItemType itemType, String itemId, long viewsDelta,
      long watchtimeDelta) throws BasicStatisticsAggregationDatabaseException {
    try {
      db.execTx(em -> {
        AggregatedTotal total = findTotal(em, organization, itemType, itemId);
        if (total == null) {
          total = new AggregatedTotal(organization, itemType, itemId);
          total.setTotalViews(viewsDelta);
          total.setTotalWatchtime(watchtimeDelta);
          em.persist(total);
        } else {
          total.setTotalViews(total.getTotalViews() + viewsDelta);
          total.setTotalWatchtime(total.getTotalWatchtime() + watchtimeDelta);
        }
      });
    } catch (Exception e) {
      throw new BasicStatisticsAggregationDatabaseException("Could not update aggregated total", e);
    }
  }

  private AggregatedEvent findByDay(EntityManager em, String organization, ItemType itemType, String itemId,
      LocalDate day) {
    TypedQuery<AggregatedEvent> query = em.createNamedQuery("AggregatedEvent.findByDay", AggregatedEvent.class);
    query.setParameter("organization", organization);
    query.setParameter("itemType", itemType);
    query.setParameter("itemId", itemId);
    query.setParameter("day", day);
    List<AggregatedEvent> results = query.getResultList();
    return results.isEmpty() ? null : results.get(0);
  }

  private AggregatedTotal findTotal(EntityManager em, String organization, ItemType itemType, String itemId) {
    TypedQuery<AggregatedTotal> query = em.createNamedQuery("AggregatedTotal.find", AggregatedTotal.class);
    query.setParameter("organization", organization);
    query.setParameter("itemType", itemType);
    query.setParameter("itemId", itemId);
    List<AggregatedTotal> results = query.getResultList();
    return results.isEmpty() ? null : results.get(0);
  }
}
