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

package org.opencastproject.themes.persistence;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.themes.Theme;
import org.opencastproject.themes.ThemesServiceDatabase;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.requests.SortCriterion;

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
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Implements {@link ThemesServiceDatabase}. Defines permanent storage for themes.
 */
@Component(
    immediate = true,
    service = { ThemesServiceDatabase.class },
    property = {
        "service.description=Themes Database Service"
    }
)
public class ThemesServiceDatabaseImpl implements ThemesServiceDatabase {

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

  public List<Theme> findThemes(
      Optional<Integer> limit,
      Optional<Integer> offset,
      ArrayList<SortCriterion> sortCriteria,
      Optional<String> creatorFilter,
      Optional<String> textFilter
  ) {
    String orgId = securityService.getOrganization().getId();

    return db.execTxChecked(em -> {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      final CriteriaQuery<ThemeDto> query = cb.createQuery(ThemeDto.class);
      Root<ThemeDto> theme = query.from(ThemeDto.class);
      query.select(theme);
      query.distinct(true);

      // filter
      List<Predicate> conditions = new ArrayList<>();
      conditions.add(cb.equal(theme.get("organization"), orgId));

      // exact match, case sensitive
      if (creatorFilter.isPresent()) {
        conditions.add(cb.equal(theme.get("username"), creatorFilter.get()));
      }
      // not exact match, case-insensitive, each token needs to match at least one field
      if (textFilter.isPresent()) {
        List<Predicate> fulltextConditions = new ArrayList<>();
        String[] tokens = textFilter.get().split("\\s+");
        for (String token: tokens) {
          List<Predicate> fieldConditions = new ArrayList<>();
          Expression<String> literal = cb.literal("%" + token + "%");

          fieldConditions.add(cb.like(cb.lower(theme.get("username")), cb.lower(literal)));
          fieldConditions.add(cb.like(cb.lower(theme.get("name")), cb.lower(literal)));
          fieldConditions.add(cb.like(cb.lower(theme.get("description")), cb.lower(literal)));

          // token needs to match at least one field
          fulltextConditions.add(cb.or(fieldConditions.toArray(new Predicate[0])));
        }
        // all token have to match something
        // (different to fulltext search for Elasticsearch, where only one token has to match!)
        conditions.add(cb.and(fulltextConditions.toArray(new Predicate[0])));
      }
      query.where(cb.and(conditions.toArray(new Predicate[0])));

      // sort
      List<Order> orders = new ArrayList<>();
      for (SortCriterion criterion : sortCriteria) {
        String fieldName = criterion.getFieldName();
        switch(fieldName) {
          case "name":
            break;
          case "description":
            break;
          case "creator":
            fieldName = "username";
            break;
          case "default":
            fieldName = "isDefault";
            break;
          case "creation_date":
            fieldName = "creationDate";
            break;
          default:
            throw new IllegalArgumentException("Sorting criterion " + criterion.getFieldName() + " is not supported "
                + "for themes.");
        }

        Expression expression = theme.get(fieldName);
        if (criterion.getOrder() == SortCriterion.Order.Ascending) {
          orders.add(cb.asc(expression));
        } else if (criterion.getOrder() == SortCriterion.Order.Descending) {
          orders.add(cb.desc(expression));
        }

      }
      query.orderBy(orders);

      // other
      TypedQuery<ThemeDto> typedQuery = em.createQuery(query);
      if (limit.isPresent()) {
        typedQuery.setMaxResults(limit.get());
      }
      if (offset.isPresent()) {
        typedQuery.setFirstResult(offset.get());
      }

      return typedQuery.getResultList().stream()
          .map(t -> t.toTheme(userDirectoryService))
          .collect(Collectors.toList());
    });
  }

  @Override
  public Theme updateTheme(final Theme theme) throws ThemesServiceDatabaseException {
    try {
      Theme newTheme = db.execTxChecked(em -> {
        ThemeDto themeDto = null;
        if (theme.getId().isPresent()) {
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

      return newTheme;
    } catch (Exception e) {
      logger.error("Could not update theme {}", theme, e);
      throw new ThemesServiceDatabaseException(e);
    }
  }

  private void updateTheme(Theme theme, ThemeDto themeDto) {
    if (theme.getId().isPresent()) {
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
}
