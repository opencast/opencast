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
package org.opencastproject.basicstatistics.persistence;

import org.opencastproject.basicstatistics.RawEvent;
import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.requests.SortCriterion;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

@Component(
    immediate = true,
    service = { BasicStatisticsDatabaseService.class },
    property = {
        "service.description=Basic Statistics Database Service"
    }
)
public class BasicStatisticsDatabaseServiceImpl implements BasicStatisticsDatabaseService {

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.basicstatistics";
  private static final Logger logger = LoggerFactory.getLogger(BasicStatisticsDatabaseServiceImpl.class);
  /** Factory used to create {@link EntityManager}s for transactions */
  private EntityManagerFactory emf;

  private DBSessionFactory dbSessionFactory;
  private DBSession db;

  /** The security service */
  protected SecurityService securityService;

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.basicstatistics)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  /**
   * OSGi callback to set the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  @Reference(name = "security-service")
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for basic statistics");
    db = dbSessionFactory.createSession(emf);
  }

  /**
   * {@inheritDoc}
   * @see BasicStatisticsDatabaseServiceImpl#getRawEvents(int, int, SortCriterion)
   */
  @Override
  public List<RawEvent> getRawEvents(int limit, int offset, SortCriterion sortCriterion)
          throws BasicStatisticsDatabaseException {
    try {
      return db.exec(em -> {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<RawEvent> criteriaQuery = criteriaBuilder.createQuery(RawEvent.class);
        Root<RawEvent> from = criteriaQuery.from(RawEvent.class);
        CriteriaQuery<RawEvent> select = criteriaQuery.select(from);

        if (sortCriterion.getOrder().equals(SortCriterion.Order.Ascending)) {
          criteriaQuery.orderBy(criteriaBuilder.asc(from.get(sortCriterion.getFieldName())));
        } else if (sortCriterion.getOrder().equals(SortCriterion.Order.Descending)) {
          criteriaQuery.orderBy(criteriaBuilder.desc(from.get(sortCriterion.getFieldName())));
        }

        TypedQuery<RawEvent> allQuery = em.createQuery(select);

        allQuery.setMaxResults(limit);
        allQuery.setFirstResult(offset);

        return allQuery.getResultList();
      });
    } catch (Exception e) {
      throw new BasicStatisticsDatabaseException("Error fetching raw events from database", e);
    }
  }

  /**
   * {@inheritDoc}
   * @see BasicStatisticsDatabaseServiceImpl#createRawEvents(List<RawEvent>)
   */
  @Override
  public void createRawEvents(List<RawEvent> events) throws BasicStatisticsDatabaseException {
    try {
      db.execTx(em -> {
        for (RawEvent event : events) {
          em.persist(event);
        }
      });
    } catch (Exception e) {
      throw new BasicStatisticsDatabaseException("Could not persist raw events", e);
    }
  }
}
