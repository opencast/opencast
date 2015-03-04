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
package org.opencastproject.themes.persistence;

import org.opencastproject.index.IndexProducer;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.index.AbstractIndexProducer;
import org.opencastproject.message.broker.api.index.IndexRecreateObject;
import org.opencastproject.message.broker.api.index.IndexRecreateObject.Service;
import org.opencastproject.message.broker.api.theme.SerializableTheme;
import org.opencastproject.message.broker.api.theme.ThemeItem;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.themes.Theme;
import org.opencastproject.themes.ThemesServiceDatabase;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Effect0;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.spi.PersistenceProvider;

/**
 * Implements {@link ThemesServiceDatabase}. Defines permanent storage for themes.
 */
public class ThemesServiceDatabaseImpl extends AbstractIndexProducer implements ThemesServiceDatabase {

  public static final String PERSISTENCE_UNIT = "org.opencastproject.themes";

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(ThemesServiceDatabaseImpl.class);

  /** Persistence provider set by OSGi */
  protected PersistenceProvider persistenceProvider;

  /** Persistence properties used to create {@link EntityManagerFactory} */
  protected Map<String, Object> persistenceProperties;

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  /** The security service */
  protected SecurityService securityService;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService;

  /** The message broker sender service */
  protected MessageSender messageSender;

  /** The message broker receiver service */
  protected MessageReceiver messageReceiver;

  /** The organization directory service to get organizations */
  protected OrganizationDirectoryService organizationDirectoryService;

  /** The component context for this themes service database */
  private ComponentContext cc;

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for themes");
    emf = persistenceProvider.createEntityManagerFactory(PERSISTENCE_UNIT, persistenceProperties);
    this.cc = cc;
    super.activate();
  }

  /**
   * Closes entity manager factory.
   *
   * @param cc
   */
  public void deactivate(ComponentContext cc) {
    super.deactivate();
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
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  /** OSGi DI */
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  /** OSGi DI */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  @Override
  public Theme getTheme(long id) throws ThemesServiceDatabaseException, NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      ThemeDto themeDto = getThemeDto(id, em);
      if (themeDto == null)
        throw new NotFoundException("No theme with id=" + id + " exists");

      return themeDto.toTheme(userDirectoryService);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get theme: {}", ExceptionUtils.getStackTrace(e));
      throw new ThemesServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  private List<Theme> getThemes() throws ThemesServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      TypedQuery<ThemeDto> q = em.createNamedQuery("Themes.findByOrg", ThemeDto.class).setParameter("org", orgId);
      List<ThemeDto> themeDtos = q.getResultList();

      List<Theme> themes = new ArrayList<Theme>();
      for (ThemeDto themeDto : themeDtos) {
        themes.add(themeDto.toTheme(userDirectoryService));
      }
      return themes;
    } catch (Exception e) {
      logger.error("Could not get themes: {}", ExceptionUtils.getStackTrace(e));
      throw new ThemesServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
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
      if (theme.getId().isSome())
        themeDto = getThemeDto(theme.getId().get(), em);

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
      messageSender.sendObjectMessage(ThemeItem.THEME_QUEUE, MessageSender.DestinationType.Queue,
              ThemeItem.update(toSerializableTheme(theme)));
      return theme;
    } catch (Exception e) {
      logger.error("Could not update theme {}: {}", theme, ExceptionUtils.getStackTrace(e));
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
      if (themeDto == null)
        throw new NotFoundException("No theme with id=" + id + " exists");

      tx = em.getTransaction();
      tx.begin();
      em.remove(themeDto);
      tx.commit();
      messageSender.sendObjectMessage(ThemeItem.THEME_QUEUE, MessageSender.DestinationType.Queue, ThemeItem.delete(id));
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete theme '{}': {}", id, ExceptionUtils.getStackTrace(e));
      if (tx.isActive())
        tx.rollback();
      throw new ThemesServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
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
      logger.error("Could not count themes: {}", ExceptionUtils.getStackTrace(e));
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
    String creator = StringUtils.isNotBlank(theme.getCreator().getName()) ? theme.getCreator().getName() : theme
            .getCreator().getUsername();
    return new SerializableTheme(theme.getId().getOrElse(org.apache.commons.lang.math.NumberUtils.LONG_MINUS_ONE),
            theme.getCreationDate(), theme.isDefault(), creator, theme.getName(), theme.getDescription(),
            theme.isBumperActive(), theme.getBumperFile(), theme.isTrailerActive(), theme.getTrailerFile(),
            theme.isTitleSlideActive(), theme.getTitleSlideMetadata(), theme.getTitleSlideBackground(),
            theme.isLicenseSlideActive(), theme.getLicenseSlideBackground(), theme.getLicenseSlideDescription(),
            theme.isWatermarkActive(), theme.getWatermarkFile(), theme.getWatermarkPosition());
  }

  @Override
  public void repopulate(final String indexName) {
    final String destinationId = ThemeItem.THEME_QUEUE_PREFIX + WordUtils.capitalize(indexName);
    for (final Organization organization : organizationDirectoryService.getOrganizations()) {
      SecurityUtil.runAs(securityService, organization, SecurityUtil.createSystemUser(cc, organization), new Effect0() {
        @Override
        protected void run() {
          try {
            final List<Theme> themes = getThemes();
            int total = themes.size();
            int current = 1;
            logger.info(
                    "Re-populating '{}' index with themes from organization {}. There are {} theme(s) to add to the index.",
                    new Object[] { indexName, securityService.getOrganization().getId(), total });
            for (Theme theme : themes) {
              messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                      ThemeItem.update(toSerializableTheme(theme)));
              messageSender.sendObjectMessage(IndexProducer.RESPONSE_QUEUE, MessageSender.DestinationType.Queue,
                      IndexRecreateObject.update(indexName, IndexRecreateObject.Service.Themes, total, current));
              current++;
            }
          } catch (ThemesServiceDatabaseException e) {
            logger.error("Unable to get themes from the database because: {}", ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException(e);
          }
        }
      });
    }
    Organization organization = new DefaultOrganization();
    SecurityUtil.runAs(securityService, organization, SecurityUtil.createSystemUser(cc, organization), new Effect0() {
      @Override
      protected void run() {
        messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                IndexRecreateObject.end(indexName, IndexRecreateObject.Service.Themes));
      }
    });
  }

  @Override
  public MessageReceiver getMessageReceiver() {
    return messageReceiver;
  }

  @Override
  public Service getService() {
    return IndexRecreateObject.Service.Themes;
  }

  @Override
  public String getClassName() {
    return ThemesServiceDatabaseImpl.class.getName();
  }

}
