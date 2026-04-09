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
package org.opencastproject.speechtotext.async.persistence;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.job.jpa.JpaJob;
import org.opencastproject.speechtotext.async.api.SpeechToTextAsyncException;

import org.apache.commons.lang3.tuple.Pair;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

@Component(property = { "service.description=Speech-to-Text Persistence" }, immediate = true, service = {
        SpeechToTextDatabase.class })
public class SpeechToTextDatabase {
  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(SpeechToTextDatabase.class);

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.speechtotext.async.persistence";

  /** Factory used to create entity managers for transactions */
  protected EntityManagerFactory emf;
  protected DBSessionFactory dbSessionFactory;
  protected DBSession db;

  @Activate
  public void activate(ComponentContext cc) {
    db = dbSessionFactory.createSession(emf);
    logger.info("Activated!");
  }

  @Deactivate
  public void deactivate() {
    db.close();
  }

  @Reference(target = "(osgi.unit.name=org.opencastproject.speechtotext.async.persistence)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  public SpeechToTextControl storeSpeechToTextControl(String mediaPackageId, long workflowId, JpaJob job)
          throws SpeechToTextAsyncException {
    Date created = job.getDateCreated() != null ? job.getDateCreated() : new Date();
    return storeSpeechToTextControl(
            new SpeechToTextControl(mediaPackageId, workflowId, job, created, SpeechToTextControl.Status.InProgress));
  }

  protected SpeechToTextControl storeSpeechToTextControl(SpeechToTextControl entity) throws SpeechToTextAsyncException {
    try {
      return db.execTx(em -> {
        em.persist(entity);
        return entity;
      });
    } catch (Exception e) {
      throw new SpeechToTextAsyncException(e);
    }
  }

  public SpeechToTextControl findByJob(JpaJob job) throws SpeechToTextAsyncException {
    try {
      return db.exec(em -> {
        return namedQuery.findOpt("SpeechToTextControl.findByJob", SpeechToTextControl.class, Pair.of("job", job))
                .apply(em).orElse(null);
      });
    } catch (Exception e) {
      throw new SpeechToTextAsyncException(e);
    }
  }

  /**
   * Returns a list of STT controls that are in one of the status passed.
   *
   * @param status
   *          Status list to search
   */
  public List<SpeechToTextControl> findByStatus(SpeechToTextControl.Status... status)
          throws SpeechToTextAsyncException {
    Collection<SpeechToTextControl.Status> statusCol = Arrays.stream(status)
            .collect(Collectors.toCollection(HashSet::new));
    try {
      return db.exec(em -> {
        return namedQuery
                .findAll("SpeechToTextControl.findByStatus", SpeechToTextControl.class, Pair.of("status", statusCol))
                .apply(em);
      });
    } catch (Exception e) {
      throw new SpeechToTextAsyncException(e);
    }
  }

  /**
   * Returns a list of STT controls that were created in the scope of a specific workflow.
   *
   * @param workflowId
   * @return
   * @throws SpeechToTextAsyncException
   */
  public List<SpeechToTextControl> findByWorkflowId(long workflowId) throws SpeechToTextAsyncException {
    try {
      return db.exec(em -> {
        return namedQuery.findAll("SpeechToTextControl.findByWorkflowId", SpeechToTextControl.class,
                Pair.of("workflowId", workflowId)).apply(em);
      });
    } catch (Exception e) {
      throw new SpeechToTextAsyncException(e);
    }
  }

  public List<Long> findDistinctWorkflowIdByStatus(SpeechToTextControl.Status... status)
          throws SpeechToTextAsyncException {
    Collection<SpeechToTextControl.Status> statusCol = Arrays.stream(status)
            .collect(Collectors.toCollection(HashSet::new));

    try {
      return db.exec(em -> {
        return namedQuery
                .findAll("SpeechToTextControl.findDistinctWorkflowIdByStatus", Long.class, Pair.of("status", statusCol))
                .apply(em);
      });
    } catch (Exception e) {
      throw new SpeechToTextAsyncException(e);
    }
  }

  public List<Long> findDistinctWorkflowIdByMediaPackageId(String mpId) throws SpeechToTextAsyncException {
    try {
      return db.exec(em -> {
        return namedQuery
                .findAll("SpeechToTextControl.findDistinctWorkflowIdByMediaPackage", Long.class, Pair.of("mpId", mpId))
                .apply(em);
      });
    } catch (Exception e) {
      throw new SpeechToTextAsyncException(e);
    }
  }

  public int updateStatusByJob(SpeechToTextControl.Status status, JpaJob... jobs) throws SpeechToTextAsyncException {
    try {
      return db.execTx(em -> {
        Query q = em.createNamedQuery("SpeechToTextControl.updateStatusByJob");
        q.setParameter("status", status);
        q.setParameter("jobs", Arrays.asList(jobs));
        return q.executeUpdate();
      });
    } catch (Exception e) {
      throw new SpeechToTextAsyncException(e);
    }
  }

  public int deleteByWorkflow(long workflowId)
          throws SpeechToTextAsyncException {
    try {
      return db.execTx(em -> {
        Query q = em.createNamedQuery("SpeechToTextControl.deleteByWorkflow");
        q.setParameter("workflowId", workflowId);
        return q.executeUpdate();
      });
    } catch (Exception e) {
      throw new SpeechToTextAsyncException(e);
    }
  }

  public int transitionStatusByDate(SpeechToTextControl.Status newStatus, Date olderThan,
          SpeechToTextControl.Status... oldStatus) throws SpeechToTextAsyncException {
    try {
      logger.debug("Changing status of stt controls older than {} from {} to {}", olderThan, Arrays.toString(oldStatus),
              newStatus);
      return db.execTx(em -> {
        Query q = em.createNamedQuery("SpeechToTextControl.updateStatusByStatusAndDate");
        q.setParameter("oldStatus", Arrays.asList(oldStatus));
        q.setParameter("newStatus", newStatus);
        q.setParameter("date", olderThan);
        return q.executeUpdate();
      });
    } catch (Exception e) {
      throw new SpeechToTextAsyncException(e);
    }
  }

}
