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
package org.opencastproject.oaipmh.persistence.impl;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabaseException;
import org.opencastproject.oaipmh.persistence.OaiPmhElementEntity;
import org.opencastproject.oaipmh.persistence.OaiPmhEntity;
import org.opencastproject.oaipmh.persistence.OaiPmhSetDefinition;
import org.opencastproject.oaipmh.persistence.OaiPmhSetDefinitionFilter;
import org.opencastproject.oaipmh.persistence.Query;
import org.opencastproject.oaipmh.persistence.QueryBuilder;
import org.opencastproject.oaipmh.persistence.SearchResult;
import org.opencastproject.oaipmh.persistence.SearchResultElementItem;
import org.opencastproject.oaipmh.persistence.SearchResultItem;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.XmlUtil;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public abstract class AbstractOaiPmhDatabase implements OaiPmhDatabase {
  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(AbstractOaiPmhDatabase.class);

  private ReadWriteLock dbAccessLock = new ReentrantReadWriteLock();

  public abstract EntityManagerFactory getEmf();

  public abstract SecurityService getSecurityService();

  public abstract Workspace getWorkspace();

  @Override
  public void store(MediaPackage mediaPackage, String repository) throws OaiPmhDatabaseException {
    try {
      dbAccessLock.writeLock().lock();
      storeInternal(mediaPackage, repository);
    } finally {
      dbAccessLock.writeLock().unlock();
    }
  }

  private void storeInternal(MediaPackage mediaPackage, String repository) throws OaiPmhDatabaseException {
    int i = 0;
    boolean success = false;
    while (!success && i < 5) {
      EntityManager em = null;
      EntityTransaction tx = null;
      try {
        em = getEmf().createEntityManager();
        tx = em.getTransaction();
        tx.begin();

        OaiPmhEntity entity = getOaiPmhEntity(mediaPackage.getIdentifier().toString(), repository, em);
        if (entity == null) {
          // no entry found, create new entity
          entity = new OaiPmhEntity();
          updateEntity(entity, mediaPackage, repository);
          em.persist(entity);
        } else {
          // entry found, update existing
          updateEntity(entity, mediaPackage, repository);
          em.merge(entity);
        }
        tx.commit();
        success = true;
      } catch (Exception e) {
        final String message = e.getCause().getMessage().toLowerCase();
        if (message.contains("unique") || message.contains("duplicate")) {
          try {
            Thread.sleep(1100L);
          } catch (InterruptedException e1) {
            throw new OaiPmhDatabaseException(e1);
          }
          i++;
          logger.info("Storing OAI-PMH entry '{}' from  repository '{}' failed, retry {} times.",
                  mediaPackage.getIdentifier(), repository, i);
        } else {
          logger.error("Could not store mediapackage '{}' to OAI-PMH repository '{}'", mediaPackage.getIdentifier(),
                  repository, e);
          if (tx != null && tx.isActive())
            tx.rollback();

          throw new OaiPmhDatabaseException(e);
        }
      } finally {
        if (em != null)
          em.close();
      }
    }
  }

  public void updateEntity(OaiPmhEntity entity, MediaPackage mediaPackage,
                            String repository) throws OaiPmhDatabaseException {
    entity.setOrganization(getSecurityService().getOrganization().getId());
    entity.setDeleted(false);
    entity.setRepositoryId(repository);

    entity.setMediaPackageId(mediaPackage.getIdentifier().toString());
    entity.setMediaPackageXML(MediaPackageParser.getAsXml(mediaPackage));
    entity.setSeries(mediaPackage.getSeries());
    entity.removeAllMediaPackageElements();


    for (MediaPackageElement mpe : mediaPackage.getElements()) {
      if (mpe.getFlavor() == null) {
        logger.debug("A flavor must be set on media package elements for publishing");
        continue;
      }

      if (mpe.getElementType() != MediaPackageElement.Type.Catalog
              && mpe.getElementType() != MediaPackageElement.Type.Attachment) {
        logger.debug("Only catalog and attachment types are currently supported");
        continue;
      }

      if (mpe.getMimeType() == null || !mpe.getMimeType().eq(MimeTypes.XML)) {
        logger.debug("Only media package elements with mime type XML are supported");
        continue;
      }
      String catalogXml = null;
      try (InputStream in = getWorkspace().read(mpe.getURI())) {
        catalogXml = IOUtils.toString(in, "UTF-8");
      } catch (Throwable e) {
        logger.warn("Unable to load catalog {} from media package {}",
                mpe.getIdentifier(), mediaPackage.getIdentifier().toString(), e);
        continue;
      }
      if (catalogXml == null || StringUtils.isBlank(catalogXml) || !XmlUtil.parseNs(catalogXml).isRight()) {
        logger.warn("The catalog {} from media package {} isn't a well formatted XML document",
                mpe.getIdentifier(), mediaPackage.getIdentifier().toString());
        continue;
      }

      entity.addMediaPackageElement(new OaiPmhElementEntity(
              mpe.getElementType().name(), mpe.getFlavor().toString(), catalogXml));
    }
  }

  @Override
  public void delete(String mediaPackageId, String repository) throws OaiPmhDatabaseException, NotFoundException {
    try {
      dbAccessLock.writeLock().lock();
      deleteInternal(mediaPackageId, repository);
    } finally {
      dbAccessLock.writeLock().unlock();
    }
  }

  private void deleteInternal(String mediaPackageId, String repository) throws OaiPmhDatabaseException, NotFoundException {
    int i = 0;
    boolean success = false;
    while (!success && i < 5) {
      EntityManager em = null;
      EntityTransaction tx = null;
      try {
        em = getEmf().createEntityManager();
        tx = em.getTransaction();
        tx.begin();

        OaiPmhEntity oaiPmhEntity = getOaiPmhEntity(mediaPackageId, repository, em);
        if (oaiPmhEntity == null)
          throw new NotFoundException("No media package with id " + mediaPackageId + " exists");

        oaiPmhEntity.setDeleted(true);
        em.merge(oaiPmhEntity);
        tx.commit();
        success = true;
      } catch (NotFoundException e) {
        throw e;
      } catch (Exception e) {
        final String message = e.getCause().getMessage().toLowerCase();
        if (message.contains("unique") || message.contains("duplicate")) {
          try {
            Thread.sleep(1100L);
          } catch (InterruptedException e1) {
            throw new OaiPmhDatabaseException(e1);
          }
          i++;
          logger.info("Deleting OAI-PMH entry '{}' from  repository '{}' failed, retry {} times.",
                  mediaPackageId, repository, i);
        } else {
          logger.error("Could not delete mediapackage '{}' from OAI-PMH repository '{}'",
                  mediaPackageId, repository, e);
          if (tx != null && tx.isActive())
            tx.rollback();

          throw new OaiPmhDatabaseException(e);
        }
      } finally {
        if (em != null)
          em.close();
      }
    }
  }

  @Override
  public SearchResult search(Query query) {
    try {
      final int chunkSize = query.getLimit().getOrElse(-1);
      dbAccessLock.readLock().lock();
      return searchInternal(query, chunkSize);
    } finally {
      dbAccessLock.readLock().unlock();
    }
  }

  private SearchResult searchInternal(Query query, int chunkSize) {
    EntityManager em = null;
    final String requestSetSpec = query.getSetSpec().getOrElseNull();
    final List<SearchResultItem> filteredItems = new ArrayList<>();
    Date lastDate = new Date();
    long resultSize;
    long resultOffset;
    long resultLimit;
    try {
      em = getEmf().createEntityManager();
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<OaiPmhEntity> q = cb.createQuery(OaiPmhEntity.class);
      Root<OaiPmhEntity> c = q.from(OaiPmhEntity.class);
      q.select(c);

      // create predicates joined in an "and" expression
      final List<Predicate> predicates = new ArrayList<Predicate>();
      predicates.add(cb.equal(c.get("organization"), getSecurityService().getOrganization().getId()));

      for (String p : query.getMediaPackageId())
        predicates.add(cb.equal(c.get("mediaPackageId"), p));
      for (String p : query.getRepositoryId())
        predicates.add(cb.equal(c.get("repositoryId"), p));
      for (String p : query.getSeriesId())
        predicates.add(cb.equal(c.get("series"), p));
      for (Boolean p : query.isDeleted())
        predicates.add(cb.equal(c.get("deleted"), p));
      if (query.isSubsequentRequest()) {
        for (Date p : query.getModifiedAfter())
          predicates.add(cb.greaterThan(c.get("modificationDate").as(Date.class), p));
      } else {
        for (Date p : query.getModifiedAfter())
          predicates.add(cb.greaterThanOrEqualTo(c.get("modificationDate").as(Date.class), p));
      }
      for (Date p : query.getModifiedBefore())
        predicates.add(cb.lessThanOrEqualTo(c.get("modificationDate").as(Date.class), p));

      q.where(cb.and(predicates.toArray(new Predicate[0])));
      q.orderBy(cb.asc(c.get("modificationDate")));

      TypedQuery<OaiPmhEntity> typedQuery = em.createQuery(q);
      if (chunkSize > 0) {
        typedQuery.setMaxResults(chunkSize);
      }
      for (int startPosition : query.getOffset()) {
        logger.warn("I'm pretty sure things break if this is used");
        typedQuery.setFirstResult(startPosition);
      }

      SearchResult result = createSearchResult(typedQuery);

      if (requestSetSpec != null) {
        Optional<OaiPmhSetDefinition> requestedSetDef = query.getSetDefinitions().stream().filter(def -> StringUtils.equals(def.getSetSpec(), requestSetSpec)).findFirst();
        // return empty result if there is no definition for a requested setSpec
        if (!requestedSetDef.isPresent()) {
          return new SearchResultImpl(result.getOffset(), result.getLimit(), new ArrayList<>());
        }
      }

      for (SearchResultItem item : result.getItems()) {
        for (OaiPmhSetDefinition setDef : query.getSetDefinitions()) {
          if (matchSetDef(setDef, item.getElements())) {
            item.addSetSpec(setDef.getSetSpec());
          }
        }
        if (requestSetSpec == null || item.getSetSpecs().contains(requestSetSpec)) {
          filteredItems.add(item);
        }
      }
      resultSize = result.size();
      resultOffset = result.getOffset();
      resultLimit = result.getLimit();
      if (requestSetSpec != null && resultSize == chunkSize) {
        lastDate = result.getItems().get(result.getItems().size() - 1).getModificationDate();
      }
    } finally {
      if (em != null)
        em.close();
    }

    if (requestSetSpec != null) {
      // only continue if we got the amount of results we requested in the first place
      // otherwise, we have no more results and it does not make any sense to continue
      logger.debug("result.size={}, chunk.size={}", resultSize, chunkSize);
      if (resultSize == chunkSize) {
        final int limit = query.getLimit().getOrElse(-1);
        logger.debug("filteredItems.size={}, query.limit={}", filteredItems.size(), limit);
        if (filteredItems.size() == 0 || filteredItems.size() < limit) {
          // No results left after filtering. Automatically request the next range to avoid returning empty results.
          QueryBuilder subQuery = QueryBuilder.query(query).modifiedAfter(lastDate)
                  .limit(limit - filteredItems.size())
                  .subsequentRequest(true);
          filteredItems.addAll(searchInternal(subQuery.build(), chunkSize).getItems());
        }
      }
    }

    if (query.getLimit().isSome() && filteredItems.size() > query.getLimit().get()) {
      logger.debug("limit items");
      return new SearchResultImpl(resultOffset, query.getLimit().get(),
              filteredItems.subList(0, query.getLimit().get()));
    }
    return new SearchResultImpl(resultOffset, resultLimit, filteredItems);
  }

  /**
   * Returns true if all set definition filters matches.
   *
   * @param setDef set definition to test
   * @param elements media package elements to test
   * @return returns true if all set definition filters matches, otherwise false
   */
  protected boolean matchSetDef(OaiPmhSetDefinition setDef, List<SearchResultElementItem> elements) {
    // all filters should match
    for (OaiPmhSetDefinitionFilter filter : setDef.getFilters()) {
      if (!matchSetDefFilter(filter, elements)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if any filter criterion matches
   *
   * @param filter filter to test
   * @param elements media package elements to test filter criteria on
   * @return true if any filter criteria matches, otherwise false
   */
  private boolean matchSetDefFilter(OaiPmhSetDefinitionFilter filter, List<SearchResultElementItem> elements) {
    // At least one filter criterion should match
    for (String criterion : filter.getCriteria().keySet()) {
      if (StringUtils.equals(OaiPmhSetDefinitionFilter.CRITERION_CONTAINS, criterion)) {
        for (SearchResultElementItem element : elements) {
          if (!StringUtils.equals(filter.getFlavor(), element.getFlavor())) {
            continue;
          }
          for (String criterionValue : filter.getCriteria().get(criterion)) {
            if (StringUtils.contains(element.getXml(), criterionValue)) {
              return true;
            }
          }
        }
      } else if (StringUtils.equals(OaiPmhSetDefinitionFilter.CRITERION_CONTAINSNOT, criterion)) {
        for (SearchResultElementItem element : elements) {
          if (!StringUtils.equals(filter.getFlavor(), element.getFlavor())) {
            continue;
          }
          for (String criterionValue : filter.getCriteria().get(criterion)) {
            if (!StringUtils.contains(element.getXml(), criterionValue)) {
              return true;
            }
          }
        }
      } else if (StringUtils.equals(OaiPmhSetDefinitionFilter.CRITERION_MATCH, criterion)) {
        for (String criterionValue : filter.getCriteria().get(criterion)) {
          Pattern matchPattern = null; // wait with initialization until we found an element to test
          for (SearchResultElementItem element : elements) {
            if (!StringUtils.equals(filter.getFlavor(), element.getFlavor())) {
              continue;
            }
            // initialize regex pattern once and only if we need it (for performance reasons)
            if (matchPattern == null) {
              matchPattern = Pattern.compile(criterionValue);
            }
            if (matchPattern.matcher(element.getXml()).find()) {
              return true;
            }
          }
        }
      } else {
        logger.warn("Unknown OAI-PMH set filter criterion '{}'. Ignore it.", criterion);
      }
    }
    return false;
  }

  /**
   * Gets a OAI-PMH entity by it's id, using the current organizational context.
   *
   * @param id
   *          the media package identifier
   * @param repository
   *          the OAI-PMH repository
   * @param em
   *          an open entity manager
   * @return the OAI-PMH entity, or null if not found
   */
  private OaiPmhEntity getOaiPmhEntity(String id, String repository, EntityManager em) {
    final String orgId = getSecurityService().getOrganization().getId();
    javax.persistence.Query q = em.createNamedQuery("OaiPmh.findById").setParameter("mediaPackageId", id)
            .setParameter("repository", repository).setParameter("organization", orgId);
    try {
      return (OaiPmhEntity) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Creates a search result from a given JPA query
   *
   * @param query
   *          the query
   * @return The search result.
   */
  private SearchResult createSearchResult(TypedQuery<OaiPmhEntity> query) {
    // Create and configure the query result
    final long offset = query.getFirstResult();
    final long limit = query.getMaxResults() != Integer.MAX_VALUE ? query.getMaxResults() : 0;
    final List<SearchResultItem> items = new ArrayList<>();
    for (OaiPmhEntity oaipmhEntity : query.getResultList()) {
      try {
        items.add(new SearchResultItemImpl(oaipmhEntity));
      } catch (Exception ex) {
        logger.warn("Unable to parse an OAI-PMH database entry", ex);
      }
    }
    return new SearchResultImpl(offset, limit, items);
  }
}
