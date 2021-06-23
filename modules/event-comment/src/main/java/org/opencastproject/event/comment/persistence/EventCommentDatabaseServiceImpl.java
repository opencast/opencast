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
package org.opencastproject.event.comment.persistence;

import static org.opencastproject.util.persistencefn.Queries.persistOrUpdate;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.AbstractSearchIndex;
import org.opencastproject.elasticsearch.index.event.Event;
import org.opencastproject.event.comment.EventComment;
import org.opencastproject.index.rebuild.AbstractIndexProducer;
import org.opencastproject.index.rebuild.IndexRebuildException;
import org.opencastproject.index.rebuild.IndexRebuildService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.persistencefn.PersistenceEnv;
import org.opencastproject.util.persistencefn.PersistenceEnvs;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 * Implements permanent storage for event comments.
 */
public class EventCommentDatabaseServiceImpl extends AbstractIndexProducer implements EventCommentDatabaseService {
  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(EventCommentDatabaseServiceImpl.class);

  public static final String PERSISTENCE_UNIT = "org.opencastproject.event.comment";

  /** Factory used to create {@link EntityManager}s for transactions */
  private EntityManagerFactory emf;

  /** Persistence environment */
  private PersistenceEnv env;

  /** The security service used to retrieve organizations. */
  private OrganizationDirectoryService organizationDirectoryService;

  /** The security service used to run the security context with. */
  private SecurityService securityService;

  /** The user directory service */
  private UserDirectoryService userDirectoryService;

  /** The component context this bundle is running in. */
  private ComponentContext cc;

  /** The elasticsearch indices */
  private AbstractSearchIndex adminUiIndex;
  private AbstractSearchIndex externalApiIndex;

  /** OSGi component activation callback */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for event comments");
    this.cc = cc;
  }

  /** OSGi DI */
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
    this.env = PersistenceEnvs.mk(emf);
  }

  /**
   * OSGi callback to set the security context to run with.
   *
   * @param securityService
   *          The security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi callback to set the user directory service.
   *
   * @param userDirectoryService
   *          the user directory service
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * OSGi callback to set the organization directory service.
   *
   * @param organizationDirectoryService
   *          the organization directory service
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /**
   * OSgi callback for the Admin UI index.
   *
   * @param index
   *          the admin UI index.
   */
  public void setAdminUiIndex(AbstractSearchIndex index) {
    this.adminUiIndex = index;
  }

  /**
   * OSGi callback for the External API index
   *
   * @param index
   *          the external API index.
   */
  public void setExternalApiIndex(AbstractSearchIndex index) {
    this.externalApiIndex = index;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> getReasons() throws EventCommentDatabaseException {
    EntityManager em = emf.createEntityManager();
    try {
      Query q = em.createNamedQuery("EventComment.findReasons");
      q.setParameter("org", securityService.getOrganization().getId());
      return q.getResultList();
    } catch (Exception e) {
      logger.error("Could not get reasons", e);
      throw new EventCommentDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public EventComment getComment(long commentId) throws NotFoundException, EventCommentDatabaseException {
    EntityManager em = emf.createEntityManager();
    try {
      EventCommentDto event = getEventComment(commentId, em);
      if (event == null)
        throw new NotFoundException("Event comment with ID " + commentId + " does not exist");

      return event.toComment(userDirectoryService);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get event comment {}", commentId, e);
      throw new EventCommentDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void deleteComment(long commentId) throws NotFoundException, EventCommentDatabaseException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      EventCommentDto event = getEventComment(commentId, em);
      if (event == null)
        throw new NotFoundException("Event comment with ID " + commentId + " does not exist");

      em.remove(event);
      tx.commit();
      updateIndices(event.getEventId());
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete event comment", e);
      if (tx.isActive())
        tx.rollback();

      throw new EventCommentDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void deleteComments(String eventId) throws NotFoundException, EventCommentDatabaseException {

    // Similar to deleteComment but we want to avoid sending a message for each deletion

    int count = 0;
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      List<EventComment> comments = getComments(eventId);
      count = comments.size();

      for (EventComment comment : comments) {
        long commentId = comment.getId().get().intValue();
        EventCommentDto event = getEventComment(commentId, em);
        if (event == null)
          throw new NotFoundException("Event comment with ID " + commentId + " does not exist");

        em.remove(event);
      }
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete event comments", e);
      if (tx.isActive())
        tx.rollback();

      throw new EventCommentDatabaseException(e);
    } finally {
      em.close();
    }

    // send updates only if we actually modified anything
    if (count > 0) {
      updateIndices(eventId);
    }
  }

  @Override
  public EventComment updateComment(EventComment comment) throws EventCommentDatabaseException {
    final EventCommentDto commentDto = EventCommentDto.from(comment);
    final EventComment updatedComment = env.tx(persistOrUpdate(commentDto)).toComment(userDirectoryService);
    updateIndices(updatedComment.getEventId());
    return updatedComment;
  }

  /**
   * Gets an event comment, using the current organizational context.
   *
   * @param commentId
   *          the comment identifier
   * @param em
   *          an open entity manager
   *
   * @return the event comment entity, or null if not found
   */
  private EventCommentDto getEventComment(long commentId, EntityManager em) {
    Query q = em.createNamedQuery("EventComment.findByCommentId");
    q.setParameter("commentId", commentId);
    try {
      return (EventCommentDto) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<EventComment> getComments(String eventId) throws EventCommentDatabaseException {
    EntityManager em = emf.createEntityManager();
    try {
      Query q = em.createNamedQuery("EventComment.findByEvent");
      q.setParameter("eventId", eventId);
      q.setParameter("org", securityService.getOrganization().getId());

      List<EventComment> comments = Monadics.mlist(q.getResultList())
              .map(new org.opencastproject.util.data.Function<EventCommentDto, EventComment>() {
                @Override
                public EventComment apply(EventCommentDto a) {
                  return a.toComment(userDirectoryService);
                }
              }).sort(new Comparator<EventComment>() {
                @Override
                public int compare(EventComment c1, EventComment c2) {
                  boolean v1 = c1.isResolvedStatus();
                  boolean v2 = c2.isResolvedStatus();
                  return (v1 ^ v2) ? ((v1 ^ false) ? 1 : -1) : 0;
                }
              }).value();
      return new ArrayList<>(comments);
    } catch (Exception e) {
      logger.error("Could not retreive comments for event {}", eventId, e);
      throw new EventCommentDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @SuppressWarnings("unchecked")
  public Iterator<EventCommentDto> getComments() throws EventCommentDatabaseException {
    EntityManager em = emf.createEntityManager();
    try {
      Query q = em.createNamedQuery("EventComment.findAll");
      return new ArrayList<EventCommentDto>(q.getResultList()).iterator();
    } catch (Exception e) {
      logger.error("Could not retreive event comments", e);
      throw new EventCommentDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  public int countComments() throws EventCommentDatabaseException {
    EntityManager em = emf.createEntityManager();
    Query query = em.createNamedQuery("EventComment.countAll");
    try {
      Number total = (Number) query.getSingleResult();
      return total.intValue();
    } catch (Exception e) {
      logger.error("Could not find the number of comments.", e);
      throw new EventCommentDatabaseException(e);
    } finally {
      em.close();
    }
  }

  /**
   * Return all known event ID's with existing comments, grouped by organization ID
   *
   * @return a list of all event ID's grouped by organization ID
   */
  public Map<String, List<String>> getEventsWithComments() {
    EntityManager em = emf.createEntityManager();
    Query query = em.createNamedQuery("EventComment.findAllWIthOrg");
    Iterator iter = query.getResultList().iterator();
    Map<String, List<String>> orgEventsMap = new Hashtable<>();
    while (iter.hasNext()) {
      Object[] orgEventResult = (Object[]) iter.next();
      String orgId = (String) orgEventResult[0];
      String eventId = (String) orgEventResult[1];
      if (!orgEventsMap.containsKey(orgId)) {
        List<String> eventIds = new ArrayList<>();
        eventIds.add(eventId);
        orgEventsMap.put(orgId, eventIds);
      } else if (!orgEventsMap.get(orgId).contains(eventId)) {
        orgEventsMap.get(orgId).add(eventId);
      }
    }
    return orgEventsMap;
  }

  private void updateIndices(String eventId)
          throws EventCommentDatabaseException {
    List<EventComment> comments = getComments(eventId);
    boolean hasOpenComments = !Stream.$(comments).filter(filterOpenComments).toList().isEmpty();
    boolean needsCutting = !Stream.$(comments).filter(filterNeedsCuttingComment).toList().isEmpty();

    String organization = securityService.getOrganization().getId();
    User user = securityService.getUser();

    updateIndex(eventId, !comments.isEmpty(), hasOpenComments, needsCutting, organization, user, adminUiIndex);
    updateIndex(eventId, !comments.isEmpty(), hasOpenComments, needsCutting, organization, user, externalApiIndex);
  }

  private void updateIndex(String eventId, boolean hasComments, boolean hasOpenComments, boolean needsCutting,
          String organization, User user, AbstractSearchIndex index) {
    logger.debug("Updating comment status of event {} in the {} index.", eventId, index.getIndexName());
    if (!hasComments && hasOpenComments) {
      throw new IllegalStateException(
              "Invalid comment update request: You can't have open comments without having any comments!");
    }
    if (!hasOpenComments && needsCutting) {
      throw new IllegalStateException(
              "Invalid comment update request: You can't have an needs cutting comment without having any open "
                      + "comments!");
    }

    Function<Optional<Event>, Optional<Event>> updateFunction = (Optional<Event> eventOpt) -> {
      if (!eventOpt.isPresent()) {
        logger.debug("Event {} not found for comment status updating", eventId);
        return Optional.empty();
      }
      Event event = eventOpt.get();
      event.setHasComments(hasComments);
      event.setHasOpenComments(hasOpenComments);
      event.setNeedsCutting(needsCutting);
      return Optional.of(event);
    };

    try {
      index.addOrUpdateEvent(eventId, updateFunction, organization, user);
    } catch (SearchIndexException e) {
      logger.error("Error updating comment status of event {} in the {} index:", eventId, index.getIndexName(), e);
    }
  }

  private static final Fn<EventComment, Boolean> filterOpenComments = new Fn<EventComment, Boolean>() {
    @Override
    public Boolean apply(EventComment comment) {
      return !comment.isResolvedStatus();
    }
  };

  private static final Fn<EventComment, Boolean> filterNeedsCuttingComment = new Fn<EventComment, Boolean>() {
    @Override
    public Boolean apply(EventComment comment) {
      return EventComment.REASON_NEEDS_CUTTING.equals(comment.getReason()) && !comment.isResolvedStatus();
    }
  };

  @Override
  public void repopulate(final AbstractSearchIndex index) throws IndexRebuildException {
    try {
      final int total = countComments();
      final int[] current = new int[1];
      current[0] = 0;
      logIndexRebuildBegin(logger, index.getIndexName(), total, "events with comment");
      final Map<String, List<String>> eventsWithComments = getEventsWithComments();
      for (String orgId : eventsWithComments.keySet()) {
        Organization organization = organizationDirectoryService.getOrganization(orgId);
        User systemUser = SecurityUtil.createSystemUser(cc, organization);
        SecurityUtil.runAs(securityService, organization, systemUser,
                () -> {
                  for (String eventId : eventsWithComments.get(orgId)) {
                    try {
                      List<EventComment> comments = getComments(eventId);
                      boolean hasOpenComments = !Stream.$(comments).filter(filterOpenComments).toList().isEmpty();
                      boolean needsCutting = !Stream.$(comments).filter(filterNeedsCuttingComment).toList().isEmpty();

                      updateIndex(eventId, !comments.isEmpty(), hasOpenComments, needsCutting, orgId, systemUser,
                              index);
                      current[0] += comments.size();
                      logIndexRebuildProgress(logger, index.getIndexName(), total, current[0]);
                    } catch (Throwable t) {
                      logSkippingElement(logger, "comment of event", eventId, organization, t);
                    }
                  }
                });
      }
    } catch (Exception e) {
      logIndexRebuildError(logger, index.getIndexName(), e);
      throw new IndexRebuildException(index.getIndexName(), getService(), e);
    }
  }

  @Override
  public IndexRebuildService.Service getService() {
    return IndexRebuildService.Service.Comments;
  }
}
