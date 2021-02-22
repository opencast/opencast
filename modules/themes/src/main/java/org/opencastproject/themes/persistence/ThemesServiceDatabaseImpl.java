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
import org.opencastproject.elasticsearch.index.theme.ThemeIndexUtils;
import org.opencastproject.index.rebuild.AbstractIndexProducer;
import org.opencastproject.index.rebuild.IndexRebuildService;
import org.opencastproject.message.broker.api.theme.SerializableTheme;
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
  protected AbstractSearchIndex externalApiIndex;

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

  /** OSGi DI */
  public void setExternalApiIndex(AbstractSearchIndex index) {
    this.externalApiIndex = index;
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
      SerializableTheme serializableTheme = toSerializableTheme(theme);
      updateThemeInIndex(serializableTheme, adminUiIndex, orgId, user);
      updateThemeInIndex(serializableTheme, externalApiIndex, orgId, user);

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
      removeThemeFromIndex(id, externalApiIndex, organization);


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

  /**
   * Converts a theme to a {@link SerializableTheme} for using by the theme message queue
   *
   * @param theme
   *          the theme
   * @return the {@link SerializableTheme}
   */
  private SerializableTheme toSerializableTheme(Theme theme) {
    String creator = StringUtils.isNotBlank(theme.getCreator().getName()) ? theme.getCreator().getName()
            : theme.getCreator().getUsername();
    return new SerializableTheme(theme.getId().getOrElse(org.apache.commons.lang3.math.NumberUtils.LONG_MINUS_ONE),
            theme.getCreationDate(), theme.isDefault(), creator, theme.getName(), theme.getDescription(),
            theme.isBumperActive(), theme.getBumperFile(), theme.isTrailerActive(), theme.getTrailerFile(),
            theme.isTitleSlideActive(), theme.getTitleSlideMetadata(), theme.getTitleSlideBackground(),
            theme.isLicenseSlideActive(), theme.getLicenseSlideBackground(), theme.getLicenseSlideDescription(),
            theme.isWatermarkActive(), theme.getWatermarkFile(), theme.getWatermarkPosition());
  }

  @Override
  public void repopulate(final AbstractSearchIndex index) {
    for (final Organization organization : organizationDirectoryService.getOrganizations()) {
      User systemUser = SecurityUtil.createSystemUser(cc, organization);
      SecurityUtil.runAs(securityService, organization, systemUser, () -> {
        try {
          final List<Theme> themes = getThemes();
          int total = themes.size();
          int current = 1;
          logIndexRebuildBegin(logger, index.getIndexName(), total, "themes", organization);
          for (Theme theme : themes) {
            SerializableTheme serializableTheme = toSerializableTheme(theme);
            updateThemeInIndex(serializableTheme, adminUiIndex, organization.getId(), systemUser);
            updateThemeInIndex(serializableTheme, externalApiIndex, organization.getId(), systemUser);

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
      index.delete(org.opencastproject.elasticsearch.index.theme.Theme.DOCUMENT_TYPE, Long.toString(themeId)
              .concat(orgId));
      logger.debug("Theme {} removed from the {} index", themeId, index.getIndexName());
    } catch (SearchIndexException e) {
      logger.error("Error deleting the theme {} from the {} index", themeId, index.getIndexName(), e);
    }
  }

  /**
   * Update the theme in the ElasticSearch index.
   *
   * @param serializableTheme
   *           the theme to update
   * @param index
   *           the index to update
   * @param orgId
   *           the organization the theme belongs to
   * @param user
   *           the user used to query the index
   */
  private void updateThemeInIndex(SerializableTheme serializableTheme, AbstractSearchIndex index, String orgId,
          User user) {
    logger.debug("Updating the theme with id '{}', name '{}', description '{}', organization '{}' in the {} index.",
            serializableTheme.getId(), serializableTheme.getName(), serializableTheme.getDescription(),
            orgId, index.getIndexName());
    try {
      org.opencastproject.elasticsearch.index.theme.Theme theme = ThemeIndexUtils
              .getOrCreate(serializableTheme.getId(), orgId, user, index);
      theme.setCreationDate(serializableTheme.getCreationDate());
      theme.setDefault(serializableTheme.isDefault());
      theme.setName(serializableTheme.getName());
      theme.setDescription(serializableTheme.getDescription());
      theme.setCreator(serializableTheme.getCreator());
      theme.setBumperActive(serializableTheme.isBumperActive());
      theme.setBumperFile(serializableTheme.getBumperFile());
      theme.setTrailerActive(serializableTheme.isTrailerActive());
      theme.setTrailerFile(serializableTheme.getTrailerFile());
      theme.setTitleSlideActive(serializableTheme.isTitleSlideActive());
      theme.setTitleSlideBackground(serializableTheme.getTitleSlideBackground());
      theme.setTitleSlideMetadata(serializableTheme.getTitleSlideMetadata());
      theme.setLicenseSlideActive(serializableTheme.isLicenseSlideActive());
      theme.setLicenseSlideBackground(serializableTheme.getLicenseSlideBackground());
      theme.setLicenseSlideDescription(serializableTheme.getLicenseSlideDescription());
      theme.setWatermarkActive(serializableTheme.isWatermarkActive());
      theme.setWatermarkFile(serializableTheme.getWatermarkFile());
      theme.setWatermarkPosition(serializableTheme.getWatermarkPosition());
      index.addOrUpdate(theme);
      logger.debug("Updated the theme {} in the {} index", serializableTheme.getId(), index.getIndexName());
    } catch (SearchIndexException e) {
      logger.error("Error updating the theme {} in the {} index", serializableTheme.getId(), index.getIndexName(), e);
    }
  }
}
