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

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.Comment;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.elasticsearch.index.rebuild.AbstractIndexProducer;
import org.opencastproject.elasticsearch.index.rebuild.IndexProducer;
import org.opencastproject.elasticsearch.index.rebuild.IndexRebuildException;
import org.opencastproject.elasticsearch.index.rebuild.IndexRebuildService;
import org.opencastproject.event.comment.EventComment;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Implements permanent storage for event comments.
 */
@Component(
    immediate = true,
    service = { EventCommentDatabaseService.class, IndexProducer.class },
    property = {
        "service.description=Event Comment Database Service"
    }
)
public class EventCommentDatabaseServiceImpl extends AbstractIndexProducer implements EventCommentDatabaseService {
  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(EventCommentDatabaseServiceImpl.class);

  public static final String PERSISTENCE_UNIT = "org.opencastproject.event.comment";

  /** Factory used to create {@link EntityManager}s for transactions */
  private EntityManagerFactory emf;

  private DBSessionFactory dbSessionFactory;
  private DBSession db;

  /** The security service used to retrieve organizations. */
  private OrganizationDirectoryService organizationDirectoryService;

  /** The security service used to run the security context with. */
  private SecurityService securityService;

  /** The user directory service */
  private UserDirectoryService userDirectoryService;

  /** The component context this bundle is running in. */
  private ComponentContext cc;

  /** The elasticsearch indices */
  private ElasticsearchIndex index;

  /** OSGi component activation callback */
  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for event comments");
    this.cc = cc;
    db = dbSessionFactory.createSession(emf);
  }

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.event.comment)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  /**
   * OSGi callback to set the security context to run with.
   *
   * @param securityService
   *          The security service
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi callback to set the user directory service.
   *
   * @param userDirectoryService
   *          the user directory service
   */
  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * OSGi callback to set the organization directory service.
   *
   * @param organizationDirectoryService
   *          the organization directory service
   */
  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /**
   * OSgi callback for the Elasticsearch index.
   *
   * @param index
   *          the Elasticsearch index.
   */
  @Reference
  public void setIndex(ElasticsearchIndex index) {
    this.index = index;
  }

  @Override
  public List<String> getReasons() throws EventCommentDatabaseException {
    try {
      return db.exec(namedQuery.findAll(
          "EventComment.findReasons",
          String.class,
          Pair.of("org", securityService.getOrganization().getId())
      ));
    } catch (Exception e) {
      logger.error("Could not get reasons", e);
      throw new EventCommentDatabaseException(e);
    }
  }

  @Override
  public EventComment getComment(long commentId) throws NotFoundException, EventCommentDatabaseException {
    try {
      Optional<EventCommentDto> event = db.exec(getEventCommentQuery(commentId));
      if (event.isEmpty()) {
        throw new NotFoundException("Event comment with ID " + commentId + " does not exist");
      }
      return event.get().toComment(userDirectoryService);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get event comment {}", commentId, e);
      throw new EventCommentDatabaseException(e);
    }
  }

  @Override
  public void deleteComment(long commentId) throws NotFoundException, EventCommentDatabaseException {
    try {
      EventCommentDto event = db.execTxChecked(em -> {
        Optional<EventCommentDto> eventOpt = getEventCommentQuery(commentId).apply(em);
        if (eventOpt.isEmpty()) {
          throw new NotFoundException("Event comment with ID " + commentId + " does not exist");
        }
        em.remove(eventOpt.get());
        return eventOpt.get();
      });
      updateIndices(event.getEventId());
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete event comment", e);
      throw new EventCommentDatabaseException(e);
    }
  }

  @Override
  public void deleteComments(String eventId) throws NotFoundException, EventCommentDatabaseException {
    // Similar to deleteComment but we want to avoid sending a message for each deletion

    int count = 0;
    try {
      count = db.execTxChecked(em -> {
        List<EventComment> comments = getComments(eventId);

        for (EventComment comment : comments) {
          long commentId = comment.getId().get().intValue();
          Optional<EventCommentDto> event = getEventCommentQuery(commentId).apply(em);
          if (event.isEmpty()) {
            throw new NotFoundException("Event comment with ID " + commentId + " does not exist");
          }
          em.remove(event.get());
        }

        return comments.size();
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete event comments", e);
      throw new EventCommentDatabaseException(e);
    }

    // send updates only if we actually modified anything
    if (count > 0) {
      updateIndices(eventId);
    }
  }

  @Override
  public EventComment updateComment(EventComment comment) throws EventCommentDatabaseException {
    try {
      final EventCommentDto commentDto = EventCommentDto.from(comment);
      final EventComment updatedComment = db.execTx(namedQuery.persistOrUpdate(commentDto))
          .toComment(userDirectoryService);
      updateIndices(updatedComment.getEventId());
      return updatedComment;
    } catch (Exception e) {
      throw new EventCommentDatabaseException(e);
    }
  }

  /**
   * Gets an event comment, using the current organizational context.
   *
   * @param commentId
   *          the comment identifier
   *
   * @return the event comment entity, or null if not found
   */
  private Function<EntityManager, Optional<EventCommentDto>> getEventCommentQuery(long commentId) {
    return namedQuery.findOpt(
        "EventComment.findByCommentId",
        EventCommentDto.class,
        Pair.of("commentId", commentId)
    );
  }

  @Override
  public List<EventComment> getComments(String eventId) throws EventCommentDatabaseException {
    try {
      return db.exec(namedQuery.findAll(
          "EventComment.findByEvent",
              EventCommentDto.class,
              Pair.of("eventId", eventId),
              Pair.of("org", securityService.getOrganization().getId())
          )).stream()
          .map(c -> c.toComment(userDirectoryService))
          .sorted((c1, c2) -> {
            boolean v1 = c1.isResolvedStatus();
            boolean v2 = c2.isResolvedStatus();
            return (v1 ^ v2) ? ((v1 ^ false) ? 1 : -1) : 0;
          })
          .collect(Collectors.toList());
    } catch (Exception e) {
      logger.error("Could not retreive comments for event {}", eventId, e);
      throw new EventCommentDatabaseException(e);
    }
  }

  public Iterator<EventCommentDto> getComments() throws EventCommentDatabaseException {
    try {
      return db.exec(namedQuery.findAll("EventComment.findAll", EventCommentDto.class)).iterator();
    } catch (Exception e) {
      logger.error("Could not retreive event comments", e);
      throw new EventCommentDatabaseException(e);
    }
  }

  public int countComments() throws EventCommentDatabaseException {
    try {
      return db.exec(namedQuery.find("EventComment.countAll", Number.class)).intValue();
    } catch (Exception e) {
      logger.error("Could not find the number of comments.", e);
      throw new EventCommentDatabaseException(e);
    }
  }

  /**
   * Return all known event ID's with existing comments, grouped by organization ID
   *
   * @return a list of all event ID's grouped by organization ID
   */
  public Map<String, List<String>> getEventsWithComments() {
    List<Object[]> orgIDsEventIDs = db.exec(namedQuery.findAll("EventComment.findAllWIthOrg", Object[].class));
    Map<String, List<String>> orgEventsMap = new Hashtable<>();
    for (Object[] orgEventResult : orgIDsEventIDs) {
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

  private void updateIndices(String eventId) throws EventCommentDatabaseException {
    List<EventComment> comments = getComments(eventId);
    boolean hasOpenComments = !Stream.$(comments).filter(filterOpenComments).toList().isEmpty();
    boolean needsCutting = !Stream.$(comments).filter(filterNeedsCuttingComment).toList().isEmpty();

    String organization = securityService.getOrganization().getId();
    User user = securityService.getUser();

    updateIndex(eventId, !comments.isEmpty(), hasOpenComments, comments, needsCutting, organization, user);
  }

  private void updateIndex(String eventId, boolean hasComments, boolean hasOpenComments, List<EventComment> comments,
          boolean needsCutting, String organization, User user) {
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
      if (eventOpt.isEmpty()) {
        logger.debug("Event {} not found for comment status updating", eventId);
        return Optional.empty();
      }
      Event event = eventOpt.get();
      event.setHasComments(hasComments);
      event.setHasOpenComments(hasOpenComments);
      List<Comment> indexComments = new ArrayList<Comment>();
      for (EventComment comment : comments) {
        indexComments.add(new Comment(
                comment.getId().get().toString(), comment.getReason(), comment.getText(), comment.isResolvedStatus()
        ));
        // Do we want to include replies? Maybe not, no good reason to filter for them?
      }
      event.setComments(indexComments);
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
  public void repopulate() throws IndexRebuildException {
    try {
      final int total = countComments();
      logIndexRebuildBegin(logger, index.getIndexName(), total, "events with comment");
      final int[] current = new int[1];
      current[0] = 0;
      int n = 16;
      var updatedEventRange = new ArrayList<Event>();

      final Map<String, List<String>> eventsWithComments = getEventsWithComments();
      for (String orgId : eventsWithComments.keySet()) {
        Organization organization = organizationDirectoryService.getOrganization(orgId);
        User systemUser = SecurityUtil.createSystemUser(cc, organization);
        SecurityUtil.runAs(securityService, organization, systemUser,
                () -> {
                  int i = 0;
                  for (String eventId : eventsWithComments.get(orgId)) {
                    try {
                      current[0] += getComments(eventId).size();
                      i++;

                      var updatedEventData = index.getEvent(eventId, orgId, securityService.getUser());
                      updatedEventData = getEventUpdateFunction(eventId).apply(updatedEventData);
                      updatedEventRange.add(updatedEventData.get());

                      if (updatedEventRange.size() >= n || i >= eventsWithComments.get(orgId).size()) {
                        index.bulkEventUpdate(updatedEventRange);
                        logIndexRebuildProgress(logger, index.getIndexName(), total, current[0]);
                        updatedEventRange.clear();
                      }
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
  /**
   * Get the function to update a commented event in the Elasticsearch index.
   *
   * @param eventId
   *          The id of the current event
   * @return the function to do the update
   */
  private Function<Optional<Event>, Optional<Event>> getEventUpdateFunction(String eventId) {
    return (Optional<Event> eventOpt) -> {
      List<EventComment> comments;
      try {
        if (eventOpt.isEmpty()) {
          logger.debug("Event {} not found for comment status updating", eventId);
          return Optional.empty();
        }
        comments = getComments(eventId);
        Boolean hasComments = !comments.isEmpty();
        Boolean hasOpenComments = !Stream.$(comments).filter(filterOpenComments).toList().isEmpty();
        Boolean needsCutting = !Stream.$(comments).filter(filterNeedsCuttingComment).toList().isEmpty();

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
        Event event = eventOpt.get();
        event.setHasComments(hasComments);
        event.setHasOpenComments(hasOpenComments);
        List<Comment> indexComments = new ArrayList<Comment>();
        for (EventComment comment : comments) {
          indexComments.add(new Comment(
                  comment.getId().get().toString(), comment.getReason(), comment.getText(), comment.isResolvedStatus()
          ));
          // Do we want to include replies? Maybe not, no good reason to filter for them?
        }
        event.setComments(indexComments);
        event.setNeedsCutting(needsCutting);
        return Optional.of(event);
      } catch (EventCommentDatabaseException e) {
        logger.error("Unable to get comments from event {}", eventId, e);
        return Optional.empty();
      }
    };
  }
}
