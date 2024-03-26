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

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.theme.IndexTheme;
import org.opencastproject.elasticsearch.index.rebuild.AbstractIndexProducer;
import org.opencastproject.elasticsearch.index.rebuild.IndexProducer;
import org.opencastproject.elasticsearch.index.rebuild.IndexRebuildException;
import org.opencastproject.elasticsearch.index.rebuild.IndexRebuildService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.themes.Theme;
import org.opencastproject.themes.ThemesServiceDatabase;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Implements {@link ThemesServiceDatabase}. Defines permanent storage for themes.
 */
@Component(
    immediate = true,
    service = { ThemesServiceDatabase.class, IndexProducer.class },
    property = {
        "service.description=Themes Database Service"
    }
)
public class ThemesServiceDatabaseImpl extends AbstractIndexProducer implements ThemesServiceDatabase {

  public static final String PERSISTENCE_UNIT = "org.opencastproject.themes";

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(ThemesServiceDatabaseImpl.class);

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  /** The security service */
  protected SecurityService securityService;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService;

  /** The organization directory service to get organizations */
  protected OrganizationDirectoryService organizationDirectoryService;

  /** The elasticsearch indices */
  protected ElasticsearchIndex index;

  /** The component context for this themes service database */
  private ComponentContext cc;

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for themes");
    this.cc = cc;
    db = dbSessionFactory.createSession(emf);
  }

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.themes)")
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
   *          the security service
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi callback to set the user directory service
   *
   * @param userDirectoryService
   *          the user directory service
   */
  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /** OSGi DI */
  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /** OSGi DI */
  @Reference
  public void setIndex(ElasticsearchIndex index) {
    this.index = index;
  }

  @Override
  public Theme getTheme(long id) throws ThemesServiceDatabaseException, NotFoundException {
    try {
      return db.exec(getThemeDtoQuery(id))
          .map(t -> t.toTheme(userDirectoryService))
          .orElseThrow(() -> new NotFoundException("No theme with id=" + id + " exists"));
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get theme", e);
      throw new ThemesServiceDatabaseException(e);
    }
  }

  private List<Theme> getThemes() throws ThemesServiceDatabaseException {
    try {
      String orgId = securityService.getOrganization().getId();
      return db.exec(namedQuery.findAll(
          "Themes.findByOrg",
              ThemeDto.class,
              Pair.of("org", orgId)
          )).stream()
          .map(t -> t.toTheme(userDirectoryService))
          .collect(Collectors.toList());
    } catch (Exception e) {
      logger.error("Could not get themes", e);
      throw new ThemesServiceDatabaseException(e);
    }
  }

  @Override
  public Theme updateTheme(final Theme theme) throws ThemesServiceDatabaseException {
    try {
      Theme newTheme = db.execTxChecked(em -> {
        ThemeDto themeDto = null;
        if (theme.getId().isSome()) {
          themeDto = getThemeDtoQuery(theme.getId().get()).apply(em).orElse(null);
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

        return themeDto.toTheme(userDirectoryService);
      });

      // update the elasticsearch indices
      String orgId = securityService.getOrganization().getId();
      User user = securityService.getUser();
      updateThemeInIndex(newTheme, orgId, user);

      return newTheme;
    } catch (Exception e) {
      logger.error("Could not update theme {}", theme, e);
      throw new ThemesServiceDatabaseException(e);
    }
  }

  private void updateTheme(Theme theme, ThemeDto themeDto) {
    if (theme.getId().isSome()) {
      themeDto.setId(theme.getId().get());
    }
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
    try {
      db.execTxChecked(em -> {
        ThemeDto themeDto = getThemeDtoQuery(id).apply(em)
            .orElseThrow(() -> new NotFoundException("No theme with id=" + id + " exists"));
        namedQuery.remove(themeDto).accept(em);
      });

      // update the elasticsearch indices
      String organization = securityService.getOrganization().getId();
      removeThemeFromIndex(id, organization);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete theme '{}'", id, e);
      throw new ThemesServiceDatabaseException(e);
    }
  }

  @Override
  public int countThemes() throws ThemesServiceDatabaseException {
    try {
      String orgId = securityService.getOrganization().getId();
      return db.exec(namedQuery.find(
          "Themes.count",
          Number.class,
          Pair.of("org", orgId)
      )).intValue();
    } catch (Exception e) {
      logger.error("Could not count themes", e);
      throw new ThemesServiceDatabaseException(e);
    }
  }

  /**
   * Gets a theme by its ID, using the current organizational context.
   *
   * @param id
   *          the theme identifier
   * @return a query function returning an optional theme entity
   */
  private Function<EntityManager, Optional<ThemeDto>> getThemeDtoQuery(long id) {
    String orgId = securityService.getOrganization().getId();
    return namedQuery.findOpt(
        "Themes.findById",
        ThemeDto.class,
        Pair.of("id", id),
        Pair.of("org", orgId)
    );
  }

  @Override
  public void repopulate(IndexRebuildService.ServicePart type) throws IndexRebuildException {
    try {
      for (final Organization organization : organizationDirectoryService.getOrganizations()) {
        try {
          final List<Theme> themes = getThemes();
          int total = themes.size();
          logIndexRebuildBegin(logger, index.getIndexName(), total, "themes", organization);
          int current = 0;
          int n = 20;
          List<IndexTheme> updatedThemeRange = new ArrayList<>();

          for (Theme theme : themes) {
            current++;

            var updatedThemeData = index.getTheme(theme.getId().get(), organization.toString(),
                        securityService.getUser());
            updatedThemeData = getThemeUpdateFunction(theme, organization.toString()).apply(updatedThemeData);
            updatedThemeRange.add(updatedThemeData.get());

            if (updatedThemeRange.size() >= n || current >= themes.size()) {
              index.bulkThemeUpdate(updatedThemeRange);
              logIndexRebuildProgress(logger, index.getIndexName(), total, current, n);
              updatedThemeRange.clear();
            }
          }
        } catch (ThemesServiceDatabaseException e) {
          logger.error("Unable to get themes from the database", e);
          throw new IllegalStateException(e);
        }
      }
    } catch (Exception e) {
      logIndexRebuildError(logger, index.getIndexName(), e);
      throw new IndexRebuildException(index.getIndexName(), getService(), e);
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
  private void removeThemeFromIndex(long themeId, String orgId) {
    logger.debug("Removing theme {} from the {} index.", themeId, index.getIndexName());

    try {
      index.deleteTheme(Long.toString(themeId), orgId);
      logger.debug("Theme {} removed from the {} index", themeId, index.getIndexName());
    } catch (SearchIndexException e) {
      logger.error("Error deleting the theme {} from the {} index", themeId, index.getIndexName(), e);
    }
  }

  /**
   * Update the theme in the ElasticSearch index.
   *  @param theme
   *           the theme to update
   * @param orgId
   *           the organization the theme belongs to
   * @param user
   */
  private void updateThemeInIndex(Theme theme, String orgId,
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
        indexTheme = indexThemeOpt.orElseGet(() -> new IndexTheme(id, orgId));
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
  /**
   * Get the function to update a theme in the Elasticsearch index.
   *
   * @param theme
   *          The theme to update
   * @param orgId
   *          The id of the current organization
   * @return the function to do the update
   */
  private Function<Optional<IndexTheme>, Optional<IndexTheme>> getThemeUpdateFunction(Theme theme, String orgId) {
    return (Optional<IndexTheme> indexThemeOpt) -> {
      IndexTheme indexTheme;
      indexTheme = indexThemeOpt.orElseGet(() -> new IndexTheme(theme.getId().get(), orgId));
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
  }
}
