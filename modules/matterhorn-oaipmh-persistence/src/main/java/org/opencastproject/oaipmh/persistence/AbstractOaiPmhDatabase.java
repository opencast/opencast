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
package org.opencastproject.oaipmh.persistence;

import static com.entwinemedia.fn.Stream.$;
import static java.lang.String.format;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.authorization.xacml.XACMLUtils;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Strings;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.xml.bind.JAXBException;

public abstract class AbstractOaiPmhDatabase implements OaiPmhDatabase {
  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(AbstractOaiPmhDatabase.class);

  public abstract EntityManagerFactory getEmf();

  public abstract SecurityService getSecurityService();

  public abstract SeriesService getSeriesService();

  public abstract Workspace getWorkspace();

  /** Return the current date. Used in implementation instead of new Date(); to facilitate unit testing. */
  public Date currentDate() {
    return new Date();
  }

  @Override
  public void store(MediaPackage mediaPackage, String repository) throws OaiPmhDatabaseException {
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
        final String message = ExceptionUtils.getMessage(e.getCause()).toLowerCase();
        if (message.contains("unique") || message.contains("duplicate")) {
          try {
            Thread.sleep(1100L);
          } catch (InterruptedException e1) {
            throw new OaiPmhDatabaseException(e1);
          }
          i++;
          logger.info("Storing OAI-PMH entry '{}' from  repository '{}' failed, retry {} times.", new String[] {
                  mediaPackage.getIdentifier().toString(), repository, Integer.toString(i) });
        } else {
          logger.error("Could not store mediapackage '{}' to OAI-PMH repository '{}': {}", new String[] {
                  mediaPackage.getIdentifier().toString(), repository, ExceptionUtils.getStackTrace(e) });
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

  private void updateEntity(OaiPmhEntity entity, MediaPackage mediaPackage,
                            String repository) throws OaiPmhDatabaseException {
    entity.setOrganization(getSecurityService().getOrganization().getId());
    entity.setDeleted(false);
    entity.setRepositoryId(repository);

    entity.setMediaPackageId(mediaPackage.getIdentifier().toString());
    entity.setMediaPackageXML(MediaPackageParser.getAsXml(mediaPackage));

    //
    // episode
    Catalog[] episodeCatalog = mediaPackage.getCatalogs(MediaPackageElements.EPISODE);
    if (episodeCatalog.length != 0) {
      entity.setEpisodeDublinCoreXML(toXml(DublinCoreUtil.loadDublinCore(getWorkspace(), episodeCatalog[0])));
    } else {
      entity.setEpisodeDublinCoreXML(null);
    }

    //
    // series DublinCore and ACL
    final Opt<String> seriesId = Opt.nul(mediaPackage.getSeries());
    final Opt<Catalog> seriesDcCatalog = $(mediaPackage.getCatalogs(MediaPackageElements.SERIES)).head();
    final Opt<Catalog> seriesAclCatalog = $(mediaPackage.getCatalogs(MediaPackageElements.XACML_POLICY_SERIES)).head();

    if (seriesId.isNone()) {
      entity.setSeries(null);
    }
    if (seriesDcCatalog.isNone()) {
      entity.setSeriesDublinCoreXML(null);
    }
    if (seriesAclCatalog.isNone()) {
      entity.setSeriesAclXML(null);
    }
    if (seriesId.isSome()) {
      // series ID is set
      final DublinCoreCatalog seriesDc = getSeriesDc(seriesId.get());
      entity.setSeries(seriesDc.getFirst(DublinCore.PROPERTY_IDENTIFIER));
      entity.setSeriesDublinCoreXML(toXml(seriesDc));
      for (final AccessControlList acl : getSeriesAcl(seriesId.get())) {
        entity.setSeriesAclXML(toXml(mediaPackage, acl));
      }
    } else {
      // no series ID, take everything from the media package
      if (seriesDcCatalog.isSome()) {
        final DublinCoreCatalog seriesDc = DublinCoreUtil.loadDublinCore(getWorkspace(), seriesDcCatalog.get());
        entity.setSeries(seriesDc.getFirst(DublinCore.PROPERTY_IDENTIFIER));
        entity.setSeriesDublinCoreXML(toXml(seriesDc));
        // only attach the series ACL if a series catalog is present
        // assume data inconsistency otherwise
        if (seriesAclCatalog.isSome()) {
          entity.setSeriesAclXML(loadAclXml(seriesAclCatalog.get()));
        }
      }
    }
  }

  @Nullable private String loadAclXml(MediaPackageElement element) {
    InputStream in = null;
    try {
      in = new FileInputStream(getWorkspace().get(element.getURI()));
      return IOUtils.toString(in, "UTF-8");
    } catch (Exception e) {
      logger.warn("Unable to load ACL from catalog '{}'", element);
      return null;
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @Override
  public void delete(String mediaPackageId, String repository) throws OaiPmhDatabaseException, NotFoundException {
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
          throw new NotFoundException("No media package with id=" + mediaPackageId + " exists");

        oaiPmhEntity.setDeleted(true);
        em.merge(oaiPmhEntity);
        tx.commit();
        success = true;
      } catch (NotFoundException e) {
        throw e;
      } catch (Exception e) {
        final String message = ExceptionUtils.getMessage(e.getCause()).toLowerCase();
        if (message.contains("unique") || message.contains("duplicate")) {
          try {
            Thread.sleep(1100L);
          } catch (InterruptedException e1) {
            throw new OaiPmhDatabaseException(e1);
          }
          i++;
          logger.info("Deleting OAI-PMH entry '{}' from  repository '{}' failed, retry {} times.",
                  new String[] { mediaPackageId, repository, Integer.toString(i) });
        } else {
          logger.error("Could not delete mediapackage '{}' from OAI-PMH repository '{}': {}",
                  new String[] { mediaPackageId, repository, ExceptionUtils.getStackTrace(e) });
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
    EntityManager em = null;
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
      if (!query.isSubsequentRequest()) {
        for (Date p : query.getModifiedAfter())
          predicates.add(cb.greaterThanOrEqualTo(c.get("modificationDate").as(Date.class), p));
      } else {
        for (Date p : query.getModifiedAfter())
          predicates.add(cb.greaterThan(c.get("modificationDate").as(Date.class), p));
      }
      for (Date p : query.getModifiedBefore())
        predicates.add(cb.lessThanOrEqualTo(c.get("modificationDate").as(Date.class), p));

      q.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
      q.orderBy(cb.asc(c.get("modificationDate")));

      TypedQuery<OaiPmhEntity> typedQuery = em.createQuery(q);
      for (int maxResult : query.getLimit())
        typedQuery.setMaxResults(maxResult);
      for (int startPosition : query.getOffset())
        typedQuery.setFirstResult(startPosition);
      return createSearchResult(typedQuery);
    } finally {
      if (em != null)
        em.close();
    }
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
    // Walk through response and create new items with title, creator, etc.
    final List<SearchResultItem> items = mlist(query.getResultList()).map(toResultItem).value();
    return new SearchResult() {
      @Override
      public List<SearchResultItem> getItems() {
        return items;
      }

      @Override
      public long size() {
        return items.size();
      }

      @Override
      public long getOffset() {
        return offset;
      }

      @Override
      public long getLimit() {
        return limit;
      }
    };
  }

  private final Function<OaiPmhEntity, SearchResultItem> toResultItem = new Function<OaiPmhEntity, SearchResultItem>() {
    @Override
    public SearchResultItem apply(final OaiPmhEntity entity) {
      final MediaPackage mp;
      try {
        mp = MediaPackageParser.getFromXml(entity.getMediaPackageXML());
      } catch (MediaPackageException e) {
        logger.error("Unable to read media package from OAI-PMH search result entity {}: {}", entity, e);
        return chuck(e);
      }
      final Opt<String> seriesDcXml = Opt.nul(entity.getSeriesDublinCoreXML()).bind(Strings.trimToNone);
      final Opt<DublinCoreCatalog> seriesDc = seriesDcXml.bind(DublinCoreXmlFormat.readOptFromString);
      final Opt<String> seriesXacml = Opt.nul(entity.getSeriesAclXML()).bind(Strings.trimToNone);
      final Opt<String> episodeDcXml = Opt.nul(entity.getEpisodeDublinCoreXML()).bind(Strings.trimToNone);
      final Opt<DublinCoreCatalog> episodeDc = episodeDcXml.bind(DublinCoreXmlFormat.readOptFromString);
      return new SearchResultItem() {
        @Override
        public String getId() {
          return entity.getMediaPackageId();
        }

        @Override
        public MediaPackage getMediaPackage() {
          return mp;
        }

        @Override
        public String getMediaPackageXml() {
          return entity.getMediaPackageXML();
        }

        @Override
        public String getOrganization() {
          return entity.getOrganization();
        }

        @Override
        public String getRepository() {
          return entity.getRepositoryId();
        }

        @Override
        public Date getModificationDate() {
          return entity.getModificationDate();
        }

        @Override
        public boolean isDeleted() {
          return entity.isDeleted();
        }

        @Override
        public Option<DublinCoreCatalog> getSeriesDublinCore() {
          return Option.fromOpt(seriesDc);
        }

        @Override
        public Option<String> getSeriesDublinCoreXml() {
          return Option.fromOpt(seriesDcXml);
        }

        @Override
        public Option<DublinCoreCatalog> getEpisodeDublinCore() {
          return Option.fromOpt(episodeDc);
        }

        @Override
        public Option<String> getEpisodeDublinCoreXml() {
          return Option.fromOpt(episodeDcXml);
        }

        @Override public Option<String> getSeriesAclXml() {
          return Option.fromOpt(seriesXacml);
        }
      };
    }
  };

  public static String forSwitch(Opt<?>... opts) {
    return $(opts).map(new Fn<Opt<?>, String>() {
      @Override public String apply(Opt<?> o) {
        return o.isSome() ? "some" : "none";
      }
    }).mkString(":");
  }

  public static String toXml(DublinCoreCatalog dc) {
    try {
      return dc.toXmlString();
    } catch (IOException e) {
      logger.error("Cannot serialize DublinCoreCatalog to XML", e);
      return chuck(e);
    }
  }

  public static String toXml(MediaPackage mp, AccessControlList acl) {
    try {
      return XACMLUtils.getXacml(mp, acl);
    } catch (JAXBException e) {
      logger.error(format("Cannot serialize access control list of media package %s to XML", mp.getIdentifier().toString()), e);
      return chuck(e);
    }
  }

  public DublinCoreCatalog getSeriesDc(String seriesId) {
    try {
      return getSeriesService().getSeries(seriesId);
    } catch (SeriesException e) {
      logger.error("An error occurred while talking to the SeriesService", e);
      return chuck(e);
    } catch (NotFoundException e) {
      logger.error(format("The requested series %s does not exist", seriesId), e);
      return chuck(e);
    } catch (UnauthorizedException e) {
      logger.error(format("You are not allowed to request series %s", seriesId), e);
      return chuck(e);
    }
  }

  public Opt<AccessControlList> getSeriesAcl(String seriesId) {
    try {
      return Opt.some(getSeriesService().getSeriesAccessControl(seriesId));
    } catch (NotFoundException e) {
      logger.info(format("Series %s does not have an ACL", seriesId), e);
      return Opt.none();
    } catch (SeriesException e) {
      logger.error("An error occurred while talking to the SeriesService", e);
      return chuck(e);
    }
  }
}
