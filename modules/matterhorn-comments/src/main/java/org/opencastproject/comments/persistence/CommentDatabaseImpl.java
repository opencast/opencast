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
package org.opencastproject.comments.persistence;

import org.opencastproject.comments.Comment;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.spi.PersistenceProvider;

/**
 * Implements {@link SeriesServiceDatabase}. Defines permanent storage for series.
 */
public class CommentDatabaseImpl implements CommentDatabase {

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(CommentDatabaseImpl.class);

  private static final String PERSISTENCE_UNIT = "org.opencastproject.comments";

  /** Persistence provider set by OSGi */
  protected PersistenceProvider persistenceProvider;

  /** Persistence properties used to create {@link EntityManagerFactory} */
  protected Map<String, Object> persistenceProperties;

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   * 
   * @param cc
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for comments");
    emf = persistenceProvider.createEntityManagerFactory(PERSISTENCE_UNIT, persistenceProperties);
  }

  /**
   * Closes entity manager factory.
   * 
   * @param cc
   */
  public void deactivate(ComponentContext cc) {
    emf.close();
  }

  /**
   * OSGi callback to set persistence properties.
   * 
   * @param persistenceProperties
   *          persistence properties
   */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /**
   * OSGi callback to set persistence provider.
   * 
   * @param persistenceProvider
   *          {@link PersistenceProvider} object
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  @Override
  public void deleteComment(long commentId) throws CommentDatabaseException, NotFoundException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      Option<CommentDto> dto = CommentDatabaseUtils.find(Option.some(commentId), em, CommentDto.class);
      if (dto.isNone())
        throw new NotFoundException("Comment with ID " + commentId + " does not exist");

      CommentDatabaseUtils.deleteReplies(dto.get().getReplies(), em);

      dto.get().setReplies(new ArrayList<CommentReplyDto>());
      em.remove(dto.get());
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete comment: {}", ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();

      throw new CommentDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public CommentDto storeComment(Comment comment) throws CommentDatabaseException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      CommentDto mergedComment = CommentDatabaseUtils.mergeComment(comment, em);
      tx.commit();
      return mergedComment;
    } catch (Exception e) {
      logger.error("Could not update or store comment: {}", ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();

      throw new CommentDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public CommentDto getComment(long commentId) throws NotFoundException, CommentDatabaseException {
    EntityManager em = emf.createEntityManager();
    try {
      Option<CommentDto> dto = CommentDatabaseUtils.find(Option.some(commentId), em, CommentDto.class);
      if (dto.isNone())
        throw new NotFoundException("No comment with id=" + commentId + " exists");

      return dto.get();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retreive comment: {}", ExceptionUtils.getStackTrace(e));
      throw new CommentDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

}
