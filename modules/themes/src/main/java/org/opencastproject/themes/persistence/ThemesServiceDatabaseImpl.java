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

package org.opencastproject.themes.persistence;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.AbstractSearchIndex;
import org.opencastproject.elasticsearch.index.theme.IndexTheme;
import org.opencastproject.index.rebuild.AbstractIndexProducer;
import org.opencastproject.index.rebuild.IndexRebuildService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.themes.Theme;
import org.opencastproject.themes.ThemesServiceDatabase;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 * Implements {@link ThemesServiceDatabase}. Defines permanent storage for themes.
 */
public class ThemesServiceDatabaseImpl extends AbstractIndexProducer implements ThemesServiceDatabase {

  public static final String PERSISTENCE_UNIT = "org.opencastproject.themes";

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(ThemesServiceDatabaseImpl.class);

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  /** The security service */
  protected SecurityService securityService;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService;

  /** The organization directory service to get organizations */
  protected OrganizationDirectoryService organizationDirectoryService;

  /** The elasticsearch indices */
  protected AbstractSearchIndex adminUiIndex;

  /** The component context for this themes service database */
  private ComponentContext cc;

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for themes");
    this.cc = cc;
  }

  /** OSGi DI */
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /**
   * OSGi callback to set the security service.
   *
   * @param securityService
   *          the security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi callback to set the user directory service
   *
   * @param userDirectoryService
   *          the user directory service
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /** OSGi DI */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /** OSGi DI */
  public void setAdminUiIndex(AbstractSearchIndex index) {
    this.adminUiIndex = index;
  }

  @Override
  public Theme getTheme(long id) throws ThemesServiceDatabaseException, NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      ThemeDto themeDto = getThemeDto(id, em);
      if (themeDto == null) {
        throw new NotFoundException("No theme with id=" + id + " exists");
      }

      return themeDto.toTheme(userDirectoryService);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get theme", e);
      throw new ThemesServiceDatabaseException(e);
    } finally {
      if (em != null) {
        em.close();
      }
    }
  }

  private List<Theme> getThemes() throws ThemesServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      TypedQuery<ThemeDto> q = em.createNamedQuery("Themes.findByOrg", ThemeDto.class).setParameter("org", orgId);
      List<ThemeDto> themeDtos = q.getResultList();

      List<Theme> themes = new ArrayList<>();
      for (ThemeDto themeDto : themeDtos) {
        themes.add(themeDto.toTheme(userDirectoryService));
      }
      return themes;
    } catch (Exception e) {
      logger.error("Could not get themes", e);
      throw new ThemesServiceDatabaseException(e);
    } finally {
      if (em != null) {
        em.close();
      }
    }
  }

  @Override
  public Theme updateTheme(Theme theme) throws ThemesServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();

      ThemeDto themeDto = null;
      if (theme.getId().isSome()) {
        themeDto = getThemeDto(theme.getId().get(), em);
      }

      if (themeDto == null) {
        // no theme stored, create new entity
        themeDto = new ThemeDto();
        themeDto.setOrganization(securityService.getOrganization().getId());
        updateTheme(theme, themeDto);
        em.persist(themeDto);
      } else {
        updateTheme(theme, themeDto);
        em.merge(themeDto);
      }
      tx.commit();
      theme = themeDto.toTheme(userDirectoryService);

      // update the elasticsearch indices
      String orgId = securityService.getOrganization().getId();
      User user = securityService.getUser();
      updateThemeInIndex(theme, adminUiIndex, orgId, user);

      return theme;
    } catch (Exception e) {
      logger.error("Could not update theme {}", theme, e);
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new ThemesServiceDatabaseException(e);
    } finally {
      if (em != null) {
        em.close();
      }
    }
  }

  private void updateTheme(Theme theme, ThemeDto themeDto) {
    themeDto.setUsername(theme.getCreator().getUsername());
    themeDto.setCreationDate(theme.getCreationDate());
    themeDto.setDefault(theme.isDefault());
    themeDto.setName(theme.getName());
    themeDto.setDescription(theme.getDescription());
    themeDto.setBumperActive(theme.isBumperActive());
    themeDto.setBumperFile(theme.getBumperFile());
    themeDto.setTrailerActive(theme.isTrailerActive());
    themeDto.setTrailerFile(theme.getTrailerFile());
    themeDto.setTitleSlideActive(theme.isTitleSlideActive());
    themeDto.setTitleSlideBackground(theme.getTitleSlideBackground());
    themeDto.setTitleSlideMetadata(theme.getTitleSlideMetadata());
    themeDto.setLicenseSlideActive(theme.isLicenseSlideActive());
    themeDto.setLicenseSlideBackground(theme.getLicenseSlideBackground());
    themeDto.setLicenseSlideDescription(theme.getLicenseSlideDescription());
    themeDto.setWatermarkActive(theme.isWatermarkActive());
    themeDto.setWatermarkFile(theme.getWatermarkFile());
    themeDto.setWatermarkPosition(theme.getWatermarkPosition());
  }

  @Override
  public void deleteTheme(long id) throws ThemesServiceDatabaseException, NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      ThemeDto themeDto = getThemeDto(id, em);
      if (themeDto == null) {
        throw new NotFoundException("No theme with id=" + id + " exists");
      }

      tx = em.getTransaction();
      tx.begin();
      em.remove(themeDto);
      tx.commit();

      // update the elasticsearch indices
      String organization = securityService.getOrganization().getId();
      removeThemeFromIndex(id, adminUiIndex, organization);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete theme '{}'", id, e);
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new ThemesServiceDatabaseException(e);
    } finally {
      if (em != null) {
        em.close();
      }
    }
  }

  @Override
  public int countThemes() throws ThemesServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      Query q = em.createNamedQuery("Themes.count").setParameter("org", orgId);
      Number countResult = (Number) q.getSingleResult();
      return countResult.intValue();
    } catch (Exception e) {
      logger.error("Could not count themes", e);
      throw new ThemesServiceDatabaseException(e);
    } finally {
      if (em != null) {
        em.close();
      }
    }
  }

  /**
   * Gets a theme by its ID, using the current organizational context.
   *
   * @param id
   *          the theme identifier
   * @param em
   *          an open entity manager
   * @return the theme entity, or null if not found
   */
  private ThemeDto getThemeDto(long id, EntityManager em) {
    String orgId = securityService.getOrganization().getId();
    Query q = em.createNamedQuery("Themes.findById").setParameter("id", id).setParameter("org", orgId);
    try {
      return (ThemeDto) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  @Override
  public void repopulate(final AbstractSearchIndex index) {
    if (index.getIndexName() != adminUiIndex.getIndexName()) {
      logger.info("Themes are currently not part of the {} index, no re-indexing necessary.");
      return;
    }

    for (final Organization organization : organizationDirectoryService.getOrganizations()) {
      User systemUser = SecurityUtil.createSystemUser(cc, organization);
      SecurityUtil.runAs(securityService, organization, systemUser, () -> {
        try {
          final List<Theme> themes = getThemes();
          int total = themes.size();
          int current = 1;
          logIndexRebuildBegin(logger, index.getIndexName(), total, "themes", organization);
          for (Theme theme : themes) {
            updateThemeInIndex(theme, index, organization.getId(), systemUser);
            logIndexRebuildProgress(logger, index.getIndexName(), total, current);
            current++;
          }
        } catch (ThemesServiceDatabaseException e) {
          logger.error("Unable to get themes from the database", e);
          throw new IllegalStateException(e);
        }
      });
    }
  }

  @Override
  public IndexRebuildService.Service getService() {
    return IndexRebuildService.Service.Themes;
  }

  /**
   * Remove the theme from the ElasticSearch index.
   *
   * @param themeId
   *           the id of the theme to remove
   * @param index
   *           the index to remove the theme from
   * @param orgId
   *           the organization the theme belongs to
   */
  private void removeThemeFromIndex(long themeId, AbstractSearchIndex index, String orgId) {
    logger.debug("Removing theme {} from the {} index.", themeId, index.getIndexName());

    try {
      index.delete(IndexTheme.DOCUMENT_TYPE, Long.toString(themeId), orgId);
      logger.debug("Theme {} removed from the {} index", themeId, index.getIndexName());
    } catch (SearchIndexException e) {
      logger.error("Error deleting the theme {} from the {} index", themeId, index.getIndexName(), e);
    }
  }

  /**
   * Update the theme in the ElasticSearch index.
   *  @param theme
   *           the theme to update
   * @param index
   *           the index to update
   * @param orgId
   *           the organization the theme belongs to
   * @param user
   */
  private void updateThemeInIndex(Theme theme, AbstractSearchIndex index, String orgId,
          User user) {
    logger.debug("Updating the theme with id '{}', name '{}', description '{}', organization '{}' in the {} index.",
            theme.getId(), theme.getName(), theme.getDescription(),
            orgId, index.getIndexName());
    try {
      if (theme.getId().isNone()) {
        throw new IllegalArgumentException("Can't put theme in index without valid id!");
      }
      Long id = theme.getId().get();

      // the function to do the actual updating
      Function<Optional<IndexTheme>, Optional<IndexTheme>> updateFunction = (Optional<IndexTheme> indexThemeOpt) -> {
        IndexTheme indexTheme;
        if (indexThemeOpt.isPresent()) {
          indexTheme = indexThemeOpt.get();
        } else {
          indexTheme = new IndexTheme(id, orgId);
        }
        String creator = StringUtils.isNotBlank(theme.getCreator().getName())
                ? theme.getCreator().getName() : theme.getCreator().getUsername();

        indexTheme.setCreationDate(theme.getCreationDate());
        indexTheme.setDefault(theme.isDefault());
        indexTheme.setName(theme.getName());
        indexTheme.setDescription(theme.getDescription());
        indexTheme.setCreator(creator);
        indexTheme.setBumperActive(theme.isBumperActive());
        indexTheme.setBumperFile(theme.getBumperFile());
        indexTheme.setTrailerActive(theme.isTrailerActive());
        indexTheme.setTrailerFile(theme.getTrailerFile());
        indexTheme.setTitleSlideActive(theme.isTitleSlideActive());
        indexTheme.setTitleSlideBackground(theme.getTitleSlideBackground());
        indexTheme.setTitleSlideMetadata(theme.getTitleSlideMetadata());
        indexTheme.setLicenseSlideActive(theme.isLicenseSlideActive());
        indexTheme.setLicenseSlideBackground(theme.getLicenseSlideBackground());
        indexTheme.setLicenseSlideDescription(theme.getLicenseSlideDescription());
        indexTheme.setWatermarkActive(theme.isWatermarkActive());
        indexTheme.setWatermarkFile(theme.getWatermarkFile());
        indexTheme.setWatermarkPosition(theme.getWatermarkPosition());
        return Optional.of(indexTheme);
      };

      index.addOrUpdateTheme(id, updateFunction, orgId, user);
      logger.debug("Updated the theme {} in the {} index", theme.getId(), index.getIndexName());
    } catch (SearchIndexException e) {
      logger.error("Error updating the theme {} in the {} index", theme.getId(), index.getIndexName(), e);
    }
  }
}
