/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.oaipmh.harvester;

import org.apache.commons.lang.ArrayUtils;
import org.opencastproject.oaipmh.util.PersistenceEnv;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.List;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

/**
 * Persists the last harvested time of a url.
 */
@Entity
@Table(name = "last_harvested")
@NamedQueries({
    @NamedQuery(name = "findLastHarvested",
        query = "SELECT a.timestamp FROM LastHarvested a WHERE a.url = :url"),
    @NamedQuery(name = "findAll",
        query = "SELECT a FROM LastHarvested a")
})
public class LastHarvested {

  @Id
  @Column(name = "url")
  private String url;

  @Column(name = "timestamp")
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp;

  /**
   * JPA constructor.
   */
  public LastHarvested() {
  }

  public LastHarvested(String url, Date timestamp) {
    this.url = url;
    this.timestamp = timestamp;
  }

  public String getUrl() {
    return url;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public static Option<Date> getLastHarvestDate(PersistenceEnv penv, final String url) {
    return penv.tx(new Function<EntityManager, Option<Date>>() {
      @Override
      public Option<Date> apply(EntityManager em) {
        Query q = em.createNamedQuery("findLastHarvested");
        q.setParameter("url", url);
        try {
          return some((Date) q.getSingleResult());
        } catch (NoResultException e) {
          return none();
        }
      }
    });
  }

  /**
   * Save or update a timestamp.
   */
  public static void update(PersistenceEnv penv, final LastHarvested entity) {
    penv.tx(new Function<EntityManager, Void>() {
      @Override
      public Void apply(EntityManager em) {
        LastHarvested existing = em.find(LastHarvested.class, entity.getUrl());
        if (existing != null) {
          em.persist(em.merge(entity));
        } else {
          em.persist(entity);
        }
        return null;
      }
    });
  }

  /**
   * Remove all URLs from the table that do not exist in <code>keepUrls</code>.
   */
  public static void cleanup(PersistenceEnv penv, final String[] keepUrls) {
    penv.tx(new Function<EntityManager, Void>() {
      @Override
      public Void apply(EntityManager em) {
        List<LastHarvested> lhs = em.createNamedQuery("findAll").getResultList();
        for (LastHarvested lh : lhs) {
          if (!ArrayUtils.contains(keepUrls, lh.getUrl())) {
            em.remove(lh);
          }
        }
        return null;
      }
    });
  }
}
